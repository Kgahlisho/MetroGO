package com.example.metrogo

import java.io.Serializable

data class TransportRoute(

    val routeId: String,
    val routeName: String,
    val originStopId: String,      // FK -> BusStop
    val destinationStopId: String, // FK -> BusStop
    val status: String = "active"
) : Serializable