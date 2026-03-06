package com.example.nhrmes

data class Appointment(
    val userId: String = "",
    val hospitalId: String = "",
    val specialist: String = "",
    val status: String = "Pending",
    val appointmentDate: String = "",
    val appointmentTime: String = "",
    val timestamp: Long = 0
)