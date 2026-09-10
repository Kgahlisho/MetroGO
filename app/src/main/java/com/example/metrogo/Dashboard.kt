package com.example.metrogo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.LinearLayout

class Dashboard : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
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

        val pillPurchaseTicket = findViewById<LinearLayout>(R.id.pillPurchaseTicket)
        pillPurchaseTicket.setOnClickListener{
            val intent = Intent(this, PurchaseTicket::class.java)
            startActivity(intent)
        }

        val pillWallet = findViewById<LinearLayout>(R.id.pillWallet)
        pillWallet.setOnClickListener {
            val intent = Intent(this , Wallet::class.java)
            startActivity(intent)
        }


        val pillTravelHistory = findViewById<LinearLayout>(R.id.pillTravelHistory)
        pillTravelHistory.setOnClickListener {
            val intent = Intent(this , TravelHistory::class.java)
            startActivity(intent)
        }


        val pillJourneyPlanner = findViewById<LinearLayout>(R.id.pillJourneyPlanner)
        pillJourneyPlanner.setOnClickListener {
            val intent = Intent(this , JourneyPlanner::class.java)
            startActivity(intent)
        }
    }
}