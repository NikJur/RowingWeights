package com.example.rowingweights

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MainActivity handles the user interface and connects inputs to the SQLite database.
 * It retrieves historical data on startup and plots it dynamically.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var weightInput: EditText
    private lateinit var submitButton: Button
    private lateinit var weightChart: LineChart

    // The lateinit keyword promises the compiler this variable will be initialized
    // before it is ever used, preventing null-reference crashes.
    private lateinit var dbHelper: DatabaseHelper

    /**
     * Initializes the interface, connects to the database, and loads existing data.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        weightInput = findViewById(R.id.weightInput)
        submitButton = findViewById(R.id.submitButton)
        weightChart = findViewById(R.id.weightChart)

        // Instantiates the database helper, passing "this" as the Context
        dbHelper = DatabaseHelper(this)

        weightChart.description.isEnabled = false
        weightChart.legend.isEnabled = false

        // Loads any previously saved data immediately when the app opens
        refreshChartFromDatabase()

        submitButton.setOnClickListener {
            processAndSaveWeight()
        }
    }

    /**
     * Extracts the input, generates a timestamp, saves to the database, and updates the UI.
     */
    private fun processAndSaveWeight() {
        val inputString = weightInput.text.toString()

        if (inputString.isNotEmpty()) {
            val weightValue = inputString.toFloat()

            // Generates today's date in a standard YYYY-MM-DD format
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayDate = dateFormat.format(Date())

            // Sends the date and weight to be stored permanently on the device
            dbHelper.insertWeight(todayDate, weightValue)

            weightInput.text.clear()

            // Triggers a full reload of the chart to include the new data point
            refreshChartFromDatabase()
        }
    }

    /**
     * Queries the database for all historical entries and rebuilds the visual plot.
     */
    private fun refreshChartFromDatabase() {
        // Retrieves the list of date/weight pairs from the database
        val historicalData = dbHelper.getAllWeights()

        val weightEntries = mutableListOf<Entry>()

        // Iterates through the stored data to format it for the charting library.
        // The index ('i') serves as the X-axis chronological coordinate.
        historicalData.forEachIndexed { i, record ->
            val weight = record.second // The float value from our Pair structure
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