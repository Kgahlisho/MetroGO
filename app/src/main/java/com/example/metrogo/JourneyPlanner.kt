package com.example.metrogo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class JourneyPlanner : AppCompatActivity() {

    private lateinit var tvFromStation: TextView
    private lateinit var tvToStation: TextView
    private lateinit var inputFrom: LinearLayout
    private lateinit var inputTo: LinearLayout
    private lateinit var btnSwap: ImageButton
    private lateinit var btnFindRoutes: Button
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_journey_planner)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val circleSettings = findViewById<FrameLayout>(R.id.circleSettings)
        circleSettings.setOnClickListener {
            intent = Intent(this, SettingsPage::class.java)
            startActivity(intent)
        }

        val circleProfile = findViewById<FrameLayout>(R.id.circleProfile)
        circleProfile.setOnClickListener {
            val intent = Intent(this, ProfileManagement::class.java)
            startActivity(intent)
        }

        val circleNotification = findViewById<FrameLayout>(R.id.circleNotification)
        circleNotification.setOnClickListener{
            val intent = Intent(this , NotificationPage::class.java)
            startActivity(intent)
        }

        // Initialize views
        initViews()

        // Set up click listeners
        setupClickListeners()
    }

    private fun initViews() {
        tvFromStation = findViewById(R.id.tvFromStation)
        tvToStation = findViewById(R.id.tvToStation)
        inputFrom = findViewById(R.id.inputFrom)
        inputTo = findViewById(R.id.inputTo)
        btnSwap = findViewById(R.id.btnSwap)
        btnFindRoutes = findViewById(R.id.btnFindRoutes)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupClickListeners() {
        // From location click - opens station selection
        inputFrom.setOnClickListener {
            showStationSelectionDialog("From")
        }

        // To location click - opens station selection
        inputTo.setOnClickListener {
            showStationSelectionDialog("To")
        }

        // Swap locations
        btnSwap.setOnClickListener {
            swapLocations()
        }

        // Find routes
        btnFindRoutes.setOnClickListener {
            findRoutes()
        }

        // Back button
        val btnback = findViewById<ImageButton>(R.id.btnBack)
        btnback.setOnClickListener {
            finish()
        }
    }

    private fun showStationSelectionDialog(type: String) {
        // Sample stations - before implementing an  API or database
        val stations = arrayOf(
            "Park Station",
            "Sandton Station",
            "Rosebank Station",
            "Midrand Station",
            "Centurion Station",
            "Pretoria Station",
            "Johannesburg Station",
            "OR Tambo Station"
        )

        //  selection dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select $type Station")
            .setItems(stations) { _, which ->
                val selectedStation = stations[which]
                if (type == "From") {
                    tvFromStation.text = selectedStation
                } else {
                    tvToStation.text = selectedStation
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun swapLocations() {
        val fromText = tvFromStation.text.toString()
        val toText = tvToStation.text.toString()

        // Swap the text
        tvFromStation.text = toText
        tvToStation.text = fromText

        // Show toast message
        Toast.makeText(this, "Locations swapped", Toast.LENGTH_SHORT).show()
    }

    private fun findRoutes() {
        val from = tvFromStation.text.toString()
        val to = tvToStation.text.toString()

        // Validate that locations are different
        if (from == to) {
            Toast.makeText(this, "Please select different locations", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress or search for routes
        Toast.makeText(this, "Searching routes from $from to $to...", Toast.LENGTH_SHORT).show()

        // In a real app, you would:
        // 1. Call API to get available routes
        // 2. Display results in a list or on map
        // 3. Show bus schedule
    }
}