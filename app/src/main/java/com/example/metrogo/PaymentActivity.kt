package com.example.metrogo


import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
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
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PaymentActivity : AppCompatActivity() {


    private lateinit var details: TransportRouteRepository.ScheduleDetails
    private var fareHoldTimer : CountDownTimer? = null

    private val currencyFormat = DecimalFormat("#,##0.00")
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* nothing to do either way */ }
    private fun rand(amount: Number): String = "R${currencyFormat.format(amount)}"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val resolved = intent.getStringExtra(EXTRA_SCHEDULE_ID)
            ?.let { TransportRouteRepository.scheduleDetails(it) }
        if (resolved == null) {
            Toast.makeText(this, "No bus selected.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        details = resolved

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }



        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            //Toast.makeText(this, "Back clicked", Toast.LENGTH_SHORT).show()
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            //Toast.makeText(this, "Back clicked", Toast.LENGTH_SHORT).show()
            onBackPressedDispatcher.onBackPressed()
        }

        bindTripSummary()
        bindWalletBalance()
        startFareHoldTimer()

        findViewById<MaterialButton>(R.id.btnPayNow).setOnClickListener { attemptPayment()
        }

        findViewById<TextView>(R.id.btnTopUp).setOnClickListener {
            startActivity(Intent(this, Wallet::class.java))
        }
    }


    override fun onResume() {
        super.onResume()
        bindWalletBalance()
    }

    private fun bindTripSummary() {
        val schedule = details.schedule
        findViewById<TextView>(R.id.tvTransportName).text = details.route.routeName
        findViewById<TextView>(R.id.tvPrice).text = rand(schedule.price)

        findViewById<TextView>(R.id.tvOrigin).text = details.originStop.stopName
        findViewById<TextView>(R.id.tvDestination).text = details.destinationStop.stopName
        findViewById<TextView>(R.id.tvOriginTime).text = "Departs ${schedule.departureTime}"
        findViewById<TextView>(R.id.tvDestinationTime).text = "Arrives ${schedule.arrivalTime}"
        findViewById<TextView>(R.id.tvDuration).text = "${schedule.durationMinutes} min"
        findViewById<TextView>(R.id.tvRegistration).text = schedule.busRegistration

        // Service fee is a flat R0.00 for now (static in the layout) -- fare is the only
        // line item, so both totals just mirror the fare price.
        findViewById<TextView>(R.id.tvTotalAmount).text = rand(schedule.price)
        findViewById<TextView>(R.id.tvTotalInline).text = rand(schedule.price)
    }


    private fun bindWalletBalance() {
        val balance = TransportCardManager.getBalance(this)
        findViewById<TextView>(R.id.tvWalletBalance).text = rand(balance)

        val afterBalance = (balance - details.schedule.price).coerceAtLeast(0)
        findViewById<TextView>(R.id.tvWalletAfter).text = "Balance after payment: ${rand(afterBalance)}"
    }


    private fun startFareHoldTimer() {
        val fareHoldMillis = 5 * 60 * 1000L
        val tvFareHold = findViewById<TextView>(R.id.tvFareHold)

        fareHoldTimer = object : CountDownTimer(fareHoldMillis, 1000) {
            override fun onTick(millisRemaining: Long) {
                val minutes = (millisRemaining / 1000) / 60
                val seconds = (millisRemaining / 1000) % 60
                tvFareHold.text = String.format(Locale.getDefault(), "Fare held for %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                tvFareHold.text = "Fare hold expired"
                Toast.makeText(
                    this@PaymentActivity,
                    "Fare hold expired. Please choose your bus again.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }.start()
    }

    private fun attemptPayment() {
        val insufficientFundsRow = findViewById<LinearLayout>(R.id.tvInsufficientFunds)

        val userId = UserManager.getCurrentUserId(this)
        if (userId == null) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show()
            val loginIntent = Intent(this, LoginPage::class.java)
            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(loginIntent)
            finish()
            return
        }

        val price = details.schedule.price
        // The ticket id is generated up front so the Payment row can reference it (FK).
        val ticketId = TicketStore.newTicketId()

        val success = TransportCardManager.deduct(
            this,
            price,
            "Ticket: ${details.originStop.stopName} \u2192 ${details.destinationStop.stopName}",
            ticketId
        )

        if (!success) {
            insufficientFundsRow.visibility = View.VISIBLE
            return
        }

        insufficientFundsRow.visibility = View.GONE
        bindWalletBalance()
        fareHoldTimer?.cancel()


        val ticket = Ticket(
            ticketId = ticketId,
            userId = userId,
            scheduleId = details.schedule.scheduleId,
            price = price // snapshot of what was actually paid
        )
        val levelBefore = TicketStore.getXpProgress(this).level
        TicketStore.saveActiveTicket(this, ticket)
        val xpEarned = TicketStore.addToHistory(this, ticket)
        TravelHistoryStore.add(this, ticket)
        val levelAfter = TicketStore.getXpProgress(this).level

        val newLevel = if (levelAfter > levelBefore) levelAfter else null
        NotificationHelper.notifyTicketPurchased(this, ticket, xpEarned, newLevel)
        Toast.makeText(this, "Ticket purchased! Check your notifications.", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, Dashboard::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    }
}