package com.example.nhrmes

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class AddHospitalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_hospital)

        val etName = findViewById<EditText>(R.id.etHospName)
        val etAddress = findViewById<EditText>(R.id.etHospAddress)
        val etLat = findViewById<EditText>(R.id.etLat)
        val etLng = findViewById<EditText>(R.id.etLng)
        val etPhone = findViewById<EditText>(R.id.etHospPhone)
        val etICU = findViewById<EditText>(R.id.etICUBeds)
        val etOxy = findViewById<EditText>(R.id.etOxyBeds)
        val etVent = findViewById<EditText>(R.id.etVentBeds)
        val btnSave = findViewById<Button>(R.id.btnSaveHospital)

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val lat = etLat.text.toString().toDoubleOrNull() ?: 0.0
            val lng = etLng.text.toString().toDoubleOrNull() ?: 0.0
            val phone = etPhone.text.toString().trim()
            val icu = etICU.text.toString().toIntOrNull() ?: 0
            val oxy = etOxy.text.toString().toIntOrNull() ?: 0
            val vent = etVent.text.toString().toIntOrNull() ?: 0

            if (name.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Name and Address are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val database = FirebaseDatabase.getInstance().getReference("Hospitals")
            val id = database.push().key ?: return@setOnClickListener

            val hospital = Hospital(
                name = name,
                location = address,
                latitude = lat,
                longitude = lng,
                icuBedsAvailable = icu,
                oxygenBedsAvailable = oxy,
                ventilatorsAvailable = vent,
                phone = phone
            )

            database.child(id).setValue(hospital).addOnSuccessListener {
                Toast.makeText(this, "Hospital Registered successfully", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
