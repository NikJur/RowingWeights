package com.example.rowingweights

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt

/**
 * PlotFragment visualises historical data using proportional time scaling.
 */
class PlotFragment : Fragment(R.layout.fragment_plot) {

    private lateinit var weightChart: LineChart
    private lateinit var dbHelper: DatabaseHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        weightChart = view.findViewById(R.id.weightChart)
        dbHelper = DatabaseHelper(requireContext())

        setupChartAppearance()

        // Wires up the zoom buttons
        view.findViewById<Button>(R.id.btnWeek).setOnClickListener { setTimeView(7f) }
        view.findViewById<Button>(R.id.btnMonth).setOnClickListener { setTimeView(30f) }
        view.findViewById<Button>(R.id.btnYear).setOnClickListener { setTimeView(365f) }
        view.findViewById<Button>(R.id.btnAll).setOnClickListener {
            weightChart.fitScreen() // Zooms all the way out
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
        weightChart.isScaleYEnabled = false // Locks vertical zoom, only allows horizontal panning

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
    }

    private fun refreshChartFromDatabase() {
        val historicalData = dbHelper.getAllWeights()

        if (historicalData.isEmpty()) {
            weightChart.clear()
            return
        }

        val weightEntries = mutableListOf<Entry>()
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
        dataSet.mode = LineDataSet.Mode.LINEAR

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
        private val outputFormat = SimpleDateFormat("dd MMM yy", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            // Converts "Days" back into standard Milliseconds for the SimpleDateFormat
            val millis = value.toLong() * 1000L * 60L * 60L * 24L
            return outputFormat.format(Date(millis))
        }
    }
}