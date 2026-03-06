package com.example.nhrmes

data class EmergencyRequest(
    val userId: String = "",
    val hospitalId: String = "",
    val bedType: String = "",
    val status: String = "Pending",
    val priority: String = "Normal", // 🔥 NEW
    val timestamp: Long = 0
)