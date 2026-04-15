package com.example.nhrmes

data class Hospital(
    val name: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val icuBedsAvailable: Int = 0,
    val oxygenBedsAvailable: Int = 0,
    val ventilatorsAvailable: Int = 0,
    val emergencyReady: Boolean = false,
    val phone: String = "",
    var distance: Double = 0.0,
    
    // New Standout Feature: Resource Inventory
    val bloodStock: Map<String, Int> = mapOf(
        "A+" to 0, "A-" to 0, "B+" to 0, "B-" to 0,
        "O+" to 0, "O-" to 0, "AB+" to 0, "AB-" to 0
    )
)