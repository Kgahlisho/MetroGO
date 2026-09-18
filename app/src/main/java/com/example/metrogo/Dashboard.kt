package com.example.metrogo

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.DecimalFormat

class Dashboard : AppCompatActivity() {

    private val currencyFormat = DecimalFormat("#,##0.00")

    // Kept so the tap-to-enlarge dialog can reuse the same QR bitmap/caption
    // that is currently shown on the dashboard, without regenerating it.
    private var currentQrBitmap: Bitmap? = null
    private var currentBoardingStatus: String? = null
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
        val circleNotification = findViewById<FrameLayout>(R.id.circleNotification)
        circleNotification.setOnClickListener{
            val intent = Intent(this , NotificationPage::class.java)
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

    override fun onResume() {
        super.onResume()
        // Refresh every time the dashboard becomes visible, so a ticket bought on
        // PurchaseTicket/PaymentActivity -- or a top-up made on Wallet -- shows up immediately.
        refreshWalletBalance()
        refreshBoardingPass()
    }

    private fun refreshWalletBalance() {
        val balance = TicketManager.getBalance(this)
        findViewById<TextView>(R.id.tvBalance).text = "R${currencyFormat.format(balance)}"
    }

    private fun refreshBoardingPass() {
        val qrImageView = findViewById<ImageView>(R.id.BoardingCode)
        val statusText = findViewById<TextView>(R.id.tvBoardingStatus)
        val tapToEnlargeLabel = findViewById<TextView>(R.id.tvTapToEnlarge)

        val ticket = TicketManager.getActiveTicket(this)
        if (ticket == null) {
            qrImageView.setImageDrawable(null)
            statusText.text = "No active boarding pass\nPurchase a ticket to get one"
            currentQrBitmap = null
            currentBoardingStatus = null
            qrImageView.setOnClickListener(null)
            tapToEnlargeLabel.visibility = android.view.View.GONE
            return
        }

        val qrBitmap = TicketManager.generateQrBitmap(ticket.toQrPayload())
        qrImageView.setImageBitmap(qrBitmap)

        statusText.textSize = 12f
        statusText.text = "Active: ${ticket.transportName}\n" +
                "${ticket.origin} \u2192 ${ticket.destination}\n" +
                "Departs ${ticket.departureTime} \u2022 Bus ${ticket.registration}"

        // Cache the current QR + caption so the enlarged dialog shows exactly
        // what's on the dashboard, and wire up the tap-to-enlarge affordance.
        currentQrBitmap = qrBitmap
        currentBoardingStatus = statusText.text.toString()
        tapToEnlargeLabel.visibility = android.view.View.VISIBLE
        val openEnlargedQr = { showEnlargedQrDialog() }
        qrImageView.setOnClickListener { openEnlargedQr() }
        tapToEnlargeLabel.setOnClickListener { openEnlargedQr() }
    }

    /** Shows the boarding pass QR code full-screen-ish, for easier scanning. */
    private fun showEnlargedQrDialog() {
        val bitmap = currentQrBitmap ?: return

        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setContentView(R.layout.dialog_qr_enlarged)

        dialog.findViewById<ImageView>(R.id.ivQrEnlarged).setImageBitmap(bitmap)
        dialog.findViewById<TextView>(R.id.tvDialogStatus).text = currentBoardingStatus
        dialog.findViewById<ImageView>(R.id.btnCloseDialog).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }
}