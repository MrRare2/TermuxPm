package com.termux.termuxpm;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PermissionInfo;
import android.content.pm.PermissionGroupInfo;

import android.net.Uri;
import android.os.IBinder;
import android.os.UserHandle;

import java.lang.reflect.Method;

import java.util.Map;
import java.util.List;

@SuppressLint("PrivateApi")
class IPermissionManager {
    private final Object mPerm;

    private final CrossVersionReflectedMethod mGetAllPermissionGroups;
    private final CrossVersionReflectedMethod mQueryPermissionsPerGroup;

    IPermissionManager() throws Exception {
        IBinder binder = (IBinder) Class
            .forName("android.os.ServiceManager")
            .getMethod("getService", String.class)
            .invoke(null, "permissionmgr");

        Class<?> stub = Class.forName("android.permission.IPermissionManager$Stub");
        Method asInterface = stub.getMethod("asInterface", IBinder.class);
        mPerm = asInterface.invoke(null, binder);

        Class<?> permClass = mPerm.getClass();
	mGetAllPermissionGroups = new CrossVersionReflectedMethod(permClass)
	    .tryMethodVariantInexact("getAllPermissionGroups",
		int.class, "flags",
		int.class, "userId")
	    .tryMethodVariantInexact("getAllPermissionGroups",
		int.class, "flags");

	mQueryPermissionsPerGroup = new CrossVersionReflectedMethod(permClass)
	    .tryMethodVariantInexact("queryPermissionsByGroup",
		String.class, "permissionGroup",
		int.class, "flags");
    }

    PermissionGroupInfo[] getAllPermissionGroups(int flags) throws Exception {
	Object mFl = mGetAllPermissionGroups.invoke(mPerm, "flags", flags);
	Method gLm = mFl.getClass().getMethod("getList");
	return ((List<PermissionGroupInfo>) gLm.invoke(mFl)).toArray(new PermissionGroupInfo[0]);
    }

    PermissionGroupInfo[] getAllPermissionGroups(int flags, int userId) throws Exception {
	Object mFl = mGetAllPermissionGroups.invoke(mPerm, "flags", flags, "userId", userId);
	Method gLm = mFl.getClass().getMethod("getList");
	return ((List<PermissionGroupInfo>) gLm.invoke(mFl)).toArray(new PermissionGroupInfo[0]);
    }

    PermissionInfo[] queryPermissionsByGroup(String group, int flags) throws Exception {
	Object mFl = mQueryPermissionsPerGroup.invoke(
            mPerm,
            "permissionGroup", group,
            "flags", flags
        );
        Method gLm = mFl.getClass().getMethod("getList");
        return ((List<PermissionInfo>) gLm.invoke(mFl)).toArray(new PermissionInfo[0]);
    }
}
