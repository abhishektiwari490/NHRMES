package com.example.nhrmes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PatientDashboardActivity : AppCompatActivity() {

    private lateinit var txtWelcome: TextView
    private lateinit var txtTotalHospitals: TextView
    private lateinit var txtAvailableICU: TextView

    private lateinit var btnViewHospitals: Button
    private lateinit var btnRequestEmergency: Button
    private lateinit var btnMyRequests: Button
    private lateinit var btnBookAppointment: Button
    private lateinit var btnSOS: Button
    private lateinit var btnLogout: Button

    private lateinit var hospitalRef: DatabaseReference
    private lateinit var emergencyRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_dashboard)

        // TextViews
        txtWelcome = findViewById(R.id.txtWelcome)
        txtTotalHospitals = findViewById(R.id.txtTotalHospitals)
        txtAvailableICU = findViewById(R.id.txtAvailableICU)

        // Buttons
        btnViewHospitals = findViewById(R.id.btnViewHospitals)
        btnRequestEmergency = findViewById(R.id.btnRequestEmergency)
        btnMyRequests = findViewById(R.id.btnMyRequests)
        btnBookAppointment = findViewById(R.id.btnBookAppointment)
        btnSOS = findViewById(R.id.btnSOS)
        btnLogout = findViewById(R.id.btnLogout)

        val email = FirebaseAuth.getInstance().currentUser?.email ?: "User"
        txtWelcome.text = "Welcome, $email"

        hospitalRef = FirebaseDatabase.getInstance().getReference("Hospitals")
        emergencyRef = FirebaseDatabase.getInstance().getReference("EmergencyRequests")

        loadDashboardData()
        listenForAmbulanceUpdates()

        // ===============================
        // BUTTON CLICK LISTENERS
        // ===============================

        btnViewHospitals.setOnClickListener {
            startActivity(Intent(this, HospitalListActivity::class.java))
        }

        btnRequestEmergency.setOnClickListener {
            startActivity(Intent(this, EmergencyRequestActivity::class.java))
        }

        btnMyRequests.setOnClickListener {
            startActivity(Intent(this, MyRequestsActivity::class.java))
        }

        btnBookAppointment.setOnClickListener {
            startActivity(Intent(this, AppointmentActivity::class.java))
        }

        btnSOS.setOnClickListener {
            sendEmergencyAlert()
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // ===============================
    // Dashboard Data
    // ===============================
    private fun loadDashboardData() {

        hospitalRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                var totalHospitals = 0
                var totalICU = 0

                for (hospitalSnapshot in snapshot.children) {

                    totalHospitals++

                    val icuBeds = hospitalSnapshot
                        .child("icuBedsAvailable")
                        .getValue(Int::class.java) ?: 0

                    totalICU += icuBeds
                }

                txtTotalHospitals.text = totalHospitals.toString()
                txtAvailableICU.text = totalICU.toString()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // ===============================
    // SOS Logic
    // ===============================
    private fun sendEmergencyAlert() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->

            if (location == null) {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val userLat = location.latitude
            val userLng = location.longitude

            hospitalRef.get().addOnSuccessListener { snapshot ->

                var nearestHospitalId: String? = null
                var minDistance = Double.MAX_VALUE

                for (child in snapshot.children) {

                    val hospital = child.getValue(Hospital::class.java)

                    if (hospital != null) {

                        val distance = calculateDistance(
                            userLat,
                            userLng,
                            hospital.latitude,
                            hospital.longitude
                        )

                        if (distance < minDistance) {
                            minDistance = distance
                            nearestHospitalId = child.key
                        }
                    }
                }

                if (nearestHospitalId != null) {

                    val requestRef = emergencyRef.push()

                    val requestMap = HashMap<String, Any>()
                    requestMap["userId"] = userId
                    requestMap["hospitalId"] = nearestHospitalId!!
                    requestMap["bedType"] = "Emergency SOS"
                    requestMap["status"] = "Pending"
                    requestMap["timestamp"] = System.currentTimeMillis()

                    requestRef.setValue(requestMap)

                    Toast.makeText(
                        this,
                        "🚑 SOS Sent to Nearest Hospital!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ===============================
    // Listen Ambulance Updates
    // ===============================
    private fun listenForAmbulanceUpdates() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        emergencyRef.orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    for (child in snapshot.children) {

                        val status = child.child("status").value?.toString() ?: ""

                        if (status == "Ambulance On The Way") {

                            Toast.makeText(
                                this@PatientDashboardActivity,
                                "🚑 Ambulance Dispatched!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ===============================
    // Distance Calculation
    // ===============================
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {

        val radius = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return radius * c
    }
}