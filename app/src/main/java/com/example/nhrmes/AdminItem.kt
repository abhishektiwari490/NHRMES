package com.example.nhrmes

data class AdminItem(

    val id: String = "",

    // Type can be:
    // REQUEST
    // SOS
    // APPOINTMENT
    val type: String = "",

    val request: EmergencyRequest? = null,
    val appointment: Appointment? = null,
    val status: String? = null
)