package com.example.metrogo

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

class ProfileManagement : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_management)
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

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etIdNumber = findViewById<EditText>(R.id.etIdNumber)
        val etDob = findViewById<EditText>(R.id.etDob)
        val etMobile = findViewById<EditText>(R.id.etMobile)
        val etEmail = findViewById<EditText>(R.id.etEmail)

        // Snapshot of the values loaded from the profile, used to restore on Cancel
        val originalFullName = etFullName.text.toString()
        val originalIdNumber = etIdNumber.text.toString()
        val originalDob = etDob.text.toString()
        val originalMobile = etMobile.text.toString()
        val originalEmail = etEmail.text.toString()

        // Date of Birth opens a date picker instead of the keyboard
        etDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    etDob.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val btnSaveChanges = findViewById<Button>(R.id.btnSaveChanges)
        btnSaveChanges.setOnClickListener {
            if (etFullName.text.isBlank() || etMobile.text.isBlank() || etEmail.text.isBlank()) {
                Toast.makeText(this, "Full name, mobile number and email are required", Toast.LENGTH_SHORT).show()
            } else {
                // TODO: persist the updated profile details to the backend
                Toast.makeText(this, "Profile changes saved", Toast.LENGTH_SHORT).show()
            }
        }

        val btnCancelChanges = findViewById<Button>(R.id.btnCancelChanges)
        btnCancelChanges.setOnClickListener {
            etFullName.setText(originalFullName)
            etIdNumber.setText(originalIdNumber)
            etDob.setText(originalDob)
            etMobile.setText(originalMobile)
            etEmail.setText(originalEmail)
            Toast.makeText(this, "Changes discarded", Toast.LENGTH_SHORT).show()
        }

        // Delete Account: destructive action, always confirm first
        val btnDeleteAccount = findViewById<Button>(R.id.btnDeleteAccount)
        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This will permanently delete your MetroGO account. This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    // TODO: call the account-deletion endpoint before navigating away
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