package com.example.metrogo

import java.io.Serializable

data class Schedule(


    val scheduleId: String,
    val routeId: String, // FK -> TransportRoute
    val departureTime: String,
    val arrivalTime: String,
    val price: Int,
    val busRegistration: String,
    val durationMinutes: Int = 0
) : Serializable