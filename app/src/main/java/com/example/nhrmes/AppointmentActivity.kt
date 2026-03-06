package com.example.nhrmes

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AppointmentActivity : AppCompatActivity() {

    private lateinit var spinnerHospital: Spinner
    private lateinit var spinnerSpecialist: Spinner
    private lateinit var btnBook: Button

    private val hospitalList = mutableListOf<Pair<String, String>>()
    // Pair<hospitalId, hospitalName>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment)

        spinnerHospital = findViewById(R.id.spinnerHospital)
        spinnerSpecialist = findViewById(R.id.spinnerSpecialist)
        btnBook = findViewById(R.id.btnBookAppointment)

        loadHospitals()
        setupSpecialistSpinner()

        btnBook.setOnClickListener {
            bookAppointment()
        }
    }

    private fun loadHospitals() {

        FirebaseDatabase.getInstance()
            .getReference("Hospitals")
            .get()
            .addOnSuccessListener { snapshot ->

                hospitalList.clear()
                val hospitalNames = mutableListOf<String>()

                for (child in snapshot.children) {

                    val id = child.key ?: continue
                    val name = child.child("name").value?.toString() ?: "Unknown"

                    hospitalList.add(Pair(id, name))
                    hospitalNames.add(name)
                }

                spinnerHospital.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    hospitalNames
                )
            }
    }

    private fun setupSpecialistSpinner() {

        val specialists = listOf(
            "Cardiologist",
            "Eye Specialist",
            "Skin Specialist",
            "Orthopedic",
            "Neurologist",
            "General Physician",
            "Pediatrician"
        )

        spinnerSpecialist.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            specialists
        )
    }

    private fun bookAppointment() {

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        val selectedHospitalIndex = spinnerHospital.selectedItemPosition
        val hospitalId = hospitalList[selectedHospitalIndex].first

        val specialist = spinnerSpecialist.selectedItem.toString()

        val appointmentRef = FirebaseDatabase.getInstance()
            .getReference("Appointments")
            .push()

        val appointment = Appointment(
            userId = userId,
            hospitalId = hospitalId,
            specialist = specialist,
            status = "Pending",
            appointmentDate = "",
            appointmentTime = "",
            timestamp = System.currentTimeMillis()
        )

        appointmentRef.setValue(appointment)

        Toast.makeText(this, "Appointment Request Sent", Toast.LENGTH_LONG).show()
        finish()
    }
}