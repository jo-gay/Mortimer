package com.example.mojocrow.mortimer

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.lang.Exception

class SavedTimerManager (context: Context, saveListFileName: String = "SAVED_TIMERS") {
    /* Timers need to be saved and loaded from various different places so encapsulate the code here */

    private val prefs: SharedPreferences = context.getSharedPreferences(saveListFileName, Context.MODE_PRIVATE)
    private val notificationIntKeys = arrayOf("NotificationType", "NotificationDuration", "NotificationUnit", "NotificationBeforeAfter")
    private val lengthIntKey = "Length"

    fun getAllSavedTimers(): MutableList<Array<String>> {
        // TODO: Start new thread to handle this
        val allEntries: List<String> =
            prefs.all.map { it.key }.filter { it.startsWith(lengthIntKey) }.map { it.drop(6) }

        // For each saved timer, create an array of three strings containing the length, the notification details
        // (for display in a list), and the id (for reference).
        val listItems = mutableListOf<Array<String>>()
        for ((idx, timerId) in allEntries.withIndex()) {
            val description = timerDescription(timerId)
            listItems.add(arrayOf(description[0], description[1], timerId))
//            Log.d("debugTrace", "Read timer ${listItems[idx][2]}: ${listItems[idx][0]}, ${listItems[idx][1]}")
        }
        return listItems
    }
    fun timerDescription(timerId: String): Array<String> {
        /* For a given timer ID, return a description of the timer in two parts, the first giving
        the length of the timer and the second, the notification information
         */
        val length = prefs.getInt("$lengthIntKey$timerId", -1)
        if(length == -1) {
            Log.e("errorTrace debugTrace", "Timer ID $timerId not found in shared prefs file")
            return arrayOf("", "")
        }

        var notificationDescription = "Notification: " //TODO: extract strings.
        try {
            notificationDescription += when (prefs.getInt(
                "${notificationIntKeys[0]}$timerId",
                -1
            )) {
                -1 -> "None"
                1 -> "Every "
                else -> ""
            }
            if (prefs.contains("${notificationIntKeys[1]}$timerId")) {
                notificationDescription += "${prefs.getInt("${notificationIntKeys[1]}$timerId", -1)} "
            }
            notificationDescription += when (prefs.getInt(
                "${notificationIntKeys[2]}$timerId",
                -1
            )) {
                0 -> "Hrs "
                1 -> "Mins "
                2 -> "Secs "
                else -> ""
            }
            notificationDescription += when (prefs.getInt(
                "${notificationIntKeys[3]}$timerId",
                -1
            )) {
                0 -> "before end"
                1 -> "after beginning"
                else -> ""
            }
        } catch (e: Exception) {
            Log.d("debugTrace", "Non-int in prefs file for timer $timerId")
        }
        val HMS = lengthToHMS(length)
        return arrayOf("Length: %02d:%02d:%02d ".format(HMS[0], HMS[1], HMS[2]), notificationDescription)
    }
    fun deleteTimer(timerId: String) {
        timerDescription(timerId).also {
            Log.d("debugTrace", "Deleting timer $timerId from saved list: ${it[0]}; ${it[1]}")
        }
        if(!prefs.contains("$lengthIntKey$timerId")) {
            Log.d("debugTrace", "Error: timer not found")
        }
        prefs.edit().apply {
            remove("$lengthIntKey$timerId")
            for (k in notificationIntKeys) {
                remove("$k$timerId")
            }
            apply()
        }
    }
    fun getLength(timerId: String): Int {
        /* Read and return the values for the notification settings for the given timer */
        return prefs.getInt("$lengthIntKey$timerId", -1)
    }
    fun getNotificationValues(timerId: String): List<Int> {
        /* Read and return the values for the notification settings for the given timer */
        return notificationIntKeys.map {
            prefs.getInt("$it$timerId", -1)
        }
    }
    fun saveTimer(timerId: String, timerLength: Int, notification: List<Int>) {
        prefs.edit().apply {
            putInt("$lengthIntKey$timerId", timerLength)
            if (notification.isNotEmpty()) {
                for((i, key) in notificationIntKeys.withIndex()) {
                    putInt("$key$timerId", notification[i])
                }
            }
            else {
                for(key in notificationIntKeys) {
                    remove("$key$timerId")
                }
            }
            apply()
        }
        timerDescription(timerId).also {
            Log.d("debugTrace", "saved timer $timerId to file: ${it[0]}; ${it[1]}")
        }

    }
    fun lengthToHMS(lenInSecs: Int): IntArray {
        /* Convert time in seconds to an array containing hours, minutes, seconds as integers
         */
        val hrs = lenInSecs / 3600
        val mins = (lenInSecs / 60) % 60
        val secs = (lenInSecs) % 60
        return intArrayOf(hrs, mins, secs)
    }
}