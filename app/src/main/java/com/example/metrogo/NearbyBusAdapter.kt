package com.example.metrogo

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BusCard(
    val key: String,
    val line: String,
    val label: String,
    val color: Int,
    val from: String,
    val to: String
) {
    var nextStop = ""
    var etaSeconds = 0
    var progress = 0
    var distM: Float? = null
    var crowd = ""
    var selected = false

    val eta: String get() = "%d:%02d".format(etaSeconds / 60, etaSeconds % 60)
    val distanceText: String? get() = distM?.let { "%.1f km away".format(it / 1000f) }
}

class NearbyBusAdapter(
    private val onClick: (BusCard) -> Unit
) : RecyclerView.Adapter<NearbyBusAdapter.VH>() {

    private var items: List<BusCard> = emptyList()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val badge: TextView = v.findViewById(R.id.tvLineBadge)
        val route: TextView = v.findViewById(R.id.tvBusRoute)
        val distance: TextView = v.findViewById(R.id.tvBusDistance)
        val next: TextView = v.findViewById(R.id.tvBusNext)
        val trip: ProgressBar = v.findViewById(R.id.pbBusTrip)
        val crowd: TextView = v.findViewById(R.id.tvBadgeCrowd)
    }

    fun submit(list: List<BusCard>) { items = list; notifyDataSetChanged() }
    fun refresh() { if (items.isNotEmpty()) notifyItemRangeChanged(0, items.size, "tick") }
    fun positionOf(key: String) = items.indexOfFirst { it.key == key }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_nearby_bus, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val c = items[position]
        val badgeBg = GradientDrawable().apply {
            cornerRadius = 12f * h.itemView.resources.displayMetrics.density
            setColor(c.color)
        }
        h.badge.background = badgeBg
        h.badge.text = c.line
        h.route.text = "${c.from} \u2192 ${c.to}"
        h.distance.text = c.distanceText ?: ""
        h.next.text = "Next stop: ${c.nextStop}  \u2022  ${c.eta}"
        h.trip.progress = c.progress
        h.trip.progressTintList = ColorStateList.valueOf(c.color)
        h.crowd.text = c.crowd
        h.itemView.setBackgroundResource(if (c.selected) R.drawable.bg_card_selected else R.drawable.bg_card_white)
        h.itemView.contentDescription =
            "${c.label}, ${c.from} to ${c.to}. Next stop ${c.nextStop}. Step-free boarding. ${c.crowd}. " +
                    (c.distanceText?.let { "$it. " } ?: "") +
                    if (c.selected) "Following. Double tap to stop." else "Double tap to follow."
        h.itemView.setOnClickListener { onClick(c) }
    }
}