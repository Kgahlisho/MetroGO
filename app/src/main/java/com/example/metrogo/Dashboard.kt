package com.example.metrogo

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Dashboard : AppCompatActivity() {

    private val currencyFormat = DecimalFormat("#,##0.00")

    companion object {
        private const val CREDIT_BAR_MAX = 5000
    }

    // Kept so the tap-to-enlarge dialog can reuse the same QR bitmap/caption
    // that is currently shown on the dashboard, without regenerating it.
    private var currentQrBitmap: Bitmap? = null
    private var currentBoardingStatus: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!UserManager.isLoggedIn(this)) {
            val intent = Intent(this, LoginPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

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

        // Tapping the balance card opens the wallet overview pop-up.
        findViewById<View>(R.id.balanceCard).setOnClickListener { showBalanceDetailsDialog() }

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
        refreshWelcomeName()
    }

    private fun refreshWelcomeName() {
        val name = UserManager.getCurrentUser(this)?.fullName ?: "Guest"
        findViewById<TextView>(R.id.tvWelcomeName).text = name
    }

    private fun refreshWalletBalance() {
        val balance = TransportCardManager.getBalance(this)
        findViewById<TextView>(R.id.tvBalance).text = "R${currencyFormat.format(balance)}"

        // Credit progress bar: how much of CREDIT_BAR_MAX the user currently holds.
        val percent = ((balance.toLong() * 100) / CREDIT_BAR_MAX).toInt().coerceIn(0, 100)
        findViewById<ProgressBar>(R.id.pbCredit).progress = percent
        findViewById<TextView>(R.id.tvCreditCaption).text =
            "$percent% of R${DecimalFormat("#,##0").format(CREDIT_BAR_MAX)}"
    }

    private fun showBalanceDetailsDialog() {
        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.dialog_balance_details)

        dialog.findViewById<ImageView>(R.id.btnCloseBalanceDialog).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<TextView>(R.id.btnDialogTopUp).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, Wallet::class.java))
        }

        // Header: available credits + progress
        val balance = TransportCardManager.getBalance(this)
        val percent = ((balance.toLong() * 100) / CREDIT_BAR_MAX).toInt().coerceIn(0, 100)
        dialog.findViewById<TextView>(R.id.tvDialogBalance).text = "R${currencyFormat.format(balance)}"
        dialog.findViewById<ProgressBar>(R.id.pbDialogCredit).progress = percent
        dialog.findViewById<TextView>(R.id.tvDialogCreditCaption).text =
            "$percent% of R${DecimalFormat("#,##0").format(CREDIT_BAR_MAX)}"

        //  Body
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val container = dialog.findViewById<LinearLayout>(R.id.layoutBalanceRows)
        val inflater = LayoutInflater.from(this)

        fun addRow(
            iconRes: Int,
            label: String,
            value: String,
            sub: String,
            progress: Int? = null,
            isLast: Boolean = false
        ) {
            val row = inflater.inflate(R.layout.item_balance_detail, container, false)
            row.findViewById<ImageView>(R.id.ivDetailIcon).setImageResource(iconRes)
            row.findViewById<TextView>(R.id.tvDetailLabel).text = label
            row.findViewById<TextView>(R.id.tvDetailValue).text = value
            row.findViewById<TextView>(R.id.tvDetailSub).text = sub
            if (progress != null) {
                val pb = row.findViewById<ProgressBar>(R.id.pbDetail)
                pb.progress = progress
                pb.visibility = View.VISIBLE
            }
            if (isLast) row.findViewById<View>(R.id.viewDetailDivider).visibility = View.GONE
            container.addView(row)
        }

        // Last purchase: the most recent ticket the user bought.
        val lastTrip = TicketStore.getTripHistory(this).firstOrNull()
        if (lastTrip != null) {
            addRow(
                R.drawable.ic_ticket,
                "LAST PURCHASE",
                "R${currencyFormat.format(lastTrip.ticket.price)}",
                "${lastTrip.route}\n${dateFormat.format(Date(lastTrip.ticket.purchaseDate))}"
            )
        } else {
            addRow(R.drawable.ic_ticket, "LAST PURCHASE", "No purchases yet", "Buy a ticket to see it here")
        }

        // Last top-up: the most recent wallet top-up.
        val lastTopUp = PaymentStore.getAll(this)
            .firstOrNull { it.type == Payment.TYPE_TOPUP }
        if (lastTopUp != null) {
            addRow(
                R.drawable.ic_wallet,
                "LAST CREDIT TOP-UP",
                "R${currencyFormat.format(lastTopUp.amount)}",
                dateFormat.format(Date(lastTopUp.paymentDate))
            )
        } else {
            addRow(R.drawable.ic_wallet, "LAST CREDIT TOP-UP", "No top-ups yet", "Top up your wallet to see it here")
        }

        // Exp  level. The level increases/changes to the next level when it reaches 100
        val xp = TicketStore.getXpProgress(this)
        val toNext = TicketStore.XP_PER_LEVEL - xp.xpInLevel
        addRow(
            R.drawable.ic_star,
            "EXPERIENCE LEVEL",
            "Level ${xp.level}",
            "${xp.xpInLevel}/${TicketStore.XP_PER_LEVEL} XP \u2022 $toNext XP to Level ${xp.level + 1}",
            progress = xp.xpInLevel * 100 / TicketStore.XP_PER_LEVEL,
            isLast = true
        )

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        LanguageManager.translateDialog(dialog)
        dialog.show()
    }

    private fun refreshBoardingPass() {
        val qrImageView = findViewById<ImageView>(R.id.BoardingCode)
        val statusText = findViewById<TextView>(R.id.tvBoardingStatus)
        val tapToEnlargeLabel = findViewById<TextView>(R.id.tvTapToEnlarge)

        val ticket = TicketStore.getActiveTicket(this)
        val details = ticket?.let { TransportRouteRepository.scheduleDetails(it.scheduleId) }
        if (ticket == null || details == null) {
            qrImageView.setImageDrawable(null)
            statusText.text = "No active boarding pass\nPurchase a ticket to get one"
            currentQrBitmap = null
            currentBoardingStatus = null
            qrImageView.setOnClickListener(null)
            tapToEnlargeLabel.visibility = android.view.View.GONE
            return
        }

        val passengerName = UserManager.getCurrentUser(this)?.fullName ?: "Guest"
        val qrBitmap = TicketStore.generateQrBitmap(ticket.toQrPayload(passengerName, details))
        qrImageView.setImageBitmap(qrBitmap)

        statusText.textSize = 12f
        statusText.text = "Active: ${details.route.routeName}\n" +
                "${details.originStop.stopName} \u2192 ${details.destinationStop.stopName}\n" +
                "Departs ${details.schedule.departureTime} \u2022 Bus ${details.schedule.busRegistration}"

        currentQrBitmap = qrBitmap
        currentBoardingStatus = statusText.text.toString()
        tapToEnlargeLabel.visibility = android.view.View.VISIBLE
        val openEnlargedQr = { showEnlargedQrDialog() }
        qrImageView.setOnClickListener { openEnlargedQr() }
        tapToEnlargeLabel.setOnClickListener { openEnlargedQr() }
    }

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
        LanguageManager.translateDialog(dialog)
        dialog.show()
    }
}