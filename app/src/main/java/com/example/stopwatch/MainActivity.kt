package com.example.stopwatch

import android.app.Activity
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var tvTimeDisplay: TextView

    // Stopwatch buttons
    private lateinit var btnStopwatchStart: Button
    private lateinit var btnStopwatchPause: Button
    private lateinit var btnStopwatchReset: Button
    private lateinit var lvLaps: ListView

    // Countdown buttons / input
    private lateinit var etCountdownInput: EditText
    private lateinit var btnCountdownStart: Button
    private lateinit var btnCountdownPause: Button
    private lateinit var btnCountdownReset: Button

    // Stopwatch state
    private var stopwatchStartTime = 0L
    private var stopwatchTimeBuffer = 0L
    private var stopwatchUpdateTime = 0L
    private var stopwatchIsRunning = false
    private val stopwatchHandler = Handler(Looper.getMainLooper())
    private val lapList = mutableListOf<String>()
    private var lapCounter = 1
    private lateinit var lapAdapter: ArrayAdapter<String>

    // Countdown state
    private var countdownTotalMillis = 0L
    private var countdownRemainingMillis = 0L
    private var countdownIsRunning = false
    private val countdownHandler = Handler(Looper.getMainLooper())

    // MediaPlayer for finish sound
    private var mediaPlayer: MediaPlayer? = null

    private val stopwatchRunnable = object : Runnable {
        override fun run() {
            val timeElapsed = System.currentTimeMillis() - stopwatchStartTime
            stopwatchUpdateTime = stopwatchTimeBuffer + timeElapsed

            val secs = (stopwatchUpdateTime / 1000).toInt()
            val mins = secs / 60
            val remSecs = secs % 60
            val millis = (stopwatchUpdateTime % 1000).toInt()

            tvTimeDisplay.text = String.format(Locale.getDefault(), "%02d:%02d:%03d", mins, remSecs, millis)

            stopwatchHandler.postDelayed(this, 50)
        }
    }

    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (countdownRemainingMillis <= 0L) {
                tvTimeDisplay.text = "00:00:00"
                countdownIsRunning = false
                playFinishSound()
                return
            }

            countdownRemainingMillis -= 1000
            val secs = (countdownRemainingMillis / 1000).toInt()
            val mins = secs / 60
            val remSecs = secs % 60

            tvTimeDisplay.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", mins, remSecs, 0)

            countdownHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind views
        tvTimeDisplay = findViewById(R.id.tvTimeDisplay)

        btnStopwatchStart = findViewById(R.id.btnStopwatchStart)
        btnStopwatchPause = findViewById(R.id.btnStopwatchPause)
        btnStopwatchReset = findViewById(R.id.btnStopwatchReset)
        lvLaps = findViewById(R.id.lvLaps)

        etCountdownInput = findViewById(R.id.etCountdownInput)
        btnCountdownStart = findViewById(R.id.btnCountdownStart)
        btnCountdownPause = findViewById(R.id.btnCountdownPause)
        btnCountdownReset = findViewById(R.id.btnCountdownReset)

        lapAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, lapList)
        lvLaps.adapter = lapAdapter

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)

        // Stopwatch buttons listeners
        btnStopwatchStart.setOnClickListener {
            if (!stopwatchIsRunning) {
                stopwatchStartTime = System.currentTimeMillis()
                stopwatchHandler.post(stopwatchRunnable)
                stopwatchIsRunning = true
            }
        }

        btnStopwatchPause.setOnClickListener {
            if (stopwatchIsRunning) {
                stopwatchTimeBuffer += System.currentTimeMillis() - stopwatchStartTime
                stopwatchHandler.removeCallbacks(stopwatchRunnable)
                stopwatchIsRunning = false

                val lapTime = stopwatchTimeBuffer
                val mins = (lapTime / 1000 / 60)
                val secs = (lapTime / 1000 % 60)
                val millisPart = (lapTime % 1000)
                val lapString = String.format(Locale.getDefault(),
                    "Lap %d: %02d:%02d:%03d", lapCounter, mins, secs, millisPart)
                lapCounter++  // increment for next lap
                lapList.add(0, lapString)  // newest lap at top
                lapAdapter.notifyDataSetChanged()
            }
        }

        btnStopwatchReset.setOnClickListener {
            stopwatchHandler.removeCallbacks(stopwatchRunnable)
            stopwatchIsRunning = false
            stopwatchStartTime = 0L
            stopwatchTimeBuffer = 0L
            stopwatchUpdateTime = 0L
            tvTimeDisplay.text = "00:00:00"
            lapList.clear()
            lapAdapter.notifyDataSetChanged()
            lapCounter = 1
        }

        // Countdown buttons listeners
        btnCountdownStart.setOnClickListener {
            val input = etCountdownInput.text.toString()
            if (input.isBlank()) {
                Toast.makeText(this, "Enter seconds first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!countdownIsRunning) {
                val secs = input.toLong()
                countdownTotalMillis = secs * 1000
                countdownRemainingMillis = countdownTotalMillis
                countdownHandler.post(countdownRunnable)
                countdownIsRunning = true
            }
        }

        btnCountdownPause.setOnClickListener {
            if (countdownIsRunning) {
                countdownHandler.removeCallbacks(countdownRunnable)
                countdownIsRunning = false
            }
        }

        btnCountdownReset.setOnClickListener {
            countdownHandler.removeCallbacks(countdownRunnable)
            countdownIsRunning = false
            tvTimeDisplay.text = "00:00:00"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun playFinishSound() {
        mediaPlayer?.start()
    }
}
