package com.example.rowingweights

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

/**
 * PlotFragment visualises historical data.
 * It queries the database every time it becomes visible.
 */
class PlotFragment : Fragment(R.layout.fragment_plot) {

    private lateinit var weightChart: LineChart
    private lateinit var dbHelper: DatabaseHelper

    /**
     * Initialises the chart parameters when the fragment builds.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        weightChart = view.findViewById(R.id.weightChart)
        dbHelper = DatabaseHelper(requireContext())

        weightChart.description.isEnabled = false
        weightChart.legend.isEnabled = false
    }

    /**
     * Triggers a database query whenever the user switches to this tab.
     */
    override fun onResume() {
        super.onResume()
        refreshChartFromDatabase()
    }

    /**
     * Retrieves stored data and constructs the LineDataSet.
     */
    private fun refreshChartFromDatabase() {
        val historicalData = dbHelper.getAllWeights()

        if (historicalData.isEmpty()) {
            weightChart.clear()
            return
        }

        val weightEntries = mutableListOf<Entry>()

        historicalData.forEachIndexed { i, record ->
            val weight = record.second
            weightEntries.add(Entry(i.toFloat(), weight))
        }

        val dataSet = LineDataSet(weightEntries, "Daily Weight")
        dataSet.color = Color.BLUE
        dataSet.valueTextColor = Color.BLACK
        dataSet.setCircleColor(Color.DKGRAY)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f

        val lineData = LineData(dataSet)
        weightChart.data = lineData
        weightChart.invalidate()
    }
}