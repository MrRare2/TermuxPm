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
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.util.AndroidException;

import com.termux.termuxpm.logger.Logger;
import com.termux.termuxpm.reflection.ReflectionUtils;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    private int mStartFlags = 0;
    private boolean mWaitOption = false;
    private boolean mStopOption = false;

    private int mRepeat = 0;
    private Integer mUserId;
    private String mReceiverPermission;
    private boolean mCheckDrawOverAppsPermissions = false;

    /*
    private String mProfileFile;
    private int mSamplingInterval;
    private boolean mAutoStop;
    private int mStackId;
    */

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
                "These are similar to commands provided by the Android platform with the /system/bin/pm command. (This is still in development (alpha)\n\n\n" +
		"  help\n" +
		"    Print this help text\n\n" +
		"  path [--user USER_ID] PACKAGE}\n" +
		"   Print the path to the .apk of the given PACKAGE.\n\n" +
		"  dump PACKAGE\n" +
		"    Print various system state associated with the given PACKAGE.\n\n" +
		"  dump-package PACKAGE\n" +
		"    Print package manager state associated with the given PACKAGE.\n\n" +
		"  has-feature FEATURE_NAME [version]\n" +
		"    Prints true and returns exit status 0 when system has a FEATURE_NAME,\n" +
		"    otherwise prints false and returns exit status 1.\n\n" +
                "  list features\n" +
                "    Prints all features of the system.\n\n" +
                "\n"
        );
        IntentCmd.printIntentArgsHelp(pw, "");
	pw.println("this is still a stub\n");
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



    /** Returns the user id for a given uid. */
    public static int getUserId(int uid) {
        return uid / PER_USER_RANGE;
    }

    /**
     * Get the user id for the current foreground user.
     *
     * This requires `android.permission.INTERACT_ACROSS_USERS` or `android.permission.INTERACT_ACROSS_USERS_FULL`,
     * so should be only called if process is running as privileged user like root (`0`) or shell (`2000`)
     * user, etc or if the app has been granted the permission.
     *
     * - https://cs.android.com/android/platform/superproject/+/android-14.0.0_r1:frameworks/base/core/java/android/app/ActivityManager.java;l=4752
     * - https://cs.android.com/android/platform/superproject/+/android-14.0.0_r1:frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java;l=17146
     * - https://cs.android.com/android/platform/superproject/+/android-14.0.0_r1:frameworks/base/services/core/java/com/android/server/am/UserController.java;l=2721
     * - https://cs.android.com/android/platform/superproject/+/android-14.0.0_r1:frameworks/base/services/core/java/com/android/server/am/UserController.java;l=1695
     *
     * @return Returns the user id.
     */
    public static int getCurrentUserId() {
        ReflectionUtils.bypassHiddenAPIReflectionRestrictions();

        String className = "android.app.ActivityManager";
        String methodName = "getCurrentUser";
        try {
            @SuppressLint("PrivateApi") Class<?> clazz = Class.forName(className);
            Method method = ReflectionUtils.getDeclaredMethod(clazz, methodName);
            if (method == null) {
                return USER_NULL;
            }

            Integer userId = (Integer) ReflectionUtils.invokeMethod(method, null).value;
            return userId != null && userId >= 0 ? userId : USER_NULL;
        } catch (Exception e) {
            Logger.logStackTraceWithMessage("Error: Failed to call " + methodName + "() method of " + className + " class", e);
            return USER_NULL;
        }
    }

    int parseUserArg(String arg) {
        return parseUserArg(arg, false);
    }

    int parseUserArg(String arg, boolean getActualUser) {
        int userId;
        if ("all".equals(arg)) {
            //userId = UserHandle.USER_ALL;
            userId = USER_ALL;
        } else if ("current".equals(arg) || "cur".equals(arg)) {
            //userId = UserHandle.USER_CURRENT;

            // We cannot USER_CURRENT (`-2`) for intent commands as it may result in following exception
            // on some devices if running as a normal app user without required permissions.
            // `java.lang.SecurityException: Permission Denial: <method> asks to run as user -2 but
            // is calling from uid <uid>; this requires android.permission.INTERACT_ACROSS_USERS_FULL
            // or android.permission.INTERACT_ACROSS_USERS`
            // - https://github.com/termux/TermuxAm/issues/11
            // Instead, if current process is owned by an app, we return user id for the current process.
            // However, if current process is running as a privileged user like root (`0`) or
            // shell (`2000`) user, `getUserId()` will always return `0`, which will be wrong if
            // running in a secondary user like `10`.
            // So if current process is not owned by an app, and we need the actual user, like for
            // `get-current-user` command, then we use reflection via `getCurrentUserId()` to get
            // the actual user id from a SystemApi, otherwise for intent commands, we return
            // `USER_CURRENT`, as privileged users should have the `INTERACT_ACROSS_USERS*` permission
            // and we let framework to get the actual user itself instead of using reflection here.
            int uid = Process.myUid();
            if (uid >= FIRST_APPLICATION_UID) {
                userId = getUserId(uid);
            } else {
                userId = getActualUser ? getCurrentUserId() : USER_CURRENT;
            }
        } else {
            userId = Integer.parseInt(arg);
        }
        return userId;
    }

    private Intent makeIntent() throws URISyntaxException {
        mStartFlags = 0;
        mWaitOption = false;
        mStopOption = false;
        mRepeat = 0;
        /*
        mProfileFile = null;
        mSamplingInterval = 0;
        mAutoStop = false;
        */
        mUserId = null;
        mCheckDrawOverAppsPermissions = false;
        /*
        mStackId = INVALID_STACK_ID;
        */


        Intent intent = IntentCmd.parseCommandArgs(mArgs, new IntentCmd.CommandOptionHandler() {
            @Override
            public boolean handleOption(String opt, ShellCommand cmd) {
                /*if (opt.equals("-D")) {
                    mStartFlags |= ActivityManager.START_FLAG_DEBUG;
                } else if (opt.equals("-N")) {
                    mStartFlags |= ActivityManager.START_FLAG_NATIVE_DEBUGGING;
                } else */if (opt.equals("-W")) {
                    mWaitOption = true;
                /*
                } else if (opt.equals("-P")) {
                    mProfileFile = nextArgRequired();
                    mAutoStop = true;
                } else if (opt.equals("--start-profiler")) {
                    mProfileFile = nextArgRequired();
                    mAutoStop = false;
                } else if (opt.equals("--sampling")) {
                    mSamplingInterval = Integer.parseInt(nextArgRequired());
                */
                } else if (opt.equals("-R")) {
                    mRepeat = Integer.parseInt(nextArgRequired());
                } else if (opt.equals("-S")) {
                    mStopOption = true;
                /*
                } else if (opt.equals("--track-allocation")) {
                    mStartFlags |= ActivityManager.START_FLAG_TRACK_ALLOCATION;
                */
                } else if (opt.equals("--user")) {
                    mUserId = parseUserArg(nextArgRequired());
                } else if (opt.equals("--receiver-permission")) {
                    mReceiverPermission = nextArgRequired();
                } else if (opt.equals("--check-draw-over-apps-permission")) {
                    mCheckDrawOverAppsPermissions = true;
                /*
                } else if (opt.equals("--stack")) {
                    mStackId = Integer.parseInt(nextArgRequired());
                */
                } else {
                    return false;
                }
                return true;
            }
        });

        // We do not get current user at start of this method in case caller knows that current process
        // does not have required permissions to call `getCurrentUserId()` and has manually passed
        // the user id with the `--user` argument.
        // .
        if (mUserId == null) {
            mUserId = parseUserArg("current");
        }

        return intent;
    }

    // TODO: start implementing here :TODO
    public static boolean isEmpty(String[] arr) {
	return arr == null || arr.length == 0;
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
}
