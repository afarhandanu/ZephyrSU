package com.sukisu.ultra.media

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationManager {
    fun vibrate(context: Context) {
        if (!VibrationConfig.isVibrationEnabled || VibrationConfig.vibrationIntensity <= 0f) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            } ?: return
            if (vibrator.hasVibrator()) {
                val intensity = (VibrationConfig.vibrationIntensity * 255).toInt().coerceIn(1, 255)
                val amplitude = if (vibrator.hasAmplitudeControl()) intensity else VibrationEffect.DEFAULT_AMPLITUDE
                vibrator.vibrate(VibrationEffect.createOneShot(30L, amplitude))
            }
        } catch (e: Exception) {
            android.util.Log.e("VibrationManager", "Failed to vibrate", e)
        }
    }
}
