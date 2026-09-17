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
            BusRoute(1, "T9 Vea Raya", "Park Station", "Soshanguve", "08:15", "08:57", 32, "GP 814-263", 42),
            BusRoute(2, "Vea Raya", "Rosebank", "Midrand", "09:42", "10:24", 27, "GP 392-715", 42),
            BusRoute(3, "T2 Vea Raya", "Soweto", "Parktown", "10:18", "10:51", 24, "GP 641-908", 33),
            BusRoute(4, "T7 Vea Raya", "Centurion", "Sandton", "11:35", "12:48", 29, "GP 527-384", 73),
            BusRoute(5, "T3 Vea Raya", "Sandton", "Rosebank", "12:10", "12:37", 22, "GP 176-529", 27),
            BusRoute(6, "Vea Raya", "Rosebank", "Parktown", "13:27", "14:08", 26, "GP 903-417", 41),
            BusRoute(7, "T9 Vea Raya", "Park Station", "Soshanguve", "14:52", "15:36", 31, "GP 458-672", 44),
            BusRoute(8, "T2 Vea Raya", "Soweto", "Parktown", "15:41", "16:16", 23, "GP 735-194", 35),
            BusRoute(9, "T7 Vea Raya", "Centurion", "Sandton", "16:25", "17:39", 28, "GP 286-843", 74),
            BusRoute(10, "T3 Vea Raya", "Sandton", "Rosebank", "17:08", "17:34", 25, "GP 619-357", 26),
            BusRoute(11, "Vea Raya", "Rosebank", "Midrand", "18:14", "18:58", 30, "GP 842-516", 44),
            BusRoute(12, "T2 Vea Raya", "Soweto", "Parktown", "19:36", "20:09", 21, "GP 374-825", 33)

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