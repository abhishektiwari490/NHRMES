package com.example.nhrmes

data class EmergencyRequest(

    val userId: String = "",
    val hospitalId: String = "",
    val bedType: String = "",
    val priority: String = "High",
    val status: String = "Pending",
    val timestamp: Long = 0,

    val ambulanceETA: String = "",
    val ambulanceDistance: String = ""
)