package com.example.metrogo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Wallet : AppCompatActivity() {

    private val currencyFormat = DecimalFormat("#,##0.00")
    private fun rand(amount: Number): String = "R ${currencyFormat.format(amount)}"

    private val dateFormat = SimpleDateFormat("dd/MM/yy\nHH:mm", Locale.getDefault())

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* nothing to do either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_wallet)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Android 13+ needs the user's permission before we can show system notifications.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
        loadTransactions()
        bindQuickAmounts()
        bindTopUpButton()
    }

    override fun onResume() {
        super.onResume()
        // Covers the case where the balance changed elsewhere (e.g. a ticket
        // purchase) while this screen was in the background.
        refreshBalance()
        loadTransactions()
    }

    private fun refreshBalance() {
        val balance = TransportCardManager.getBalance(this)
        findViewById<TextView>(R.id.tvBalance).text = rand(balance)
    }

    /** Fills the transaction history table with the most recent wallet activity. */
    private fun loadTransactions() {
        val container = findViewById<LinearLayout>(R.id.layoutTransactionRows)
        val tvEmpty = findViewById<TextView>(R.id.tvNoTransactions)
        container.removeAllViews()

        val transactions = PaymentStore.getAll(this).take(MAX_ROWS_SHOWN)
        tvEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE

        val inflater = LayoutInflater.from(this)
        for (t in transactions) {
            val row = inflater.inflate(R.layout.item_transaction_row, container, false)
            val isTopUp = t.type == Payment.TYPE_TOPUP

            row.findViewById<TextView>(R.id.tvTxDate).text = dateFormat.format(Date(t.paymentDate))
            row.findViewById<TextView>(R.id.tvTxDescription).text = t.description

            val tvAmount = row.findViewById<TextView>(R.id.tvTxAmount)
            tvAmount.text = (if (isTopUp) "+R" else "-R") + currencyFormat.format(t.amount)
            tvAmount.setTextColor(
                if (isTopUp) android.graphics.Color.parseColor("#4CAF50")
                else android.graphics.Color.parseColor("#F44336")
            )

            row.findViewById<TextView>(R.id.tvTxBalance).text = "R" + currencyFormat.format(t.balanceAfter)
            container.addView(row)
        }
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

            TransportCardManager.topUp(this, amount)
            refreshBalance()
            loadTransactions()
            NotificationHelper.notifyWalletTopUp(this, amount, TransportCardManager.getBalance(this))

            etAmount.text.clear()
            tvSuccess.text = "Wallet topped up successfully with ${rand(amount)}"
            rowSuccess.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val MAX_ROWS_SHOWN = 20
    }
}