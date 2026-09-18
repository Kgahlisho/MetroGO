package com.example.metrogo


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TripAdapter(
    private var tripList: List<Trip>,
    private val onTripClick: (Trip) -> Unit = {}
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivStatus: ImageView = itemView.findViewById(R.id.ivStatus)
        val tvRoute: TextView = itemView.findViewById(R.id.tvTripRoute)
        val tvTime: TextView = itemView.findViewById(R.id.tvBoardingTime)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvCost: TextView = itemView.findViewById(R.id.tvCost)
        val tvPayment: TextView = itemView.findViewById(R.id.tvPaymentMethod)
        val tvXp: TextView = itemView.findViewById(R.id.tvXpEarned)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = tripList[position]

        holder.tvRoute.text = trip.route
        holder.tvTime.text = "Boarding Time: ${trip.boardingTime}"
        holder.tvDate.text = "Date: ${trip.date}"
        holder.tvCost.text = "Cost: ${trip.cost}"
        holder.tvPayment.text = "Paid via: ${trip.paymentMethod}"
        holder.tvXp.text = "XP Earned: ${trip.xpEarned}xp"
        holder.itemView.setOnClickListener { onTripClick(trip) }

        if (trip.isPaid) {
            holder.ivStatus.setImageResource(R.drawable.ic_check_circle_green) // Ensure you have this drawable
            holder.ivStatus.setColorFilter(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            holder.ivStatus.setImageResource(R.drawable.ic_close_circle_red) // Ensure you have this drawable
            holder.ivStatus.setColorFilter(android.graphics.Color.parseColor("#F44336"))
        }
    }

    override fun getItemCount() = tripList.size

    // Method to update the list when filtering
    fun updateList(newList: List<Trip>) {
        tripList = newList
        notifyDataSetChanged()
    }
}