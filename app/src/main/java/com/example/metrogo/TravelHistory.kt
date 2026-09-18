package com.example.metrogo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TravelHistory : AppCompatActivity() {


    private lateinit var adapter: TripAdapter
    private lateinit var allTrips: List<Trip>
    private lateinit var pbXp: ProgressBar
    private lateinit var tvXpValue: TextView
    private lateinit var tvLevel: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_travel_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        pbXp = findViewById(R.id.pbXp)
        tvXpValue = findViewById(R.id.tvXpValue)
        tvLevel = findViewById(R.id.tvLevel)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<FrameLayout>(R.id.circleSettings).setOnClickListener {
            startActivity(Intent(this, SettingsPage::class.java))
        }
        findViewById<FrameLayout>(R.id.circleNotification).setOnClickListener {
            startActivity(Intent(this, NotificationPage::class.java))
        }
        findViewById<FrameLayout>(R.id.circleProfile).setOnClickListener {
            startActivity(Intent(this, ProfileManagement::class.java))
        }

        generateDummyData()

        // 2. Calculate and Update XP Bar
        calculateXp()

        // 3. Setup RecyclerView
        val rvTrips = findViewById<RecyclerView>(R.id.rvTrips)
        rvTrips.layoutManager = LinearLayoutManager(this)
        adapter = TripAdapter(allTrips)
        rvTrips.adapter = adapter

        // 4. Setup Filter Spinner
        setupFilter()
    }

    private fun generateDummyData() {
        allTrips = listOf(
            Trip(1, "Rosebank to Parktown", "06:20", "10/08/26", "R21", "Phone", 10, true),
            Trip(
                2,
                "Rosebank to Parktown",
                "Missed",
                "10/08/26",
                "R21",
                "Phone",
                10,
                false
            ), // Not paid
            Trip(3, "Sandton to Rosebank", "07:15", "11/08/26", "R25", "Card", 15, true),
            Trip(4, "Parktown to Sandton", "17:30", "11/08/26", "R25", "Card", 15, true),
            Trip(
                5,
                "Rosebank to Parktown",
                "Missed",
                "12/08/26",
                "R21",
                "Phone",
                0,
                false
            ), // Not paid
            Trip(6, "Sandton to Parktown", "08:00", "13/08/26", "R30", "Phone", 20, true)
        )
    }

    private fun calculateXp() {
        // Sum up all XP earned from all trips (or just paid trips, depending on logic)
        val totalXp = allTrips.sumOf { it.xpEarned }
        val maxXp = 1000

        // Update Progress Bar
        pbXp.max = maxXp
        pbXp.progress = totalXp

        // Update Text
        tvXpValue.text = "$totalXp/$maxXp"

        // Calculate Level (Simple logic: every 100 XP = 1 Level)
        val level = (totalXp / 100) + 1
        tvLevel.text = "Level $level"
    }


    private fun setupFilter() {
        val spinnerFilter = findViewById<Spinner>(R.id.spinnerFilter)

        // Define filter options
        val filterOptions = listOf("All Trips", "Paid", "Not Paid", "Missed Boarding")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedFilter = filterOptions[position]
                filterTrips(selectedFilter)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterTrips(filter: String) {
        val filteredList = when (filter) {
            "Paid" -> allTrips.filter { it.isPaid }
            "Not Paid" -> allTrips.filter { !it.isPaid }
            "Missed Boarding" -> allTrips.filter { it.boardingTime == "Missed" }
            else -> allTrips // "All Trips"
        }
        adapter.updateList(filteredList)
    }
}