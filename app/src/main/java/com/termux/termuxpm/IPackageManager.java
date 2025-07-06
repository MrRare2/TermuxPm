package com.termux.termuxpm;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PermissionGroupInfo;
import android.net.Uri;
import android.os.IBinder;

import java.lang.reflect.Method;

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

    IPackageManager() throws Exception {
        IBinder binder = (IBinder) Class
            .forName("android.os.ServiceManager")
            .getMethod("getService", String.class)
            .invoke(null, "package");

        Class<?> stub = Class.forName("android.content.pm.IPackageManager$Stub");
        Method asInterface = stub.getMethod("asInterface", IBinder.class);
        mPm = asInterface.invoke(null, binder);

        Class<?> pmClass = mPm.getClass();

        // getPackageInfo variants
        mGetPackageInfo = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getPackageInfo",
                String.class, "packageName",
                int.class,    "flags",
                int.class,    "userId")
            .tryMethodVariantInexact("getPackageInfo",
                String.class, "packageName",
                int.class,    "flags");

        // getApplicationInfo variants
        mGetApplicationInfo = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getApplicationInfo",
                String.class, "packageName",
                int.class,    "flags",
                int.class,    "userId")
            .tryMethodVariantInexact("getApplicationInfo",
                String.class, "packageName",
                int.class,    "flags");

        // getInstalledPackages variants
        mGetInstalledPackages = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getInstalledPackages",
                int.class, "flags",
                int.class, "userId")
            .tryMethodVariantInexact("getInstalledPackages",
                int.class, "flags");

        // getInstalledApplications variants
        mGetInstalledApplications = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getInstalledApplications",
                int.class, "flags",
                int.class, "userId")
            .tryMethodVariantInexact("getInstalledApplications",
                int.class, "flags");

        // getInstallerPackageName variants
        mGetInstallerPackageName = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getInstallerPackageName",
                String.class, "packageName")
            .tryMethodVariantInexact("getInstallerForPackage",
                String.class, "packageName");
        // the rest
        mGetSystemAvailableFeatures = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getSystemAvailableFeatures");
        mGetSystemSharedLibraryNames = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getSystemSharedLibraryNames");
        mGetAllPermissionGroups = new CrossVersionReflectedMethod(pmClass)
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

    Object getInstalledPackages(int flags, int userId) throws Exception {
        return mGetInstalledPackages.invoke(
            mPm,
            "flags",  flags,
            "userId", userId
        );
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
        return (PermissionGroupInfo[]) mGetAllPermissionGroups.invoke(
            mPm,
            "flags", flags
        );
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

    Object queryIntentActivities(Intent intent, int flags, int userId) throws Exception {
        return mQueryIntentActivities.invoke(
            mPm,
            "intent", intent,
            "flags",  flags,
            "userId", userId
        );
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
}
