package com.example.rowingweights

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import android.app.AlertDialog

/**
 * PlotFragment visualises historical data using proportional time scaling.
 */
class PlotFragment : Fragment(R.layout.fragment_plot) {

    private lateinit var weightChart: LineChart
    private lateinit var dbHelper: DatabaseHelper

    // UI Elements for the Edit Panel
    private lateinit var editPanel: LinearLayout
    private lateinit var editDateDisplay: TextView
    private lateinit var editWeightInput: EditText

    // Stores the exact database date format of the selected point so we can overwrite it
    private var activeEditDateDbFormat: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        weightChart = view.findViewById(R.id.weightChart)
        dbHelper = DatabaseHelper(requireContext())

        editPanel = view.findViewById(R.id.editPanel)
        editDateDisplay = view.findViewById(R.id.editDateDisplay)
        editWeightInput = view.findViewById(R.id.editWeightInput)

        setupChartAppearance()
        setupEditPanelListeners(view)

        // Wires up the zoom buttons
        view.findViewById<Button>(R.id.btnWeek).setOnClickListener { setTimeView(7f) }
        view.findViewById<Button>(R.id.btnMonth).setOnClickListener { setTimeView(30f) }
        view.findViewById<Button>(R.id.btnYear).setOnClickListener { setTimeView(365f) }
        view.findViewById<Button>(R.id.btnAll).setOnClickListener {
            weightChart.fitScreen()
            weightChart.invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshChartFromDatabase()
    }

    private fun setupChartAppearance() {
        weightChart.description.isEnabled = false
        weightChart.legend.isEnabled = false
        weightChart.setDrawGridBackground(false)
        weightChart.isScaleYEnabled = false

        val xAxis = weightChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.DKGRAY
        xAxis.labelRotationAngle = -45f

        // Attaches custom translator to the X-Axis
        xAxis.valueFormatter = DateAxisFormatter()

        val leftAxis = weightChart.axisLeft
        leftAxis.textColor = Color.DKGRAY
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = "#E0E0E0".toColorInt()
        leftAxis.gridLineWidth = 1f

        weightChart.axisRight.isEnabled = false

        // Wires up the chart to listen for taps on the data points
        weightChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight?) {
                openEditPanel(e)
            }

            override fun onNothingSelected() {
                closeEditPanel()
            }
        })
    }

    /**
     * Binds click listeners to the Edit Panel buttons.
     * Includes an AlertDialog safeguard to prevent accidental database deletions.
     */
    private fun setupEditPanelListeners(view: View) {
        // Hides the panel and clears the chart crosshairs
        view.findViewById<Button>(R.id.btnCancelEdit).setOnClickListener {
            closeEditPanel()
            weightChart.highlightValues(null)
        }

        // Saves the updated text field data to the SQLite database
        view.findViewById<Button>(R.id.btnSaveEdit).setOnClickListener {
            val newWeight = editWeightInput.text.toString().toFloatOrNull()
            if (newWeight != null && activeEditDateDbFormat.isNotEmpty()) {
                dbHelper.insertWeight(activeEditDateDbFormat, newWeight)

                Toast.makeText(requireContext(), "Entry updated", Toast.LENGTH_SHORT).show()
                closeEditPanel()
                refreshChartFromDatabase()
            }
        }

        // Triggers a confirmation pop-up before executing the database deletion
        view.findViewById<Button>(R.id.btnDeleteEdit).setOnClickListener {
            if (activeEditDateDbFormat.isNotEmpty()) {

                // Constructs and displays the native Android confirmation dialog
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Deletion")
                    .setMessage("Are you sure you want to permanently delete this weight record?")
                    .setPositiveButton("Delete") { _, _ ->
                        // This block only executes if the user explicitly clicks "Delete"
                        dbHelper.deleteWeight(activeEditDateDbFormat)
                        Toast.makeText(requireContext(), "Entry deleted", Toast.LENGTH_SHORT).show()
                        closeEditPanel()
                        refreshChartFromDatabase()
                    }
                    .setNegativeButton("Cancel", null) // Closes the dialog without taking action
                    .show()
            }
        }
    }

    /**
     * Extracts the date from the clicked point and reveals the hidden editing tools.
     */
    private fun openEditPanel(entry: Entry) {
        // Convert the X-axis "Days" back into milliseconds
        val millis = entry.x.toLong() * 1000L * 60L * 60L * 24L
        val dateObject = Date(millis)

        // Formats for displaying to the user (e.g., Monday, 15 Apr 2026)
        val displayFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
        displayFormat.timeZone = TimeZone.getTimeZone("UTC")
        // Formats for saving to the database securely
        val dbFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dbFormat.timeZone = TimeZone.getTimeZone("UTC")

        activeEditDateDbFormat = dbFormat.format(dateObject)

        // Populates the panel with the existing data
        editDateDisplay.text = displayFormat.format(dateObject)
        editWeightInput.setText(entry.y.toString())

        // Reveals the panel
        editPanel.visibility = View.VISIBLE
    }

    private fun closeEditPanel() {
        editPanel.visibility = View.GONE
        activeEditDateDbFormat = ""
    }

    private fun refreshChartFromDatabase() {
        val historicalData = dbHelper.getAllWeights()

        if (historicalData.isEmpty()) {
            weightChart.clear()
            return
        }

        val weightEntries = mutableListOf<Entry>()
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")

        historicalData.forEach { record ->
            val rawDate = record.first
            val weight = record.second

            try {
                val parsedDate = inputFormat.parse(rawDate)
                if (parsedDate != null) {
                    // Converts the date into "Days" so the chart calculates proportional gaps
                    val daysSinceEpoch = (parsedDate.time / (1000f * 60f * 60f * 24f))
                    weightEntries.add(Entry(daysSinceEpoch, weight))
                }
            } catch (_: Exception) {
                // Skips corrupted dates
            }
        }

        // Sorts entries by time to prevent rendering glitches
        weightEntries.sortBy { it.x }

        val dataSet = LineDataSet(weightEntries, "Daily Weight")

        val modernBlue = "#007AFF".toColorInt()
        dataSet.color = modernBlue
        dataSet.lineWidth = 2.5f
        dataSet.mode = LineDataSet.Mode.LINEAR // Reverted to straight lines!

        dataSet.setCircleColor(modernBlue)
        dataSet.circleRadius = 5f
        dataSet.circleHoleRadius = 2.5f

        dataSet.valueTextColor = Color.DKGRAY
        dataSet.valueTextSize = 10f

        dataSet.setDrawFilled(true)
        dataSet.fillColor = modernBlue
        dataSet.fillAlpha = 50

        val lineData = LineData(dataSet)
        weightChart.data = lineData

        // Defaults to a 1-Month view on load
        setTimeView(30f)
    }

    /**
     * Zooms the chart to a specific number of days and pans to the most recent entry.
     */
    private fun setTimeView(days: Float) {
        if (weightChart.data != null && weightChart.data.entryCount > 0) {
            weightChart.fitScreen() // Resets any previous zoom
            weightChart.setVisibleXRangeMaximum(days) // Sets the maximum visible window

            // Finds the very last entry (the most recent date)
            val highestXValue = weightChart.data.dataSets[0].xMax
            // Moves the camera to look at the most recent data point
            weightChart.moveViewToX(highestXValue)
        }
    }

    /**
     * A custom translator that converts raw mathematical "Days" back into "dd MMM yy"
     */
    class DateAxisFormatter : ValueFormatter() {
        private val outputFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        override fun getFormattedValue(value: Float): String {
            // Converts "Days" back into standard Milliseconds for the SimpleDateFormat
            val millis = value.toLong() * 1000L * 60L * 60L * 24L
            return outputFormat.format(Date(millis))
        }
    }
}