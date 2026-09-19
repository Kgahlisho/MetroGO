package com.example.metrogo


import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
class NotificationAdapter(private var items: List<AppNotification>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivNotificationIcon)
        val tvTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        val tvMessage: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvNotificationTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val n = items[position]
        holder.tvTitle.text = n.title
        holder.tvMessage.text = n.message
        holder.tvTime.text = relativeTime(n.timestamp)
        holder.ivIcon.setImageResource(
            when (n.type) {
                AppNotification.TYPE_TICKET -> R.drawable.ic_ticket
                AppNotification.TYPE_WALLET -> R.drawable.ic_wallet
                else -> R.drawable.ic_notification_bell
            }
        )
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<AppNotification>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun relativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        if (now - timestamp < DateUtils.MINUTE_IN_MILLIS) return "Just now"
        return DateUtils.getRelativeTimeSpanString(
            timestamp, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
}

