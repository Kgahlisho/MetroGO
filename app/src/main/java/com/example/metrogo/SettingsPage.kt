package com.example.metrogo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings_page)
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

        val btnback = findViewById<ImageButton>(R.id.btnBack)
        btnback.setOnClickListener {
            finish()
        }

        val rowProfileManagement = findViewById<LinearLayout>(R.id.rowProfileManagement)
        rowProfileManagement.setOnClickListener {
         val intent = Intent(this, ProfileManagement::class.java)
       startActivity(intent)
        }

        val rowNotifications = findViewById<LinearLayout>(R.id.rowNotifications)
        rowNotifications.setOnClickListener {
            intent = Intent(this, NotificationPage::class.java)
            startActivity(intent)

        }


        val rowPreferences = findViewById<LinearLayout>(R.id.rowPreferences)
        rowPreferences.setOnClickListener {
            startActivity(Intent(this, PreferencesPage::class.java))
        }

        val rowHelpFaqs = findViewById<LinearLayout>(R.id.rowHelpFaqs)
        rowHelpFaqs.setOnClickListener {
            intent = Intent(this, HelpPage::class.java)
            startActivity(intent)
        }

        // Log Out button: confirm, then clear the back stack and return to LoginPage
        val btnLogOut = findViewById<Button>(R.id.btnLogOut)
        btnLogOut.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of MetroGO?")
                .setPositiveButton("Log Out") { _, _ ->
                    val loginIntent = Intent(this, LoginPage::class.java)
                    loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(loginIntent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}