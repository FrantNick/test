package com.example.focusflight.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object FocusModeManager {

    fun hasDndAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.isNotificationPolicyAccessGranted == true
        } else {
            true
        }
    }

    fun requestDndAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun setFocusMode(context: Context, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager?.isNotificationPolicyAccessGranted == true) {
                manager.setInterruptionFilter(
                    if (enabled) NotificationManager.INTERRUPTION_FILTER_NONE
                    else NotificationManager.INTERRUPTION_FILTER_ALL
                )
            }
        }
    }
}
