package com.example.nhrmes

data class Hospital(
    val name: String = "",
    val distanceKm: Double = 0.0,
    val availableBeds: Int = 0,
    val doctorsAvailable: Int = 0,
    val emergencyReady: Boolean = false,
    val phone: String = ""
)
