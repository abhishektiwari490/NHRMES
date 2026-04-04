package com.example.nhrmes

data class Doctor(
    val id: String = "",
    val name: String = "",
    val specialist: String = "",
    val hospitalId: String = "",
    val fees: Int = 0,
    val startTime: String = "09:00", // e.g., 09:00
    val endTime: String = "17:00"    // e.g., 17:00
)