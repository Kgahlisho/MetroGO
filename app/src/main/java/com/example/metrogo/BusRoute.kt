package com.example.metrogo

import java.io.Serializable

data class BusRoute (

    val id: Int,
    val transportName: String,
    val origin : String,
    val destination : String,
    val departureTime: String,
    val arrivalTime : String,
    val price : Int,
    val registration : String,
    val durationMinutes: Int = 0

):Serializable