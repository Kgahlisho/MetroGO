package com.example.metrogo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
class Busrouteadapter(

    private val routes: List<BusRoute>,
    private val onSelect: (BusRoute) -> Unit

): RecyclerView.Adapter<Busrouteadapter.BusRouteViewHolder>() {

    class BusRouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTransportName: TextView = itemView.findViewById(R.id.tvTransportName)
        val tvRoute: TextView = itemView.findViewById(R.id.tvRoute)
        val tvDeparture: TextView = itemView.findViewById(R.id.tvDeparture)
        val tvArrival: TextView = itemView.findViewById(R.id.tvArrival)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val btnSelect: TextView = itemView.findViewById(R.id.btnSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusRouteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bus_route, parent, false)
        return BusRouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusRouteViewHolder, position: Int) {
        val route = routes[position]
        holder.tvTransportName.text = route.transportName
        holder.tvRoute.text = "${route.origin} \u2192 ${route.destination}"
        holder.tvDeparture.text = "Departs ${route.departureTime}"
        holder.tvArrival.text = "Arrives ${route.arrivalTime}"
        holder.tvPrice.text = "R${route.price}"

        val selectAction = { onSelect(route) }
        holder.btnSelect.setOnClickListener { selectAction() }
        holder.itemView.setOnClickListener { selectAction() }
    }

    override fun getItemCount() = routes.size
}