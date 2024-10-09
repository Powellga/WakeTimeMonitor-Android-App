package com.example.waketimemonitor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.waketimemonitor.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val logList = mutableListOf<String>()
    private val TAG = "WakeTimeMonitor"
    private var lastShakeTimestamp: Long = 0

    // Constants for shake detection
    private val SHAKE_THRESHOLD_GRAVITY = 2.7f
    private val SHAKE_DEBOUNCE_DELAY = 1000 // 1 second

    // Preferences for storing logs
    private val PREFS_NAME = "WakeTimePrefs"
    private val LOG_LIST_KEY = "LogList"

    // Handler for periodic tasks
    private val handler = Handler(Looper.getMainLooper())
    private val checkTimeRunnable = object : Runnable {
        override fun run() {
            updateTimeColor()
            handler.postDelayed(this, 60000) // Check every minute
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load stored log entries
        loadLogEntries()

        // Initialize button states
        binding.resetButton.isEnabled = false
        binding.offButton.isEnabled = false

        // Unlock checkbox controls button enable/disable state
        binding.unlockCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.resetButton.isEnabled = isChecked
            binding.offButton.isEnabled = isChecked
        }

        // Set up RecyclerView
        binding.logRecyclerView.layoutManager = LinearLayoutManager(this)

        // Add click listener to the Reset button
        binding.resetButton.setOnClickListener {
            if (binding.unlockCheckbox.isChecked) {
                logList.clear()
                updateLog()
                saveLogEntries() // Save empty log to storage
                Log.d(TAG, "Log reset")
            }
        }

        // Add click listener to the Off button
        binding.offButton.setOnClickListener {
            if (binding.unlockCheckbox.isChecked) {
                finish() // Close the app
                Log.d(TAG, "App closed")
            }
        }

        // Add click listener to the Display Log button (this is the corrected part)
        binding.displayLogButton.setOnClickListener {
            toggleLogVisibility()
        }

        // Capture screen touches and log the time
        binding.colorDisplay.setOnClickListener {
            logCurrentTime()
        }

        // Start the periodic time check
        handler.post(checkTimeRunnable)

        // Set up the accelerometer for shake detection
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShakeTimestamp > SHAKE_DEBOUNCE_DELAY) {
                    lastShakeTimestamp = currentTime
                    logCurrentTime()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    private fun logCurrentTime() {
        val currentTime = Calendar.getInstance().time.toString()
        if (logList.isEmpty() || logList.last() != currentTime) {
            logList.add("Wakeup ${logList.size + 1}: $currentTime")
            updateLog()
            saveLogEntries() // Save log to storage
        }
    }

    private fun updateTimeColor() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val timeText = when {
            hour in 1..2 -> "After 1 AM"
            hour in 3..6 -> "After 3 AM"
            else -> "After 7 AM"
        }
        Log.d(TAG, "Time: $hour, Displaying: $timeText")

        binding.colorDisplay.text = timeText
    }

    private fun updateLog() {
        binding.logRecyclerView.adapter = LogAdapter(logList)
    }

    // This method will toggle the visibility of the log entries
    private fun toggleLogVisibility() {
        if (binding.logRecyclerView.visibility == View.GONE) {
            binding.logRecyclerView.visibility = View.VISIBLE
            binding.displayLogButton.text = "Hide Log" // Change button text to 'Hide Log'
        } else {
            binding.logRecyclerView.visibility = View.GONE
            binding.displayLogButton.text = "Display Log" // Change button text to 'Display Log'
        }
    }

    private fun saveLogEntries() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val gson = Gson()
        val json = gson.toJson(logList)
        editor.putString(LOG_LIST_KEY, json)
        editor.apply()
    }

    private fun loadLogEntries() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPrefs.getString(LOG_LIST_KEY, null)
        if (json != null) {
            val gson = Gson()
            val type: Type = object : TypeToken<MutableList<String>>() {}.type
            logList.addAll(gson.fromJson(json, type))
            updateLog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkTimeRunnable) // Stop the handler when the activity is destroyed
        sensorManager.unregisterListener(this) // Unregister the sensor when the activity is destroyed
    }
}
