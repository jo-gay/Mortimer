package com.example.mojocrow.mortimer

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ContentLoadingProgressBar


class TimerViewActivity : AppCompatActivity() {

    lateinit var progressBar: ContentLoadingProgressBar
    lateinit var progressText: TextView
    lateinit var pauseButton: ImageView
    var timerId: String? = ""
    var timerLength: Int = 0 //length in seconds
    var finished: Boolean = false
    lateinit var timer: MorTimer

    var notificationType: Int? = null
    var notificationDuration: Int? = null
    var notificationUnit: Int? = null
    var notificationBeforeAfter: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer_view)
        progressBar = findViewById(R.id.contentLoadingProgressBar)
        progressBar.progress = 50
        progressText = findViewById(R.id.textView_remainingTime)
        pauseButton = findViewById(R.id.imageButton_pause)

        readTimerDetails()
        setupTimer(timerLength.toLong()*1000)
    }

    /* For now we will cancel the timer if the user exits the timer
    view screen. Todo later: keep timer running in background and list on main activity screen
     */
    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }

    private fun readTimerDetails() {
        val caller = intent
        timerId = caller.getStringExtra("timerId")
        if (timerId == null) {
            Log.e("debugTrace", "No timer Id received from intent - unable to proceed")
            finish()
        }
        val preferences = getSharedPreferences("SAVED_TIMERS", Context.MODE_PRIVATE)
        timerLength = preferences.getInt("Length$timerId", 0)

        notificationType = preferences.getInt("NotificationType$timerId", -1)
        notificationDuration = preferences.getInt("NotificationDuration$timerId", -1)
        notificationUnit = preferences.getInt("NotificationUnit$timerId", -1)
        notificationBeforeAfter = preferences.getInt("NotificationBeforeAfter$timerId", -1)

        Log.d("debugTrace", "Starting a timer with id $timerId and length $timerLength")
        Log.d("debugTrace", "$timerId has notification type $notificationType after $notificationDuration of unit $notificationUnit before/after $notificationBeforeAfter")
    }

    private fun setupTimer(timerMS: Long) {
        timer = MorTimer(timerMS)
        timer.start()
    }

    @Suppress("UNUSED_PARAMETER")
    fun pauseUnpauseTimer(view: View) {
        timer.pauseUnpause()

        // change the button image to reflect the current state
        if (timer.paused) {
            pauseButton.setImageResource(android.R.drawable.ic_media_play)
        }
        else {
            pauseButton.setImageResource(android.R.drawable.ic_media_pause)
            // If we are restarting from a pause, create a new timer object to time the remainder
            setupTimer(timer.remaining)
        }
    }

    fun notificationTriggered() {
        try {
//            // TODO: add options about how it should sound / vibrate
//            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//            val r = RingtoneManager.getRingtone(applicationContext, notification)
//            r.play()
            vibrationNotification()
            Toast.makeText(this,"Timer notification triggered", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("debugTrace", "Notification triggered")
    }

    fun timerTriggered() {
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("debugTrace", "Timer triggered")
    }

    private fun vibrationNotification() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) { // Vibrator availability checking
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.EFFECT_HEAVY_CLICK)) // New vibrate method for API Level 26 or higher
            } else {
                vibrator.vibrate(250) // Vibrate method for below API Level 26
            }
        }
    }



    inner class MorTimer(millisecs: Long) : CountDownTimer(millisecs, 50) {
        var paused = false
        var remaining = millisecs
        var nextNotification: Long = -1 //time to next notification in ms
        init {
            calculateNextNotificationTime(millisecs)
        }

        fun pauseUnpause() {
            paused = ! paused
            Log.d("debugTrace", "timer paused = $paused")
        }

        override fun onTick(millisUntilFinished: Long) {
            // If the pause button has been activated, cancel the timer but record how long is remaining
            if (paused) {
                cancel()
                remaining = millisUntilFinished
            }
            progressText.text = formatMsAsString(millisUntilFinished)
            if (millisUntilFinished < nextNotification) {
                notificationTriggered()
                calculateNextNotificationTime(millisUntilFinished)
            }
        }

        override fun onFinish() {
            progressText.text = formatMsAsString(0)

            //prepare for a restart by setting button back to play, and resetting time to the original time.
            pauseButton.setImageResource(android.R.drawable.ic_media_next)
            remaining = timerLength.toLong() * 1000
            paused = true
            timerTriggered()
            Log.d("debugTrace", "timer onFinish()")
        }

        fun formatMsAsString(ms: Long): String {
            val hrs = ms / 3600000
            val mins = (ms / 60000) % 60
            val secs = (ms / 1000) % 60
            val msecs = ms % 1000

            return "%02d:%02d:%02d:%03d".format(hrs,mins,secs,msecs)
        }

        fun calculateNextNotificationTime(millisUntilFinished: Long) {
            if (notificationType == -1) {
                nextNotification = -1
                return
            }
            val interval = when (notificationUnit) {
                0 -> 3600
                1 -> 60
                2 -> 1
                else -> 0
            }.toLong() * 1000 * notificationDuration!!

            if (notificationType == 0) {
                // Once
                nextNotification = if (notificationBeforeAfter == 0) {
                    // Before end
                    interval
                } else {
                    // After beginning
                    timerLength.toLong() * 1000 - interval
                }
            }
            else {
                // Every x hrs/mins/secs
                nextNotification = if (notificationBeforeAfter == 0) {
                    // Before end
                    millisUntilFinished - (millisUntilFinished % interval)
                }
                else {
                    // After beginning
                    val offset = (timerLength.toLong() * 1000) % interval
                    millisUntilFinished - ((millisUntilFinished - offset) % interval)
                }
                if (nextNotification >= timerLength.toLong() * 1000) {
                    // If the timer length is a multiple of the interval then we don't want
                    // a notification at the beginning
                    nextNotification -= interval
                }
            }

            if (millisUntilFinished < nextNotification) {
                // All notifications have been triggered.
                nextNotification = -1
            }
        }
    }
}
