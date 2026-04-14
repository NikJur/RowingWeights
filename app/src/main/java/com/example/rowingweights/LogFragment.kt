package com.example.rowingweights

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * LogFragment handles the data entry interface.
 * It writes new records to the local database.
 */
class LogFragment : Fragment(R.layout.fragment_log) {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var dateSelector: TextView

    // Initialises a Calendar object to now
    private var selectedCalendar = Calendar.getInstance()

    /**
     * Executes immediately after the view hierarchy inflates.
     * It binds interface elements and defines click listeners.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        dateSelector = view.findViewById(R.id.dateSelector)
        val weightInput = view.findViewById<EditText>(R.id.weightInput)
        val submitButton = view.findViewById<Button>(R.id.submitButton)

        // Sets the initial text to today's date upon opening the tab
        updateDateDisplay()

        // Opens the native calendar widget when the user taps the date
        dateSelector.setOnClickListener {
            showDatePicker()
        }

        submitButton.setOnClickListener {
            val weightValue = weightInput.text.toString().toFloatOrNull()

            if (weightValue != null) {
                // Formats the selected calendar date for database sorting (ISO format)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayDate = dateFormat.format(selectedCalendar.time)

                // Inserts the validated value into the database
                dbHelper.insertWeight(todayDate, weightValue)
                weightInput.text.clear()

                // Provides brief visual confirmation to the user
                Toast.makeText(requireContext(), "Weight saved for \$dbDate", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Generates and displays the Android DatePickerDialog.
     * It updates the selectedCalendar variable when the user makes a choice.
     */
    private fun showDatePicker() {
        val year = selectedCalendar.get(Calendar.YEAR)
        val month = selectedCalendar.get(Calendar.MONTH)
        val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Overwrites the calendar variable with the user's new selection
                selectedCalendar.set(Calendar.YEAR, selectedYear)
                selectedCalendar.set(Calendar.MONTH, selectedMonth)
                selectedCalendar.set(Calendar.DAY_OF_MONTH, selectedDay)

                // Refreshes the visual text on the screen
                updateDateDisplay()
            },
            year,
            month,
            day
        )

        // Restricts the calendar so you cannot log weights in the future
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    /**
     * Converts the internal calendar data into a human-readable string.
     * EEEE represents the full weekday name; dd.MM.yyyy formats the numbers.
     */
    private fun updateDateDisplay() {
        val displayFormat = SimpleDateFormat("EEEE dd.MM.yyyy", Locale.getDefault())
        dateSelector.text = displayFormat.format(selectedCalendar.time)
    }
}