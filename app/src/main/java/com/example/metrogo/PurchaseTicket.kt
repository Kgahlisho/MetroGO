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
        val availableBuses = BusRouteRepository.sampleRoutes



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