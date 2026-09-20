package com.example.metrogo

import java.io.Serializable

data class BusStop(

    val stopId: String,
    val stopName: String,
    val latitude: Double,
    val longitude: Double
) : Serializable
