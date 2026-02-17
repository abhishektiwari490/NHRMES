package com.example.nhrmes

data class Hospital(
    val name: String = "",
    val location: String = "",
    val phone: String = "",
    val icuBedsAvailable: Int = 0,
    val oxygenBedsAvailable: Int = 0,
    val ventilatorsAvailable: Int = 0,
    val emergencyReady: Boolean = false,
    val lastUpdated: Long = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0

)
