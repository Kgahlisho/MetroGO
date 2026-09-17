package com.example.metrogo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PaymentActivity : AppCompatActivity() {

    private lateinit var busRoute: BusRoute

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

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        bindTripSummary()
        bindWalletBalance()

        findViewById<TextView>(R.id.btnPayNow).setOnClickListener { attemptPayment() }
    }

    override fun onResume() {
        super.onResume()
        bindWalletBalance()
    }

    private fun bindTripSummary() {
        findViewById<TextView>(R.id.tvTransportName).text = busRoute.transportName
        findViewById<TextView>(R.id.tvRoute).text = "${busRoute.origin} \u2192 ${busRoute.destination}"
        findViewById<TextView>(R.id.tvPrice).text = "R${busRoute.price}"
        findViewById<TextView>(R.id.tvDeparture).text = busRoute.departureTime
        findViewById<TextView>(R.id.tvArrival).text = busRoute.arrivalTime
        findViewById<TextView>(R.id.tvRegistration).text = busRoute.registration
    }

    private fun bindWalletBalance() {
        val balance = TicketManager.getBalance(this)
        findViewById<TextView>(R.id.tvWalletBalance).text = "R$balance"
    }

    private fun attemptPayment() {
        val success = TicketManager.deduct(this, busRoute.price)
        if (!success) {
            findViewById<TextView>(R.id.tvInsufficientFunds).visibility = View.VISIBLE
            return
        }

        findViewById<TextView>(R.id.tvInsufficientFunds).visibility = View.GONE
        bindWalletBalance()

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
