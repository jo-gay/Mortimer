package com.example.mojocrow.mortimer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var listViewSavedTimers: ListView
    private lateinit var savedTimerMan: SavedTimerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        savedTimerMan = SavedTimerManager(this)
        listViewSavedTimers = findViewById(R.id.listView_savedList)
        readSavedTimers()
    }

    override fun onResume() {
        /* Refresh the list of saved timers since one of them may have been
           amended during the create timer activity
         */
        super.onResume()
        readSavedTimers()
    }


    private fun readSavedTimers() {
        val listItems = savedTimerMan.getAllSavedTimers()

        listViewSavedTimers.adapter = SavedTimerAdapter(this, listItems, savedTimerMan)
        listViewSavedTimers.setOnItemClickListener {_, _, position, _ ->
            Log.d("debugTrace","List of saved timers: onItemClick")
            val timerDetails = listViewSavedTimers.adapter.getItem(position) as Array<String>
            val timerId = timerDetails[2]
            val i = Intent(this, CreateTimerActivity::class.java)
            i.putExtra("timerId", timerId)
            startActivity(i)
        }

    }

    @Suppress("UNUSED_PARAMETER")
    fun startCreateTimerActivity(v: View) {
        startActivity(Intent(this, CreateTimerActivity::class.java))
    }

    class SavedTimerAdapter(
                        context: Context,
                        private val dataSource: MutableList<Array<String>>,
                        private val prefsManager: SavedTimerManager) : BaseAdapter() {

        private val inflater: LayoutInflater
                = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getCount(): Int {
            return dataSource.size
        }

        override fun getItem(position: Int): Any {
            return dataSource[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val rowView = inflater.inflate(R.layout.saved_timer_list_layout, parent, false)

            val titleTextView = rowView.findViewById(R.id.textViewSavedTimerDuration) as TextView
            val subtitleTextView = rowView.findViewById(R.id.textViewSavedTimerNotification) as TextView
            val deleteItemImageView = rowView.findViewById(R.id.textViewSavedTimerDeleteButton) as ImageView

            val timerDescription = getItem(position) as Array<String>
            titleTextView.text = timerDescription[0]
            subtitleTextView.text = timerDescription[1]
            deleteItemImageView.setOnClickListener {
                //First delete the item from the shared preferences file. Then delete it from the
                //list (rather than re-loading the full list).
                deleteFromSharedPrefs(timerDescription[2])
                dataSource.removeAt(position)
                notifyDataSetChanged()
//                Toast.makeText(context, "Delete saved timer onClick listener $position", Toast.LENGTH_SHORT).show()
            }

            return rowView
        }

        private fun deleteFromSharedPrefs(timerId: String) {
            Log.d("debugTrace", "Timer List adapter deleting timer $timerId from saved list")
            prefsManager.deleteTimer(timerId)
        }
    }
}
