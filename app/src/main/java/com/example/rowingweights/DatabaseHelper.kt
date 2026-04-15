package com.example.rowingweights

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * DatabaseHelper manages the creation and version management of the local SQLite database.
 * It provides methods to insert new weight entries and retrieve historical data securely.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "WeightTracker.db", null, 1) {

    /**
     * Creates the database table when the application first initialises it.
     */
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE weights (date TEXT PRIMARY KEY, weight REAL)"
        db.execSQL(createTable)
    }

    /**
     * Upgrades the database structure if the version number increases in the future.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS weights")
        onCreate(db)
    }

    /**
     * Inserts a new weight record or overwrites an existing one for a specific date.
     */
    fun insertWeight(date: String, weight: Float) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put("date", date)
        values.put("weight", weight)

        // Uses replace to ensure only one weight entry exists per date
        db.replace("weights", null, values)
        db.close()
    }

    /**
     * Deletes a specific weight record based on its date.
     */
    fun deleteWeight(date: String) {
        val db = this.writableDatabase
        // The "?" acts as a secure placeholder to prevent SQL injection errors
        db.delete("weights", "date = ?", arrayOf(date))
        db.close()
    }

    /**
     * Retrieves all stored weight records and sorts them chronologically.
     */
    fun getAllWeights(): List<Pair<String, Float>> {
        val weightList = mutableListOf<Pair<String, Float>>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT date, weight FROM weights ORDER BY date ASC", null)

        // Iterates through the database results and populates the list
        if (cursor.moveToFirst()) {
            do {
                val date = cursor.getString(0)
                val weight = cursor.getFloat(1)
                weightList.add(Pair(date, weight))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return weightList
    }
}