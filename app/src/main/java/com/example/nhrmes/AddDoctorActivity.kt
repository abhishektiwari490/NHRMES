package com.example.nhrmes

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class AddDoctorActivity : AppCompatActivity() {

    private val hospitalList = mutableListOf<Pair<String, String>>() // ID to Name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_doctor)

        val etName = findViewById<EditText>(R.id.etDocName)
        val etSpec = findViewById<EditText>(R.id.etDocSpecialist)
        val etFees = findViewById<EditText>(R.id.etDocFees)
        val etStart = findViewById<EditText>(R.id.etStartTime)
        val etEnd = findViewById<EditText>(R.id.etEndTime)
        val spinner = findViewById<Spinner>(R.id.spinnerHospitalsDoc)
        val btnSave = findViewById<Button>(R.id.btnSaveDoctor)

        loadHospitals(spinner)

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val spec = etSpec.text.toString().trim()
            val fees = etFees.text.toString().toIntOrNull() ?: 0
            val start = etStart.text.toString().trim()
            val end = etEnd.text.toString().trim()
            
            val selectedPos = spinner.selectedItemPosition
            if (selectedPos < 0) {
                Toast.makeText(this, "Select a hospital", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val hospId = hospitalList[selectedPos].first

            if (name.isEmpty() || spec.isEmpty()) {
                Toast.makeText(this, "Name and Specialization required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val database = FirebaseDatabase.getInstance().getReference("Doctors")
            val id = database.push().key ?: return@setOnClickListener

            val doctor = Doctor(
                id = id,
                name = name,
                specialist = spec,
                hospitalId = hospId,
                fees = fees,
                startTime = start,
                endTime = end
            )

            database.child(id).setValue(doctor).addOnSuccessListener {
                Toast.makeText(this, "Doctor Registered successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadHospitals(spinner: Spinner) {
        FirebaseDatabase.getInstance().getReference("Hospitals")
            .get().addOnSuccessListener { snapshot ->
                hospitalList.clear()
                val names = mutableListOf<String>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val name = child.child("name").value?.toString() ?: "Unknown"
                    hospitalList.add(id to name)
                    names.add(name)
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
                spinner.adapter = adapter
            }
    }
}
