package com.example.metrogo

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PurchaseTicket : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_purchase_ticket)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val circleSettings = findViewById<FrameLayout>(R.id.circleSettings)
        circleSettings.setOnClickListener {
            startActivity(Intent(this, SettingsPage::class.java))
        }

        val circleProfile = findViewById<FrameLayout>(R.id.circleProfile)
        circleProfile.setOnClickListener {
            startActivity(Intent(this, ProfileManagement::class.java))
        }

        val circleNotification = findViewById<FrameLayout>(R.id.circleNotification)
        circleNotification.setOnClickListener {
            startActivity(Intent(this, NotificationPage::class.java))
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        setupBusList()
    }

    private fun setupBusList() {
        // TODO: replace with real bus/route data (API or local schedule DB) once available.
        val availableBuses = listOf(
            BusRoute(1, "T4 Vea Raya", "Rosebank", "Parktown", "14:33", "15:10", 21, "GP 123-456"),
            BusRoute(2, "T3 Vea Raya", "Sandton", "Rosebank", "14:58", "15:20", 25, "GP 234-567"),
            BusRoute(3, "T7 Vea Raya", "Parktown", "Sandton", "15:13", "15:50", 25, "GP 345-678"),
            BusRoute(4, "T5 Vea Raya", "Rosebank", "Midrand", "16:33", "17:15", 30, "GP 456-789"),
            BusRoute(5, "T2 Vea Raya", "Sandton", "Parktown", "16:58", "17:30", 21, "GP 567-890"),
            BusRoute(6, "T9 Vea Raya", "Park Station", "Midrand", "16:58", "17:40", 28, "GP 678-901")
        )

        val rvBuses = findViewById<RecyclerView>(R.id.rvBuses)
        rvBuses.layoutManager = LinearLayoutManager(this)
        rvBuses.adapter = Busrouteadapter(availableBuses) { selectedRoute ->
            val intent = Intent(this, PaymentActivity::class.java)
            intent.putExtra(PaymentActivity.EXTRA_BUS_ROUTE, selectedRoute)
            startActivity(intent)
        }

        findViewById<android.widget.TextView>(R.id.tvNoBuses).visibility =
            if (availableBuses.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
}