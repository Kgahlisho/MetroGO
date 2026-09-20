package com.example.metrogo

import android.app.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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
    private var allTrips: List<Trip> = emptyList()
    private var currentFilter = "All Trips"
    private lateinit var tvEmpty: TextView
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
        tvEmpty = findViewById(R.id.tvNoTrips)


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

        val rvTrips = findViewById<RecyclerView>(R.id.rvTrips)
        rvTrips.layoutManager = LinearLayoutManager(this)
        adapter = TripAdapter(emptyList()) { trip -> showTripDetailsDialog(trip) }
        rvTrips.adapter = adapter

        setupFilter()
        loadTrips()
    }

    override fun onResume() {
        super.onResume()
        // Re-read the store so a ticket bought a moment ago shows up immediately.
        if (::adapter.isInitialized) loadTrips()
    }

    /** Loads the real tickets the user has bought, then refreshes XP + the list. */
    private fun loadTrips() {
        allTrips = TicketStore.getTripHistory(this)
        updateXpBar()
        filterTrips(currentFilter)
    }

    private fun updateXpBar() {
        val progress = TicketStore.getXpProgress(this)
        val maxXp = TicketStore.XP_PER_LEVEL

        pbXp.max = maxXp
        pbXp.progress = progress.xpInLevel
        tvXpValue.text = "${progress.xpInLevel}/$maxXp"
        tvLevel.text = "Level ${progress.level}"
    }

    private fun showTripDetailsDialog(trip: Trip) {
        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.dialog_trip_details)

        dialog.findViewById<ImageView>(R.id.btnCloseTripDialog).setOnClickListener { dialog.dismiss() }

        val t = trip.ticket
        // Route/stop/time details and the passenger's name live on other tables now.
        val details = TransportRouteRepository.scheduleDetails(t.scheduleId)
        val passenger = UserManager.getCurrentUser(this)?.fullName ?: "Guest"
        val rows = listOf(
            "Ticket ID" to t.ticketId,
            "Passenger" to passenger,
            "Transport" to (details?.route?.routeName ?: "--"),
            "Bus registration" to (details?.schedule?.busRegistration ?: "--"),
            "From" to (details?.originStop?.stopName ?: "--"),
            "To" to (details?.destinationStop?.stopName ?: "--"),
            "Departs" to (details?.schedule?.departureTime ?: "--"),
            "Arrives" to (details?.schedule?.arrivalTime ?: "--"),
            "Purchased" to formatDateTime(t.purchaseDate),
            "Fare" to trip.cost,
            "Paid via" to trip.paymentMethod,
            "Status" to if (trip.isPaid) "Paid" else "Not paid",
            "XP earned" to "${trip.xpEarned} XP"
        )

        val container = dialog.findViewById<LinearLayout>(R.id.layoutTripDetailRows)
        val inflater = LayoutInflater.from(this)
        for ((label, value) in rows) {
            val row = inflater.inflate(R.layout.item_trip_detail_row, container, false)
            row.findViewById<TextView>(R.id.tvDetailLabel).text = label
            row.findViewById<TextView>(R.id.tvDetailValue).text = value
            container.addView(row)
        }

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        LanguageManager.translateDialog(dialog)
        dialog.show()
    }

    private fun formatDateTime(timestamp: Long): String =
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))

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
                filterTrips(filterOptions[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun filterTrips(filter: String) {
        currentFilter = filter
        val filteredList = when (filter) {
            "Paid" -> allTrips.filter { it.isPaid }
            "Not Paid" -> allTrips.filter { !it.isPaid }
            "Missed Boarding" -> emptyList() // not tracked for real tickets yet
            else -> allTrips // "All Trips"
        }
        adapter.updateList(filteredList)
        tvEmpty.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }
}