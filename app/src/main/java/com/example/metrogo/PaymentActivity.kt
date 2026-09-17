package com.example.metrogo

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.FrameLayout
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

class PaymentActivity : AppCompatActivity() {

    private lateinit var busRoute: BusRoute
    private var fareHoldTimer : CountDownTimer? = null

    private val currencyFormat = DecimalFormat("#,##0.00")
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

        val extraRoute = intent.getSerializableExtra(EXTRA_BUS_ROUTE) as? BusRoute
        if (extraRoute == null) {
            Toast.makeText(this, "No bus selected.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        busRoute = extraRoute

        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

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
        findViewById<TextView>(R.id.tvTransportName).text = busRoute.transportName
        findViewById<TextView>(R.id.tvPrice).text = rand(busRoute.price)

        findViewById<TextView>(R.id.tvOrigin).text = busRoute.origin
        findViewById<TextView>(R.id.tvDestination).text = busRoute.destination
        findViewById<TextView>(R.id.tvOriginTime).text = "Departs ${busRoute.departureTime}"
        findViewById<TextView>(R.id.tvDestinationTime).text = "Arrives ${busRoute.arrivalTime}"
        findViewById<TextView>(R.id.tvDuration).text = "${busRoute.durationMinutes} min"
        findViewById<TextView>(R.id.tvRegistration).text = busRoute.registration

        // Service fee is a flat R0.00 for now (static in the layout) -- fare is the only
        // line item, so both totals just mirror the fare price.
        findViewById<TextView>(R.id.tvTotalAmount).text = rand(busRoute.price)
        findViewById<TextView>(R.id.tvTotalInline).text = rand(busRoute.price)
    }

    private fun bindWalletBalance() {
        val balance = TicketManager.getBalance(this)
        findViewById<TextView>(R.id.tvWalletBalance).text = rand(balance)

        val afterBalance = (balance - busRoute.price).coerceAtLeast(0)
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

        val success = TicketManager.deduct(this, busRoute.price)
        if (!success) {
            insufficientFundsRow.visibility = View.VISIBLE
            return
        }

        insufficientFundsRow.visibility = View.GONE
        bindWalletBalance()
        fareHoldTimer?.cancel()


        val ticket = Ticket(
            ticketId = TicketManager.newTicketId(),
            passengerName = "John Doe", // TODO: pull from the signed-in user's profile once auth is wired up
            transportName = busRoute.transportName,
            origin = busRoute.origin,
            destination = busRoute.destination,
            departureTime = busRoute.departureTime,
            arrivalTime = busRoute.arrivalTime,
            registration = busRoute.registration,
            price = busRoute.price,
            purchaseTimestamp = System.currentTimeMillis()
        )
        TicketManager.saveActiveTicket(this, ticket)

        Toast.makeText(this, "Ticket purchased! Your QR code is on the Dashboard.", Toast.LENGTH_LONG).show()
        val intent = Intent(this, Dashboard::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_BUS_ROUTE = "extra_bus_route"
    }
}
