package com.termux.termuxpm;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PermissionGroupInfo;
import android.net.Uri;
import android.os.IBinder;

import java.lang.reflect.Method;

@SuppressLint("PrivateApi")
class IPackageManager {
    private final Object mPm;

    private final CrossVersionReflectedMethod mGetPackageInfo;
    private final CrossVersionReflectedMethod mGetApplicationInfo;
    private final CrossVersionReflectedMethod mGetInstalledPackages;
    private final CrossVersionReflectedMethod mGetInstalledApplications;
    private final CrossVersionReflectedMethod mGetInstallerPackageName;
    private final CrossVersionReflectedMethod mInstallPackage;
    private final CrossVersionReflectedMethod mDeletePackage;
    private final CrossVersionReflectedMethod mMovePackage;
    private final CrossVersionReflectedMethod mSetInstallLocation;
    private final CrossVersionReflectedMethod mGetInstallLocation;
    private final CrossVersionReflectedMethod mGetSystemAvailableFeatures;
    private final CrossVersionReflectedMethod mGetSystemSharedLibraryNames;
    private final CrossVersionReflectedMethod mGetAllPermissionGroups;
    private final CrossVersionReflectedMethod mGrantRuntimePermission;
    private final CrossVersionReflectedMethod mRevokeRuntimePermission;
    private final CrossVersionReflectedMethod mClearApplicationUserData;
    private final CrossVersionReflectedMethod mSetComponentEnabledSetting;

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

        // installPackage variants
        mInstallPackage = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("installPackage",
                Uri.class,   "origin",
                "android.content.pm.IPackageInstallObserver", "observer", null,
                int.class,   "flags", 0,
                String.class,"installerPackageName", null,
                int.class,   "userId", 0)
            .tryMethodVariantInexact("installPackage",
                Uri.class,   "origin",
                "android.content.pm.IPackageInstallObserver", "observer", null,
                int.class,   "flags", 0,
                boolean.class,"installExternal", false)
            .tryMethodVariantInexact("installPackage",
                Uri.class,   "origin",
                "android.content.pm.IPackageInstallObserver", "observer", null,
                int.class,   "flags", 0);

        // deletePackage variants
        mDeletePackage = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("deletePackage",
                String.class, "packageName",
                "android.content.pm.IPackageDeleteObserver", "observer", null,
                int.class,    "flags", 0,
                int.class,    "userId", 0)
            .tryMethodVariantInexact("deletePackage",
                String.class, "packageName",
                "android.content.pm.IPackageDeleteObserver", "observer", null,
                int.class,    "flags", 0);

        // movePackage variants (Lollipop+)
        mMovePackage = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("movePackage",
                String.class, "packageName",
                int.class,    "flags",
                "android.content.pm.IPackageMoveObserver", "observer", null);

        // install location variants
        mSetInstallLocation = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("setInstallLocation",
                int.class, "loc",
                int.class, "userId")
            .tryMethodVariantInexact("setInstallLocation",
                int.class, "loc");
        mGetInstallLocation = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getInstallLocation");

        // the rest
        mGetSystemAvailableFeatures = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getSystemAvailableFeatures");
        mGetSystemSharedLibraryNames = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getSystemSharedLibraryNames");
        mGetAllPermissionGroups = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("getAllPermissionGroups",
                int.class, "flags");
        mGrantRuntimePermission = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("grantRuntimePermission",
                String.class, "packageName",
                String.class, "permission",
                int.class,    "userId");
        mRevokeRuntimePermission = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("revokeRuntimePermission",
                String.class, "packageName",
                String.class, "permission",
                int.class,    "userId");
        mClearApplicationUserData = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("clearApplicationUserData",
                String.class, "packageName",
                "android.content.pm.IPackageDataObserver", "observer", null,
                int.class,    "userId", 0);
        mSetComponentEnabledSetting = new CrossVersionReflectedMethod(pmClass)
            .tryMethodVariantInexact("setComponentEnabledSetting",
                ComponentName.class, "componentName",
                int.class,           "newState",
                int.class,           "flags",
                int.class,           "userId");
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

    void installPackage(Uri origin, Object observer, int flags,
                        String installer, int userId) throws Exception {
        mInstallPackage.invoke(
            mPm,
            "origin",               origin,
            "observer",             observer,
            "flags",                flags,
            "installerPackageName", installer,
            "userId",               userId
        );
    }

    void deletePackage(String pkg, Object observer, int flags, int userId) throws Exception {
        mDeletePackage.invoke(
            mPm,
            "packageName", pkg,
            "observer",    observer,
            "flags",       flags,
            "userId",      userId
        );
    }

    void movePackage(String pkg, int flags, Object observer) throws Exception {
        mMovePackage.invoke(
            mPm,
            "packageName", pkg,
            "flags",       flags,
            "observer",    observer
        );
    }

    void setInstallLocation(int loc, int userId) throws Exception {
        mSetInstallLocation.invoke(
            mPm,
            "loc",    loc,
            "userId", userId
        );
    }

    int getInstallLocation() throws Exception {
        return (Integer) mGetInstallLocation.invoke(mPm);
    }

    FeatureInfo[] getSystemAvailableFeatures() throws Exception {
        return (FeatureInfo[]) mGetSystemAvailableFeatures.invoke(mPm);
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

    void grantRuntimePermission(String pkg, String perm, int userId) throws Exception {
        mGrantRuntimePermission.invoke(
            mPm,
            "packageName", pkg,
            "permission",  perm,
            "userId",      userId
        );
    }

    void revokeRuntimePermission(String pkg, String perm, int userId) throws Exception {
        mRevokeRuntimePermission.invoke(
            mPm,
            "packageName", pkg,
            "permission",  perm,
            "userId",      userId
        );
    }

    void clearApplicationUserData(String pkg, Object observer, int userId) throws Exception {
        mClearApplicationUserData.invoke(
            mPm,
            "packageName", pkg,
            "observer",    observer,
            "userId",      userId
        );
    }

    void setComponentEnabledSetting(ComponentName cmp, int newState,
                                    int flags, int userId) throws Exception {
        mSetComponentEnabledSetting.invoke(
            mPm,
            "componentName", cmp,
            "newState",      newState,
            "flags",         flags,
            "userId",        userId
        );
    }
}
