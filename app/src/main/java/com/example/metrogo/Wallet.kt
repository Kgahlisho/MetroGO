package com.example.metrogo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat

class Wallet : AppCompatActivity() {

    private val currencyFormat = DecimalFormat("#,##0.00")
    private fun rand(amount: Number): String = "R ${currencyFormat.format(amount)}"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_wallet)
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

        refreshBalance()
        bindQuickAmounts()
        bindTopUpButton()
    }

    override fun onResume() {
        super.onResume()
        // Covers the case where the balance changed elsewhere (e.g. a ticket
        // purchase) while this screen was in the background.
        refreshBalance()
    }

    private fun refreshBalance() {
        val balance = TicketManager.getBalance(this)
        findViewById<TextView>(R.id.tvBalance).text = rand(balance)
    }

    private fun bindQuickAmounts() {
        val etAmount = findViewById<EditText>(R.id.etAmount)

        val quickAmounts = listOf(
            R.id.btnQuick50 to 50,
            R.id.btnQuick100 to 100,
            R.id.btnQuick200 to 200,
            R.id.btnQuick500 to 500
        )

        quickAmounts.forEach { (viewId, amount) ->
            findViewById<TextView>(viewId).setOnClickListener {
                etAmount.setText(amount.toString())
                etAmount.setSelection(etAmount.text.length)
            }
        }
    }

    private fun bindTopUpButton() {
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val rowSuccess = findViewById<LinearLayout>(R.id.rowTopUpSuccess)
        val tvSuccess = findViewById<TextView>(R.id.tvTopUpSuccess)

        findViewById<MaterialButton>(R.id.btnTopUp).setOnClickListener {
            rowSuccess.visibility = View.GONE

            val amount = etAmount.text.toString().trim().toIntOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Enter a valid amount to top up.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            TicketManager.topUp(this, amount)
            refreshBalance()

            etAmount.text.clear()
            tvSuccess.text = "Wallet topped up successfully with ${rand(amount)}"
            rowSuccess.visibility = View.VISIBLE
        }
    }
}