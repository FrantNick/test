package com.example.focusflight.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

class HapticsHelper(context: Context) {

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    fun pulseStrong() {
        vibrator?.let {
            if (!it.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(120)
            }
        }
    }

    fun pulseSoft() {
        vibrator?.let {
            if (!it.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(60)
            }
        }
    }
}
