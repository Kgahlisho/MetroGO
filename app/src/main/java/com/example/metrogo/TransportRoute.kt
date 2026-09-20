package com.example.metrogo

import java.io.Serializable

data class TransportRoute(

    val routeId: String,
    val routeName: String,
    val originStopId: String,
    val destinationStopId: String,
    val status: String = "active"
) : Serializable