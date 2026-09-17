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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class JourneyPlanner : AppCompatActivity() {

    private lateinit var tvFromStation: TextView
    private lateinit var tvToStation: TextView
    private lateinit var inputFrom: LinearLayout
    private lateinit var inputTo: LinearLayout
    private lateinit var btnSwap: ImageButton
    private lateinit var btnFindRoutes: Button
    private lateinit var btnBack: ImageButton

    private lateinit var tvMatchingSubtitle: TextView
    private lateinit var tvNoMatches: TextView
    private lateinit var rvMatchingBuses: RecyclerView
    private lateinit var rvOtherBuses: RecyclerView

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

        initViews()
        setupClickListeners()
        setupBusLists()

        // Show results for the default From/To straight away, rather than waiting
        // for the user to tap "Find Routes" first.
        findRoutes()
    }

    private fun initViews() {
        tvFromStation = findViewById(R.id.tvFromStation)
        tvToStation = findViewById(R.id.tvToStation)
        inputFrom = findViewById(R.id.inputFrom)
        inputTo = findViewById(R.id.inputTo)
        btnSwap = findViewById(R.id.btnSwap)
        btnFindRoutes = findViewById(R.id.btnFindRoutes)
        btnBack = findViewById(R.id.btnBack)

        tvMatchingSubtitle = findViewById(R.id.tvMatchingSubtitle)
        tvNoMatches = findViewById(R.id.tvNoMatches)
        rvMatchingBuses = findViewById(R.id.rvMatchingBuses)
        rvOtherBuses = findViewById(R.id.rvOtherBuses)
    }

    private fun setupClickListeners() {
        inputFrom.setOnClickListener {
            showStationSelectionDialog("From")
        }

        inputTo.setOnClickListener {
            showStationSelectionDialog("To")
        }

        btnSwap.setOnClickListener {
            swapLocations()
        }

        btnFindRoutes.setOnClickListener {
            findRoutes()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupBusLists() {
        rvMatchingBuses.layoutManager = LinearLayoutManager(this)
        rvOtherBuses.layoutManager = LinearLayoutManager(this)
        // Both lists live inside the screen's outer NestedScrollView, so let it own scrolling.
        rvMatchingBuses.isNestedScrollingEnabled = false
        rvOtherBuses.isNestedScrollingEnabled = false
    }

    private fun showStationSelectionDialog(type: String) {
        // Pulled from the same repository PurchaseTicket uses, so every station shown here
        // is guaranteed to actually match a real route's origin/destination.
        val stations = BusRouteRepository.stationNames.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select $type Station")
            .setItems(stations) { _, which ->
                val selectedStation = stations[which]
                if (type == "From") {
                    tvFromStation.text = selectedStation
                } else {
                    tvToStation.text = selectedStation
                }
                // Re-run the search immediately so results always reflect what's on screen.
                findRoutes()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun swapLocations() {
        val fromText = tvFromStation.text.toString()
        val toText = tvToStation.text.toString()

        tvFromStation.text = toText
        tvToStation.text = fromText

        Toast.makeText(this, "Locations swapped", Toast.LENGTH_SHORT).show()
        findRoutes()
    }

    private fun findRoutes() {
        val from = tvFromStation.text.toString()
        val to = tvToStation.text.toString()

        if (from == to) {
            Toast.makeText(this, "Please select different locations", Toast.LENGTH_SHORT).show()
            return
        }

        val matchingRoutes = BusRouteRepository.routesBetween(from, to)
        val otherRoutes = BusRouteRepository.routesExcluding(from, to)

        tvMatchingSubtitle.text = "$from \u2192 $to"
        tvNoMatches.visibility = if (matchingRoutes.isEmpty()) View.VISIBLE else View.GONE
        rvMatchingBuses.visibility = if (matchingRoutes.isEmpty()) View.GONE else View.VISIBLE

        rvMatchingBuses.adapter = Busrouteadapter(matchingRoutes) { selectedRoute ->
            openPayment(selectedRoute)
        }
        rvOtherBuses.adapter = Busrouteadapter(otherRoutes) { selectedRoute ->
            openPayment(selectedRoute)
        }
    }

    private fun openPayment(route: BusRoute) {
        val intent = Intent(this, PaymentActivity::class.java)
        intent.putExtra(PaymentActivity.EXTRA_BUS_ROUTE, route)
        startActivity(intent)
    }
}