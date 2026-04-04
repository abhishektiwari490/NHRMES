package com.example.nhrmes

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EmergencyRequestActivity : AppCompatActivity() {

    private lateinit var spinnerHospital: Spinner
    private lateinit var spinnerBedType: Spinner
    private lateinit var btnSubmit: Button
    private lateinit var txtHospDetails: TextView
    private lateinit var progressBar: ProgressBar
    
    private val hospitalList = mutableListOf<Hospital>()
    private val hospitalMap = mutableMapOf<String, String>() // Name to ID mapping

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_request)

        spinnerHospital = findViewById(R.id.spinnerHospital)
        spinnerBedType = findViewById(R.id.spinnerBedType)
        btnSubmit = findViewById(R.id.btnSubmitRequest)
        txtHospDetails = findViewById(R.id.txtHospitalResources)
        progressBar = findViewById(R.id.progressBar)

        setupBedTypeSpinner()
        checkLocationAndLoad()

        spinnerHospital.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateHospitalDetails(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSubmit.setOnClickListener {
            submitRequest()
        }
    }

    private fun setupBedTypeSpinner() {
        val bedTypes = arrayOf("ICU Bed", "Oxygen Bed", "Ventilator")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bedTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBedType.adapter = adapter
    }

    private fun checkLocationAndLoad() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            loadHospitals(null) // Load without sorting
        } else {
            LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener { location ->
                loadHospitals(location)
            }
        }
    }

    private fun loadHospitals(userLocation: Location?) {
        progressBar.visibility = View.VISIBLE
        FirebaseDatabase.getInstance().getReference("Hospitals")
            .get().addOnSuccessListener { snapshot ->
                hospitalList.clear()
                for (child in snapshot.children) {
                    val hospital = child.getValue(Hospital::class.java)
                    if (hospital != null) {
                        // Calculate distance if location is available
                        userLocation?.let {
                            val results = FloatArray(1)
                            Location.distanceBetween(it.latitude, it.longitude, hospital.latitude, hospital.longitude, results)
                            hospital.distance = results[0] / 1000.0
                        }
                        hospitalList.add(hospital)
                        hospitalMap[hospital.name] = child.key ?: ""
                    }
                }

                // Sort by nearest
                if (userLocation != null) {
                    hospitalList.sortBy { it.distance }
                }

                val spinnerItems = hospitalList.map { 
                    if (it.distance > 0) "${it.name} (${String.format("%.1f", it.distance)} km)" 
                    else it.name 
                }
                
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerItems)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerHospital.adapter = adapter
                progressBar.visibility = View.GONE
            }
    }

    private fun updateHospitalDetails(position: Int) {
        if (position >= hospitalList.size) return
        val h = hospitalList[position]
        
        val details = """
            🏥 ${h.name}
            📍 ${h.location}
            
            AVAILABLE RESOURCES:
            🔵 ICU Beds: ${h.icuBedsAvailable}
            🟢 Oxygen Beds: ${h.oxygenBedsAvailable}
            🟠 Ventilators: ${h.ventilatorsAvailable}
            👨‍⚕️ Doctors: ${h.icuBedsAvailable + 2} (Est.)
            
            ${if (h.icuBedsAvailable == 0) "⚠️ NO ICU BEDS AVAILABLE" else "✅ Resources Available"}
        """.trimIndent()
        
        txtHospDetails.text = details
        
        // Disable submit if no beds of selected type
        val selectedType = spinnerBedType.selectedItem.toString()
        val isAvailable = when(selectedType) {
            "ICU Bed" -> h.icuBedsAvailable > 0
            "Oxygen Bed" -> h.oxygenBedsAvailable > 0
            "Ventilator" -> h.ventilatorsAvailable > 0
            else -> false
        }
        
        btnSubmit.isEnabled = isAvailable
        if (!isAvailable) {
            Toast.makeText(this, "Selected resource is currently unavailable at this hospital", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitRequest() {
        val selectedHospPos = spinnerHospital.selectedItemPosition
        if (selectedHospPos < 0) return

        val hospital = hospitalList[selectedHospPos]
        val hospitalId = hospitalMap[hospital.name] ?: return
        val bedType = spinnerBedType.selectedItem.toString()
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val requestRef = FirebaseDatabase.getInstance().getReference("EmergencyRequests")
        val requestId = requestRef.push().key ?: return

        val requestMap = mapOf(
            "requestId" to requestId,
            "userId" to user.uid,
            "userEmail" to user.email,
            "hospitalId" to hospitalId,
            "hospitalName" to hospital.name,
            "bedType" to bedType,
            "status" to "Pending",
            "timestamp" to System.currentTimeMillis(),
            "priority" to "High"
        )

        requestRef.child(requestId).setValue(requestMap).addOnSuccessListener {
            Toast.makeText(this, "🚨 Emergency Request Submitted! Nearest hospital notified.", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
