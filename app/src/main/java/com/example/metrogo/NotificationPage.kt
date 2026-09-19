package com.example.metrogo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationPage : AppCompatActivity() {

    private lateinit var adapter: NotificationAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val circleProfile = findViewById<FrameLayout>(R.id.circleProfile)
        circleProfile.setOnClickListener {
            startActivity(Intent(this, ProfileManagement::class.java))
        }

        val circleSettings = findViewById<FrameLayout>(R.id.circleSettings)
        circleSettings.setOnClickListener {
            startActivity(Intent(this, SettingsPage::class.java))
        }

        tvEmpty = findViewById(R.id.tvNoNotifications)
        val rv = findViewById<RecyclerView>(R.id.rvNotifications)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(emptyList())
        rv.adapter = adapter

        loadNotifications()
    }

    override fun onResume() {
        super.onResume()
        // Refresh in case a notification arrived while this page was in the background.
        if (::adapter.isInitialized) loadNotifications()
    }

    private fun loadNotifications() {
        val items = NotificationStore.getAll(this)
        adapter.updateList(items)
        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}