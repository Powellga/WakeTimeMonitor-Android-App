package com.example.waketimemonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ColorChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val color = intent.getStringExtra("color") ?: "#FFFFFF"  // Default to white if no color is provided

        // Log the color change for debugging
        Log.d("WakeTimeMonitor", "Receiver triggered for color: $color")

        // Broadcast an intent back to MainActivity to update the UI
        val updateIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("color", color)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(updateIntent)
    }
}
