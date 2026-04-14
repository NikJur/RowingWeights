package com.example.rowingweights

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * MainActivity acts as the host container.
 * It monitors the bottom navigation bar and swaps the active Fragment.
 */
class MainActivity : AppCompatActivity() {

    /**
     * Connects the navigation logic and loads the default screen.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Loads the Log tab automatically on the initial launch
        if (savedInstanceState == null) {
            replaceFragment(LogFragment())
        }

        // Listens for tab selections and routes to the correct Fragment
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_log -> replaceFragment(LogFragment())
                R.id.nav_history -> replaceFragment(PlotFragment())
            }
            true
        }
    }

    /**
     * Executes the visual transition between different layout files.
     */
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}