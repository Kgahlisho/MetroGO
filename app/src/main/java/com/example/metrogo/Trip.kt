package com.example.metrogo

data class Trip(


    val id : Int,
    val route: String,
    val boardingTime : String,
    val date: String,
    val cost : String,
    val paymentMethod: String,
    val xpEarned: Int,
    val isPaid: Boolean
)
