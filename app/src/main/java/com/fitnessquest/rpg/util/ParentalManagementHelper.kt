package com.fitnessquest.rpg.util

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.os.UserManager

object ParentalManagementHelper {

    /**
     * Checks if the current Android environment has active parental controls,
     * child supervision (e.g. Google Family Link), or enterprise/MDM management restrictions.
     */
    fun isDeviceManagedOrRestricted(context: Context): Boolean {
        return runCatching {
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

            var restricted = false

            if (userManager != null) {
                // Check common restrictions enforced on supervised/child accounts
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (userManager.isManagedProfile) restricted = true
                }

                val restrictions = listOf(
                    UserManager.DISALLOW_MODIFY_ACCOUNTS,
                    UserManager.DISALLOW_APPS_CONTROL,
                    UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES
                )
                if (restrictions.any { userManager.hasUserRestriction(it) }) {
                    restricted = true
                }

                // Check active user restrictions bundle
                val userBundle = userManager.userRestrictions
                if (!userBundle.isEmpty) {
                    val keys = userBundle.keySet()
                    if (keys.any { it.startsWith("no_", ignoreCase = true) || it.startsWith("disallow_", ignoreCase = true) }) {
                        restricted = true
                    }
                }
            }

            if (devicePolicyManager != null) {
                if (devicePolicyManager.isDeviceOwnerApp(context.packageName) ||
                    devicePolicyManager.isProfileOwnerApp(context.packageName) ||
                    !devicePolicyManager.activeAdmins.isNullOrEmpty()
                ) {
                    restricted = true
                }
            }

            restricted
        }.getOrDefault(false)
    }
}
