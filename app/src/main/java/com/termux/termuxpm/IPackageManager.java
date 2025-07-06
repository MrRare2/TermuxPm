package com.termux.termuxpm;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PermissionInfo;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.ResolveInfo;

import android.net.Uri;
import android.os.IBinder;
import android.os.UserHandle;

import java.lang.reflect.Method;

import java.util.Map;
import java.util.List;

@SuppressLint("PrivateApi")
class IPackageManager {
    private final Object mPm;

    private final CrossVersionReflectedMethod mGetPackageInfo;
    private final CrossVersionReflectedMethod mGetApplicationInfo;
    private final CrossVersionReflectedMethod mGetInstalledPackages;
    private final CrossVersionReflectedMethod mGetInstalledApplications;
    private final CrossVersionReflectedMethod mGetInstallerPackageName;
    private final CrossVersionReflectedMethod mGetInstallLocation;
    private final CrossVersionReflectedMethod mGetSystemAvailableFeatures;
    private final CrossVersionReflectedMethod mGetSystemSharedLibraryNames;
    private final CrossVersionReflectedMethod mGetAllPermissionGroups;
    private final CrossVersionReflectedMethod mHasSystemFeature;
    private final CrossVersionReflectedMethod mQueryIntentServices;
    private final CrossVersionReflectedMethod mQueryIntentActivities;
    private final CrossVersionReflectedMethod mQueryIntentReceivers;
    private final CrossVersionReflectedMethod mGetAllPackages;
    private final CrossVersionReflectedMethod mQueryInstrumentationAsUser;
    private final CrossVersionReflectedMethod mGetPackageInstaller;
    private final CrossVersionReflectedMethod mGetPermissionGroupInfo;
    private final CrossVersionReflectedMethod mQueryPermissionsByGroup;

    IPackageManager() throws Exception {
        IBinder binder = (IBinder) Class
            .forName("android.os.ServiceManager")
            .getMethod("getService", String.class)
            .invoke(null, "package");

        Class<?> stub = Class.forName("android.content.pm.IPackageManager$Stub");
        Method asInterface = stub.getMethod("asInterface", IBinder.class);
        mPm = asInterface.invoke(null, binder);

        Class<?> pmClass = mPm.getClass();

        mGetPackageInfo = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getPackageInfo",
                String.class, "packageName",
                int.class,    "flags",
                int.class,    "userId")
            .tryMethodVariantInexact("getPackageInfo",
                String.class, "packageName",
                int.class,    "flags");

        mGetApplicationInfo = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getApplicationInfo",
                String.class, "packageName",
                int.class,    "flags",
                int.class,    "userId")
            .tryMethodVariantInexact("getApplicationInfo",
                String.class, "packageName",
                int.class,    "flags");
        
	mGetInstalledPackages = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getInstalledPackages",
                int.class, "flags",
                int.class, "userId")
            .tryMethodVariantInexact("getInstalledPackages",
                int.class, "flags");

        mGetInstalledApplications = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getInstalledApplications",
                int.class, "flags",
                int.class, "userId")
            .tryMethodVariantInexact("getInstalledApplications",
                int.class, "flags");

	mGetInstallerPackageName = new CrossVersionReflectedMethod(pmClass)
        .tryMethodVariantInexact("getInstallerPackageName",
            String.class, "packageName",
            int.class,    "userId")
        .tryMethodVariantInexact("getInstallerPackageName",
            String.class, "packageName")
        .tryMethodVariantInexact("getInstallerForPackage",
            String.class, "packageName");

        mGetSystemAvailableFeatures = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getSystemAvailableFeatures");
        mGetSystemSharedLibraryNames = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getSystemSharedLibraryNames");
        mGetAllPermissionGroups = new CrossVersionReflectedMethod(pmClass)
	    .tryMethodVariantInexact("getAllPermissionGroups",
		int.class, "flags",
		int.class, "userId")
            .tryMethodVariantInexact("getAllPermissionGroups",
                int.class, "flags");
	mHasSystemFeature = new CrossVersionReflectedMethod(pmClass)
	    .tryMethodVariantInexact("hasSystemFeature",
		String.class, "featureName",
		int.class,    "featureVersion",
		int.class,    "userId")
	    .tryMethodVariantInexact("hasSystemFeature",
		String.class, "featureName",
		int.class,    "userId")
	    .tryMethodVariantInexact("hasSystemFeature",
		String.class, "featureName");

        mQueryIntentServices = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("queryIntentServices",
                "android.content.Intent", "intent", null,
                int.class,                 "flags",  0,
                int.class,                 "userId", 0)
            .tryMethodVariantInexact("queryIntentServices",
                "android.content.Intent", "intent", null,
                int.class,                 "flags");

        mQueryIntentActivities = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("queryIntentActivities",
                "android.content.Intent", "intent", null,
                int.class,                 "flags",  0,
                int.class,                 "userId", 0)
            .tryMethodVariantInexact("queryIntentActivities",
                "android.content.Intent", "intent", null,
                int.class,                 "flags");

        mQueryIntentReceivers = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("queryIntentReceivers",
                "android.content.Intent", "intent", null,
                int.class,                 "flags",  0,
                int.class,                 "userId", 0)
            .tryMethodVariantInexact("queryIntentReceivers",
		"android.content.Intent",
		"intent", null,
                int.class,
		"flags"
	);
        mGetAllPackages = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getInstalledPackages",
                int.class, "flags",
                int.class, "userId")
            .tryMethodVariantInexact("getInstalledPackages",
                int.class, "flags"
	);

        mQueryInstrumentationAsUser = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("queryInstrumentationAsUser",
                String.class, "targetPackage", null,
                int.class,    "userId",        0)
            .tryMethodVariantInexact("queryInstrumentation",
                String.class, "targetPackage", null);

	mGetInstallLocation = new CrossVersionReflectedMethod(pmClass)
        .tryMethodVariantInexact("getInstallLocation",
            int.class, "userId")
        .tryMethodVariantInexact("getInstallLocation");
	mGetPackageInstaller = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getPackageInstaller");

        mGetPermissionGroupInfo = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getPermissionGroupInfo",
                String.class, "name",  null,
                int.class,    "flags");

        mQueryPermissionsByGroup = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("queryPermissionsByGroup",
                String.class, "permissionGroup", null,
                int.class,    "flags");
    }

    PackageInfo getPackageInfo(String pkg, int flags, int userId) throws Exception {
        return (PackageInfo) mGetPackageInfo.invoke(
            mPm,
            "packageName", pkg,
            "flags",       flags,
            "userId",      userId
        );
    }

    ApplicationInfo getApplicationInfo(String pkg, int flags, int userId) throws Exception {
        return (ApplicationInfo) mGetApplicationInfo.invoke(
            mPm,
            "packageName", pkg,
            "flags",       flags,
            "userId",      userId
        );
    }

    PackageInfo[] getInstalledPackages(int flags, int userId) throws Exception {
        Object mFl = mGetInstalledPackages.invoke(
	    mPm,
	    "flags",  flags,
            "userId", userId
        );
	Method gLm = mFl.getClass().getMethod("getList");
	return ((List<PackageInfo>) gLm.invoke(mFl)).toArray(new PackageInfo[0]);
    }

    Object getInstalledApplications(int flags, int userId) throws Exception {
        return mGetInstalledApplications.invoke(
            mPm,
            "flags",  flags,
            "userId", userId
        );
    }

    String getInstallerPackageName(String pkg) throws Exception {
        return (String) mGetInstallerPackageName.invoke(
            mPm,
            "packageName", pkg
        );
    }

    int getInstallLocation() throws Exception {
        return (Integer) mGetInstallLocation.invoke(mPm);
    }

    FeatureInfo[] getSystemAvailableFeatures() throws Exception {
        Object mFl = mGetSystemAvailableFeatures.invoke(mPm);
	Method gLm = mFl.getClass().getMethod("getList");
	return ((List<FeatureInfo>) gLm.invoke(mFl)).toArray(new FeatureInfo[0]);
    }

    String[] getSystemSharedLibraryNames() throws Exception {
        return (String[]) mGetSystemSharedLibraryNames.invoke(mPm);
    }

    PermissionGroupInfo[] getAllPermissionGroups(int flags) throws Exception {
        Object mFl = mGetAllPermissionGroups.invoke(mPm,"flags", flags);
        Method gLm = mFl.getClass().getMethod("getList");
        return ((List<PermissionGroupInfo>) gLm.invoke(mFl)).toArray(new PermissionGroupInfo[0]);
    }

    PermissionGroupInfo[] getAllPermissionGroups(int flags, int userId) throws Exception {
	Object mFl = mGetAllPermissionGroups.invoke(mPm,"flags", flags, "userId", userId);
	Method gLm = mFl.getClass().getMethod("getList");
	return ((List<PermissionGroupInfo>) gLm.invoke(mFl)).toArray(new PermissionGroupInfo[0]);
    }

    boolean hasSystemFeature(String featureName, int featureVersion, int userId) throws Exception {
	return (Boolean) mHasSystemFeature.invoke(
	    mPm,
	    "featureName",    featureName,
	    "featureVersion", featureVersion,
	    "userId",         userId
	);
    }

    boolean hasSystemFeature(String featureName, int userId) throws Exception {
	return (Boolean) mHasSystemFeature.invoke(
	    mPm,
	    "featureName", featureName,
	    "userId",      userId
	);
    }

    boolean hasSystemFeature(String featureName) throws Exception {
	return (Boolean) mHasSystemFeature.invoke(
	    mPm,
	    "featureName", featureName
	);
    }

    Object queryIntentServices(Intent intent, int flags, int userId) throws Exception {
        return mQueryIntentServices.invoke(
            mPm,
            "intent", intent,
            "flags",  flags,
            "userId", userId
        );
    }

    ResolveInfo[] queryIntentActivities(Intent intent, int flags, int userId) throws Exception {
        Object mFl = mQueryIntentActivities.invoke(
            mPm,
            "intent", intent,
            "flags",  flags,
            "userId", userId
        );

	Method gLm = mFl.getClass().getMethod("getList");

	return ((List<ResolveInfo>) gLm.invoke(mFl)).toArray(new ResolveInfo[0]);
    }

    Object queryIntentReceivers(Intent intent, int flags, int userId) throws Exception {
        return mQueryIntentReceivers.invoke(
            mPm,
            "intent", intent,
            "flags",  flags,
            "userId", userId
        );
    }

    PackageInfo[] getAllPackages(int flags, int userId) throws Exception {
        @SuppressWarnings("unchecked")
        List<PackageInfo> pkgs = (List<PackageInfo>) mGetAllPackages.invoke(
            mPm,
            "flags",  flags,
            "userId", userId
        );
        return pkgs.toArray(new PackageInfo[0]);
    }

    InstrumentationInfo[] queryInstrumentationAsUser(String targetPackage, int userId) throws Exception {
        Object mFl = mQueryInstrumentationAsUser.invoke(
                mPm,
                "targetPackage", targetPackage,
                "userId",        userId
            );
	Method gLm = mFl.getClass().getMethod("getList");
        return ((List<InstrumentationInfo>) gLm.invoke(mFl)).toArray(new InstrumentationInfo[0]);
    }

    InstrumentationInfo[] queryInstrumentation(String targetPackage) throws Exception {
        Object mFl = mQueryInstrumentationAsUser.invoke(
                mPm,
                "targetPackage", targetPackage
        );
	Method gLm = mFl.getClass().getMethod("getList");
	return ((List<InstrumentationInfo>) gLm.invoke(mFl)).toArray(new InstrumentationInfo[0]);
    }

    Object getPackageInstaller() throws Exception {
        return mGetPackageInstaller.invoke(mPm);
    }

    PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws Exception {
        return (PermissionGroupInfo) mGetPermissionGroupInfo.invoke(
            mPm,
            "name",  name,
            "flags", flags
        );
    }

    PermissionInfo[] queryPermissionsByGroup(String group, int flags) throws Exception {
        Object mFl = mQueryPermissionsByGroup.invoke(
            mPm,
            "permissionGroup", group,
            "flags", flags
        );
	Method gLm = mFl.getClass().getMethod("getList");
	return ((List<PermissionInfo>) gLm.invoke(mFl)).toArray(new PermissionInfo[0]);
    }
}
