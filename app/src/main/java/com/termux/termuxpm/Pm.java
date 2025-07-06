/*
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/


package com.termux.termuxpm;

import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageItemInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.AndroidException;

import com.termux.termuxpm.logger.Logger;
import com.termux.termuxpm.reflection.ReflectionUtils;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.WeakHashMap;

public class Pm extends BaseCommand {

    public static final String LOG_TAG = "TermuxPm";

    /*
    private static final String SHELL_PACKAGE_NAME = "com.android.shell";

    // Is the object moving in a positive direction?
    private static final boolean MOVING_FORWARD = true;
    // Is the object moving in the horizontal plan?
    private static final boolean MOVING_HORIZONTALLY = true;
    // Is the object current point great then its target point?
    private static final boolean GREATER_THAN_TARGET = true;
    // Amount we reduce the stack size by when testing a task re-size.
    private static final int STACK_BOUNDS_INSET = 10;
    */

    /**
     * Range of uids allocated for a user.
     *
     * This is the same as `AID_USER_OFFSET` defined in `android_filesystem_config.h`.
     *
     * - https://cs.android.com/android/platform/superproject/+/android-13.0.0_r18:frameworks/base/core/java/android/os/UserHandle.java;l=42
     * - https://cs.android.com/android/platform/superproject/+/android-5.0.0_r1.0.1:frameworks/base/core/java/android/os/UserHandle.java;l=29
     * - https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:system/core/libcutils/include/private/android_filesystem_config.h;l=214
     * - https://cs.android.com/android/platform/superproject/+/android-13.0.0_r18:system/core/libcutils/multiuser.cpp
     */
    public static final int PER_USER_RANGE = 100000;

    /**
     * Defines the start of a range of UIDs (and GIDs), going from this
     * number to {@link #LAST_APPLICATION_UID} that are reserved for assigning
     * to applications.
     */
    public static final int FIRST_APPLICATION_UID = 10000;

    /**
     * Last of application-specific UIDs starting at
     * {@link #FIRST_APPLICATION_UID}.
     */
    public static final int LAST_APPLICATION_UID = 19999;

    /** A user id to indicate all users on the device. */
    public static final int USER_ALL = -1;

    /** A user id to indicate the currently active user. */
    public static final int USER_CURRENT = -2;

    /** An undefined user id. */
    public static final int USER_NULL = -10000;

    private IActivityManager mAm;
    
    private IPackageManager mPm; // IPackageManager mPm;
    
    private IPermissionManager mPermManager;
    final private WeakHashMap<String, Resources> mResourceCache = new WeakHashMap<String, Resources>();

    /**
     * Command-line entry point.
     *
     * @param args The command-line arguments
     */
    public static void main(String[] args) {
        Integer exitCode = new Pm().run(args);
        // If command finished, then exit with exit code, otherwise let command waiting thread to call exit itself.
        if (exitCode != null)
            System.exit(parseExitCode(exitCode));
    }

    public static int parseExitCode(int exitCode) {
        return exitCode < 0 || exitCode > 255 ? 1 : exitCode;
    }

    @Override
    public void onShowUsage(PrintStream out) {
        PrintWriter pw = new PrintWriter(out);
        pw.println(
                "Package manager (package) commands provided by the " + FakeContext.PACKAGE_NAME + " app.\n" +
                "These are similar to commands provided by the Android platform with the /system/bin/pm command.\n\n" +
		"  help\n" +
		"    Print this help text.\n\n" +
		"  path [--user USER_ID] PACKAGE\n" +
		"   Print the path to the .apk of the given PACKAGE.\n\n" +
		"  has-feature FEATURE_NAME [version]\n" +
		"    Prints true and returns exit status 0 when system has a FEATURE_NAME,\n" +
		"    otherwise prints false and returns exit status 1.\n\n" +
                "  list features\n" +
                "    Prints all features of the system.\n\n" +
		"  list instrumentation [-f] [TARGET-PACKAGE]\n" +
		"    Prints all test packages; optionally only those targeting TARGET-PACKAGE\n\n" +
		"    Options:\n" +
		"      -f: dump the name of the .apk file containing the test package\n\n" +
		"  list libraries\n" +
		"    Prints all system libraries.\n\n" +
		"  list packages [-f] [-d] [-e] [-s] [-3] [-i] [-u] [-l] [--user USER_ID] [FILTER]\n" +
		"    Prints all packages; optionally only those whose name contains\n" +
		"    the text in FILTER.\n" +
		"    Options:\n" +
		"      -f: see their associated file\n" +
		"      -d: filter to only show disabled packages\n" +
		"      -s: filter to only show system packages\n" +
		"      -3: filter to only show third party packages\n" +
		"      -i: see the installer for the packages\n" +
		"      -u: also include uninstalled packages\n" +
		"      -l: also includes launcher activity (Termux only)\n\n" +
		"  list permission-groups\n" +
		"    Prints all known permission groups.\n" +
		"  list permissions [-g] [-f] [-d] [-u] [GROUP]\n" +
		"    Prints all known permissions; optionally only those in GROUP.\n" +
		"    Options:\n" +
		"      -g: organize by group\n" +
		"      -f: print all information\n" +
		"      -s: short summary\n" +
		"      u: list only the permissions users will see\n\n" +
		"  install PATH\n" +
		"    Install an application. Must provide the apk data to install,\n" +
		"    as an absolute file path\n\n" +
		"  uninstall PACKAGE\n" +
		"    Remove the given package name from the system.\n"
	);
        //IntentCmd.printIntentArgsHelp(pw, "");
        pw.flush();
    }

    @Override
    public Integer onRun() throws Exception {
        String op = nextArgRequired();
        if (op.equals("-h") || op.equals("--help") || op.equals("help")) {
            onShowUsage(System.out);
            return 0;
        }

        mAm = new IActivityManager();
        if (mAm == null) {
            System.err.println(NO_SYSTEM_ERROR_CODE);
            throw new AndroidException("Can't connect to activity manager; is the system running?");
        }

        mPm = new IPackageManager(); // IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
	if (mPm == null) {
            System.err.println(NO_SYSTEM_ERROR_CODE);
            throw new AndroidException("Can't connect to package manager; is the system running?");
        }

	mPermManager = new IPermissionManager();

	if (mPermManager == null) {
	    System.err.println(NO_SYSTEM_ERROR_CODE);
	    throw new AndroidException("Can't connect to permission manager; is the system running?");
	}

	if (op.equals("path")) {
            return runPath();
	} else if (op.equals("has-feature")) {
	    return runHasFeature();
        } else if (op.equals("list")) {
	    return runList();
	} else if (op.equals("install")) {
	    return runInstall();
	} else if (op.equals("uninstall")) {
	    return runUninstall();
	} else {
            showError("Error: unknown command '" + op + "'");
            return 1;
        }
    }

    // TODO: start implementing here :TODO
    public static boolean isEmpty(String[] arr) {
	return arr == null || arr.length == 0;
    }

    private String loadText(PackageItemInfo pii, int res, CharSequence nonLocalized) throws Exception {
        if (nonLocalized != null) {
            return nonLocalized.toString();
        }
        if (res != 0) {
            Resources r = getResources(pii);
            if (r != null) {
                try {
		    return r.getString(res);
                } catch (Resources.NotFoundException e) {
                }
            }
        }
        return null;
    }

    private Resources getResources(PackageItemInfo pii) throws Exception {
        Resources res = mResourceCache.get(pii.packageName);
        if (res != null) return res;
	
	ApplicationInfo ai = mPm.getApplicationInfo(pii.packageName, 0, 0);
	Constructor<AssetManager> ctor = AssetManager.class.getDeclaredConstructor();
	ctor.setAccessible(true);
	AssetManager am = ctor.newInstance();
	Method mAddAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
	mAddAssetPath.setAccessible(true);
        mAddAssetPath.invoke(am, ai.publicSourceDir);
        res = new Resources(am, null, null);
        mResourceCache.put(pii.packageName, res);
        return res;
    }

    private String getProtLevel(int protLevel) throws Exception {
	Method mProtToStr = PermissionInfo.class.getDeclaredMethod("protectionToString", int.class);
	mProtToStr.setAccessible(true);
	return (String) mProtToStr.invoke(null, protLevel);
    }

    private int displayPackageFilePath(String pckg, int userId) throws Exception {
	PackageInfo info = mPm.getPackageInfo(pckg, PackageManager.MATCH_APEX, userId);
	if (info != null && info.applicationInfo != null) {
	    System.out.print("package:");
	    System.out.println(info.applicationInfo.sourceDir);
	    if (!isEmpty(info.applicationInfo.splitSourceDirs)) {
		for (String splitSourceDir : info.applicationInfo.splitSourceDirs) {
		    System.out.print("package:");
		    System.out.println(splitSourceDir);
		}
	    }
	    return 0;
	}
	return 1;
    }

    private int runPath() throws Exception {
	int userId = USER_ALL;
	String option = nextOption();
	if (option != null && option.equals("--user")) {
	    userId = Integer.parseInt(option);
	}
	String pkg = nextArgRequired();
	if (pkg == null) {
	    System.err.println("Error: no package specified");
	    return 1;
	}
	return displayPackageFilePath(pkg, userId);
    }

    private int runHasFeature() throws Exception {
	final String featureName = nextArg();
	if (featureName == null) {
	    System.err.println("Error: Expected FEATURE name");
	    return 1;
	}
	final String versionString = nextArg();
	try {
	    final int version = (versionString == null) ? 0 : Integer.parseInt(versionString);
	    final boolean hasFeature = mPm.hasSystemFeature(featureName, version);
	    System.out.println(hasFeature);
	    return hasFeature ? 0 : 1;
	} catch (NumberFormatException e) {
	    System.err.println("Error: illegal version number " + versionString);
	    return 1;
	}
    }

    private Uri doGetUri(String path) {
        if (path.startsWith("/data/data/com.termux/files")) {
            String trimmed = path.substring("/data/data/com.termux/files".length());
            return Uri.parse("content://com.termux.provider/files" + trimmed);
        } else if (path.startsWith("/")) {
            return Uri.parse("content://com.termux.provider/root" + path);
        } else if (path.startsWith("./")) {
            System.err.println("Error: you must provide the absolute path"); // FIXME: way to handle relative path?
            return null;
        } else {
            System.err.println("Error: you must provide the absolute path");
	    return null;
        }
    }

    private int doPromptInstall(String apkPath) throws Exception {
        String installerPkg = FakeContext.PACKAGE_NAME;
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
	Uri apkUri = doGetUri(apkPath);
	if (apkUri == null) return 1;
	System.out.println("debug: content uri " + apkUri.toString());
	intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

	intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
	intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + installerPkg));
        mAm.startActivityAsUser(
            intent, null, Intent.FLAG_ACTIVITY_NEW_TASK, null, 0
        );
        return 0;
    }

    private int runInstall() throws Exception {
	// idk how to do this fully
	final String apkPath = nextArg();
	if (apkPath == null) {
	    System.err.println("Error: no APK path supplied");
	    return 1;
	}

	return doPromptInstall(apkPath);
    }

    private int runUninstall() throws Exception {
	final String packageName = nextArgRequired();

	if (packageName == null) {
	    System.err.println("Error: package name not specified");
	    return 1;
	}

	Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
	intent.setData(Uri.parse("package:" + packageName));
	mAm.startActivityAsUser(
	    intent, null, Intent.FLAG_ACTIVITY_NEW_TASK, null, 0
	);

	return 0;
    }

    /* TODO: runList* impl :TODO */

    private int runList() throws Exception {
	final String type = nextArg();
        if (type == null) {
            System.err.println("Error: didn't specify type of data to list");
            return 1;
        }

	if (type.equals("features")) {
	    return runListFeatures();
	} else if (type.equals("instrumentation")) {
	    return runListInstrumentation();
	} else if (type.equals("libraries")) {
	    return runListLibraries();
        } else if (type.equals("package")||type.equals("packages")) {
	    return runListPackages(false);
	} else if (type.equals("permission-groups")) {
	    return runListPermissionGroups();
	} else if (type.equals("permissions")) {
	    return runListPermissions();
	}

	System.err.println("Error: unknown list type '" + type + "'");
	return 1;
    }

    private int runListFeatures() throws Exception {
        final FeatureInfo[] list = mPm.getSystemAvailableFeatures();

        Arrays.sort(list, new Comparator<FeatureInfo>() {
            public int compare(FeatureInfo o1, FeatureInfo o2) {
                if (o1.name == o2.name) return 0;
                if (o1.name == null) return -1;
                if (o2.name == null) return 1;
                return o1.name.compareTo(o2.name);
            }
        });

        final int count = (list != null) ? list.length : 0;
        for (int p = 0; p < count; p++) {
            FeatureInfo fi = list[p];
            System.out.print("feature:");
            if (fi.name != null) {
                System.out.print(fi.name);
                if (fi.version > 0) {
                    System.out.print("=");
                    System.out.print(fi.version);
                }
                System.out.println();
            } else {
                System.out.println("reqGlEsVersion=0x"
                        + Integer.toHexString(fi.reqGlEsVersion));
            }
        }
        return 0;
    }

    private int runListInstrumentation() throws Exception {
        boolean showSourceDir = false;
        String targetPackage = null;

        try {
            String opt;
            while ((opt = nextArg()) != null) {
                switch (opt) {
                    case "-f":
                        showSourceDir = true;
                        break;
                    default:
                        if (opt.charAt(0) != '-') {
                            targetPackage = opt;
                        } else {
                            System.err.println("Error: Unknown option: " + opt);
                            return -1;
                        }
                        break;
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("Error: " + ex.toString());
            return -1;
        }

        final InstrumentationInfo[] list = mPm.queryInstrumentationAsUser(targetPackage, USER_ALL);

        Arrays.sort(list, new Comparator<InstrumentationInfo>() {
            public int compare(InstrumentationInfo o1, InstrumentationInfo o2) {
                return o1.targetPackage.compareTo(o2.targetPackage);
            }
        });

        final int count = (list != null) ? list.length : 0;
        for (int p = 0; p < count; p++) {
            final InstrumentationInfo ii = list[p];
            System.out.print("instrumentation:");
            if (showSourceDir) {
                System.out.print(ii.sourceDir);
                System.out.print("=");
            }
            final ComponentName cn = new ComponentName(ii.packageName, ii.name);
            System.out.print(cn.flattenToShortString());
            System.out.print(" (target=");
            System.out.print(ii.targetPackage);
            System.out.println(")");
        }
        return 0;
    }

    private int runListLibraries() throws Exception {
        final List<String> list = new ArrayList<String>();
        final String[] rawList = mPm.getSystemSharedLibraryNames();
        for (int i = 0; i < rawList.length; i++) {
            list.add(rawList[i]);
        }
        // sort by name
        Collections.sort(list, new Comparator<String>() {
            public int compare(String o1, String o2) {
                if (o1 == o2) return 0;
                if (o1 == null) return -1;
                if (o2 == null) return 1;
                return o1.compareTo(o2);
            }
        });
        final int count = (list != null) ? list.size() : 0;
        for (int p = 0; p < count; p++) {
            String lib = list.get(p);
            System.out.print("library:");
            System.out.println(lib);
        }
        return 0;
    }

    private int runListPackages(boolean showSourceDir) throws Exception {
        final PrintStream pw = System.out;
	final PrintStream pwErr = System.err;
        int getFlags = 0;
        boolean listDisabled = false, listEnabled = false;
        boolean listSystem = false, listThirdParty = false;
        boolean listInstaller = false;
	boolean listLauncher = false;
	int userId = USER_ALL;
        try {
            String opt;
            while ((opt = nextOption()) != null) {
                switch (opt) {
                    case "-d":
                        listDisabled = true;
                        break;
                    case "-e":
                        listEnabled = true;
                        break;
                    case "-f":
                        showSourceDir = true;
                        break;
                    case "-i":
                        listInstaller = true;
                        break;
                    case "-s":
                        listSystem = true;
                        break;
                    case "-u":
                        getFlags |= PackageManager.GET_UNINSTALLED_PACKAGES;
                        break;
                    case "-3":
                        listThirdParty = true;
                        break;
                    case "--user":
                        userId = Integer.parseInt(nextArgRequired());
                        break;
		    // FIXME: while i dont have time to implement resolve-activity
		    case "-l":
		        listLauncher = true;
			break;
                    default:
                        pw.println("Error: Unknown option: " + opt);
                        return -1;
                }
            }
        } catch (Exception ex) {
            pwErr.println("Error: " + ex.toString());
            return -1;
        }
        final String filter = nextArg();
        @SuppressWarnings("unchecked")
        final PackageInfo[] slice = mPm.getInstalledPackages(getFlags, userId);
        final List<PackageInfo> packages = Arrays.asList(slice);
        final int count = packages.size();
        for (int p = 0; p < count; p++) {
            final PackageInfo info = packages.get(p);
            if (filter != null && !info.packageName.contains(filter)) {
                continue;
            }
            final boolean isSystem =
                    (info.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM) != 0;
            if ((!listDisabled || !info.applicationInfo.enabled) &&
                    (!listEnabled || info.applicationInfo.enabled) &&
                    (!listSystem || isSystem) &&
                    (!listThirdParty || !isSystem)) {
                pw.print("package:");
                if (showSourceDir) {
                    pw.print(info.applicationInfo.sourceDir);
                    pw.print("=");
                }
                pw.print(info.packageName);
                if (listInstaller) {
                    pw.print("  installer=");
                    pw.print(mPm.getInstallerPackageName(info.packageName));
                }

		if (listLauncher) {
		    Intent intent = new Intent(Intent.ACTION_MAIN, null);
		    intent.addCategory(Intent.CATEGORY_LAUNCHER);
		    intent.setPackage(info.packageName);
		    List<ResolveInfo> ri = Arrays.asList(mPm.queryIntentActivities(intent, 0, 0));
		    String launcherAct = null;
		    if (!ri.isEmpty()) {
			launcherAct = ri.get(0).activityInfo.name;
		    }
		    if (launcherAct != null) {
			pw.print("  launcher=");
			pw.print(launcherAct);
		    }
		}
                pw.println();
            }
        }
        return 0;
    }

    private int runListPermissionGroups() throws Exception {
	List<PermissionGroupInfo> pgs;
        if (Build.VERSION.SDK_INT >= 30) {
	    pgs = Arrays.asList(mPermManager.getAllPermissionGroups(0));
	} else {
	    pgs = Arrays.asList(mPm.getAllPermissionGroups(0));
	}
        final int count = pgs.size();
        for (int p = 0; p < count ; p++) {
            final PermissionGroupInfo pgi = (PermissionGroupInfo) pgs.get(p);
            System.out.print("permission group:");
            System.out.println(pgi.name);
        }
        return 0;
    }

    private void doListPermissions(ArrayList<String> groupList, boolean groups, boolean labels, boolean summary, int startProtectionLevel, int endProtectionLevel) throws Exception {
        final PrintStream pw = System.out;
        final int groupCount = groupList.size();
        for (int i = 0; i < groupCount; i++) {
            String groupName = groupList.get(i);
            String prefix = "";
            if (groups) {
                if (i > 0) {
                    pw.println("");
                }
                if (groupName != null) {
                    PermissionGroupInfo pgi =
                            mPm.getPermissionGroupInfo(groupName, 0 /*flags*/);
                    if (summary) {
                        Resources res = getResources(pgi);
                        if (res != null) {
                            pw.print(loadText(pgi, pgi.labelRes, pgi.nonLocalizedLabel) + ": ");
                        } else {
                            pw.print(pgi.name + ": ");
			}
                    } else {
                        pw.println((labels ? "+ " : "") + "group:" + pgi.name);
                        if (labels) {
                            pw.println("  package:" + pgi.packageName);
                            Resources res = getResources(pgi);
                            if (res != null) {
                                pw.println("  label:"
                                        + loadText(pgi, pgi.labelRes, pgi.nonLocalizedLabel));
                                pw.println("  description:"
                                        + loadText(pgi, pgi.descriptionRes,
                                                pgi.nonLocalizedDescription));
                            }
                        }
                    }
                } else {
                    pw.println(((labels && !summary) ? "+ " : "") + "ungrouped:");
                }
                prefix = "  ";
            }

	    List<PermissionInfo> ps;
	    try {
		ps = Arrays.asList(mPm.queryPermissionsByGroup(groupList.get(i), 0));
	    } catch (RuntimeException e) {
		ps = Arrays.asList(mPermManager.queryPermissionsByGroup(groupList.get(i), 0));
	    }
            final int count = ps.size();
            boolean first = true;
            for (int p = 0 ; p < count ; p++) {
                PermissionInfo pi = ps.get(p);
                if (groups && groupName == null && pi.group != null) {
                    continue;
                }
                final int base = pi.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE;
                if (base < startProtectionLevel
                        || base > endProtectionLevel) {
                    continue;
                }
                if (summary) {
                    if (first) {
                        first = false;
                    } else {
                        pw.print(", ");
                    }
                    Resources res = getResources(pi);
                    if (res != null) {
                        pw.print(loadText(pi, pi.labelRes,
                                pi.nonLocalizedLabel));
                    } else {
                        pw.print(pi.name);
                    }
                } else {
                    pw.println(prefix + (labels ? "+ " : "")
                            + "permission:" + pi.name);
                    if (labels) {
                        pw.println(prefix + "  package:" + pi.packageName);
                        Resources res = getResources(pi);
                        if (res != null) {
                            pw.println(prefix + "  label:"
                                    + loadText(pi, pi.labelRes,
                                            pi.nonLocalizedLabel));
                            pw.println(prefix + "  description:"
                                    + loadText(pi, pi.descriptionRes,
                                            pi.nonLocalizedDescription));
                        }
                        pw.println(prefix + "  protectionLevel:"
                                + getProtLevel(pi.protectionLevel));
                    }
                }
            }
	    if (summary) {
                pw.println("");
            }
        }
    }

    private int runListPermissions() throws Exception {
        boolean labels = false;
        boolean groups = false;
        boolean userOnly = false;
        boolean summary = false;
        boolean dangerousOnly = false;
        String opt;
        while ((opt = nextOption()) != null) {
            switch (opt) {
                case "-d":
                    dangerousOnly = true;
                    break;
                case "-f":
                    labels = true;
                    break;
                case "-g":
                    groups = true;
                    break;
                case "-s":
                    groups = true;
                    labels = true;
                    summary = true;
                    break;
                case "-u":
                    userOnly = true;
                    break;
		default:
                    System.err.println("Error: Unknown option: " + opt);
                    return 1;
            }
        }

	final ArrayList<String> groupList = new ArrayList<String>();
        if (groups) {
            final List<PermissionGroupInfo> infos = Arrays.asList(mPm.getAllPermissionGroups(0));
            final int count = infos.size();
            for (int i = 0; i < count; i++) {
                groupList.add(infos.get(i).name);
            }
            groupList.add(null);
        } else {
            final String grp = nextArg();
            groupList.add(grp);
        }

	if (dangerousOnly) {
            System.out.println("Dangerous Permissions:");
            System.out.println("");
            doListPermissions(groupList, groups, labels, summary,
                    PermissionInfo.PROTECTION_DANGEROUS,
                    PermissionInfo.PROTECTION_DANGEROUS);
            if (userOnly) {
                System.out.println("Normal Permissions:");
                System.out.println("");
                doListPermissions(groupList, groups, labels, summary,
                        PermissionInfo.PROTECTION_NORMAL,
                        PermissionInfo.PROTECTION_NORMAL);
            }
        } else if (userOnly) {
            System.out.println("Dangerous and Normal Permissions:");
            System.out.println("");
            doListPermissions(groupList, groups, labels, summary,
                    PermissionInfo.PROTECTION_NORMAL,
                    PermissionInfo.PROTECTION_DANGEROUS);
        } else {
            System.out.println("All Permissions:");
            System.out.println("");
            doListPermissions(groupList, groups, labels, summary,
                    -10000, 10000);
        }
        return 0;
    }
}
