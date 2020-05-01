package com.example.mojocrow.mortimer

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import kotlin.math.roundToInt
import kotlin.random.Random

class CreateTimerActivity : AppCompatActivity() {
    private lateinit var savedTimerMan: SavedTimerManager

    private lateinit var hourPicker: NumberPicker
    private lateinit var minPicker: NumberPicker
    private lateinit var secPicker: NumberPicker

    private lateinit var notifierType: Spinner
    private lateinit var notifierFreq: NumberPicker
    private lateinit var notifierUnit: Spinner
    private lateinit var notifierBeforeAfter: Spinner
    private var notificationActive = false

    private lateinit var timerId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_timer)
        savedTimerMan = SavedTimerManager(this)

        /* If the intent includes a timer ID, we are editing an existing timer and should
        populate the time picker with the values from the preferences file. Otherwise start at 0.
         */
        val caller = intent
        timerId = caller.getStringExtra("timerId") ?: Random.nextInt().toString()

        setupTimerPickers()
        setupNotificationPickers()
    }

    private fun setupTimerPickers() {
        /* Read the length of the timer from the preferences file (zero if it's a new one);
           convert to hours, minutes and seconds; and initialize the hour/min/second pickers
         */
        val HMS = savedTimerMan.lengthToHMS(savedTimerMan.getLength(timerId))

        /* Set the range and starting values for the pickers */
        hourPicker = findViewById<NumberPicker>(R.id.picker_createTimerHour).apply {
            maxValue = 23
            minValue = 0
            value = HMS[0]
        }
        minPicker = findViewById<NumberPicker>(R.id.picker_createTimerMin).apply {
            maxValue = 59
            minValue = 0
            value = HMS[1]
        }
        secPicker = findViewById<NumberPicker>(R.id.picker_createTimerSec).apply {
            maxValue = 59
            minValue = 0
            value = HMS[2]
        }
    }

    private fun makeNotificationActive() {
        /* Something has been selected in the notification panel. Make the notification active
           and activate the button allowing the user to deactivate the notification, if not already active
         */
        if (!notificationActive) {
            notificationActive = true
            findViewById<ImageButton>(R.id.button_addNotification).visibility = View.VISIBLE
            findViewById<ImageButton>(R.id.button_addNotification).setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        }
    }

    private fun setupNotificationPickers() {
        /* Read the notification settings from the preferences file (none if it's not found);
           and initialize the notification pickers accordingly. If no data is found then set
           the notification to inactive to begin with.
         */

        var (ntnType, ntnFreq, ntnUnit, ntnBeforeAfter) = savedTimerMan.getNotificationValues(timerId)
        if (ntnType == -1) {
            // No notification settings were found. Hide 'delete' button and set pickers to default values.
            findViewById<ImageButton>(R.id.button_addNotification).visibility = View.INVISIBLE
            ntnType = 0 // Once / Every
            ntnFreq = 0 // Interval
            ntnUnit = 1 // Hours / Mins / Seconds
            ntnBeforeAfter = 0 // Before end / After beginning
        }
        else {
            // Notification saved values have been loaded into the vars. Make sure the notification
            // is marked as active and the delete button is available.
            makeNotificationActive()
        }
        notifierType = findViewById(R.id.spinner_notificationOnce)
        ArrayAdapter.createFromResource(notifierType.context,
            R.array.onceEvery,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            notifierType.adapter = adapter
        }
        notifierType.setSelection(ntnType)
        notifierFreq = findViewById<NumberPicker>(R.id.picker_notificationInterval).apply {
            maxValue = 59
            minValue = 0
            value = ntnFreq
            setOnValueChangedListener{ _, _, _ -> makeNotificationActive()}
        }
        notifierUnit = findViewById(R.id.spinner_notificationUnits)
        ArrayAdapter.createFromResource(this,
            R.array.units,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            notifierUnit.adapter = adapter
        }
        notifierUnit.setSelection(ntnUnit)
        notifierBeforeAfter = findViewById(R.id.spinner_notificationBefore)
        ArrayAdapter.createFromResource(this,
            R.array.beforeAfter,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            notifierBeforeAfter.adapter = adapter
        }
        notifierBeforeAfter.setSelection(ntnBeforeAfter)

    }

    private fun resetNotificationPickers() {
        notifierType.setSelection(0)
        notifierFreq.value = 0
        notifierUnit.setSelection(1)
        notifierBeforeAfter.setSelection(0)
    }

    fun deleteNotification(v: View){
        /* ImageButton callback: Turn off the notification for this timer */
        Log.d("debugTrace","deleteNotification()")
        if(notificationActive) {
            // If the notification is active (not zeroed out) then delete (zero out) and set inactive.
            notificationActive = !notificationActive
            resetNotificationPickers()
            v.visibility = View.INVISIBLE
        }
    }

    fun activateTimer(v: View) {
        /* ImageButton callback: Save and then activate the timer that has just been created or modified */
        saveCreateTimer(v)
        val intent = Intent(this, TimerViewActivity::class.java)
        intent.putExtra("timerId", timerId)
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER")
    fun saveCreateTimer(v: View) {
        /* Button callback: Save a timer to the shared preferences file
        *  Also used by the start button */
//        val preferences = getSharedPreferences("SAVED_TIMERS", Context.MODE_PRIVATE)
//

        val timerLengthSecs = hourPicker.value * 3600 +
                minPicker.value * 60 +
                secPicker.value

        val notificationSettings = listOf(
            notifierType.selectedItemPosition,
            notifierFreq.value,
            notifierUnit.selectedItemPosition,
            notifierBeforeAfter.selectedItemPosition
        )
        savedTimerMan.saveTimer(timerId, timerLengthSecs, notificationSettings)
//        val notificationType = notifierType.selectedItemPosition
//        val notificationDuration = notifierFreq.value
//        val notificationUnit = notifierUnit.selectedItemPosition
//        val notificationBeforeAfter = notifierBeforeAfter.selectedItemPosition
//        preferences.edit().apply {
//            putInt("Length$timerId", timerLengthSecs)
//            if (notificationActive) {
//                putInt("NotificationType$timerId", notificationType)
//                putInt("NotificationDuration$timerId", notificationDuration)
//                putInt("NotificationUnit$timerId", notificationUnit)
//                putInt("NotificationBeforeAfter$timerId", notificationBeforeAfter)
//            }
//            else {
//                remove("NotificationType$timerId")
//                remove("NotificationDuration$timerId")
//                remove("NotificationUnit$timerId")
//                remove("NotificationBeforeAfter$timerId")
//            }
//            apply()
//        }
//        Log.d("debugTrace","saved timer $timerId to file. Length $timerLengthSecs, notification $notificationType after $notificationDuration ${notificationUnit}s $notificationBeforeAfter")
    }

    @Suppress("UNUSED_PARAMETER")
    fun cancelCreateTimer(v: View) {
        /* Button callback: cancel creating / modifying this timer and go back to the previous activity */
        finish()
    }

}
