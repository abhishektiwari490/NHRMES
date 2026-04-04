package com.example.nhrmes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class PatientDashboardActivity : AppCompatActivity() {

    private lateinit var txtWelcome: TextView
    private lateinit var txtUserNameLabel: TextView
    private lateinit var txtTotalHospitals: TextView
    private lateinit var txtICUBeds: TextView
    private lateinit var txtAmbulances: TextView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fabSOS: ExtendedFloatingActionButton
    private lateinit var cardNearbyMap: CardView

    private lateinit var database: DatabaseReference
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_dashboard)

        // Initialize Views
        txtWelcome = findViewById(R.id.txtWelcome)
        txtUserNameLabel = findViewById(R.id.txtUserEmail)
        txtTotalHospitals = findViewById(R.id.txtTotalHospitals)
        txtICUBeds = findViewById(R.id.txtICUBeds)
        txtAmbulances = findViewById(R.id.txtAmbulances)
        bottomNav = findViewById(R.id.bottom_navigation)
        fabSOS = findViewById(R.id.fabSOS)
        cardNearbyMap = findViewById(R.id.cardNearbyMap)

        database = FirebaseDatabase.getInstance().reference

        setupUserData()
        setupStats()
        setupClickListeners()
        setupBottomNav()
        
        // Dynamic Greeting and Status bar color
        window.statusBarColor = getColor(R.color.primary_dark)
    }

    private fun setupUserData() {
        val user = auth.currentUser ?: return
        
        database.child("Users").child(user.uid).get().addOnSuccessListener { snapshot ->
            val name = snapshot.child("name").value?.toString() ?: "User"
            
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            
            val greeting = when (hour) {
                in 0..11 -> "Good Morning"
                in 12..15 -> "Good Afternoon"
                in 16..20 -> "Good Evening"
                else -> "Good Night"
            }
            
            val emoji = when (hour) {
                in 0..11 -> "☀️"
                in 12..15 -> "🌤️" 
                in 16..20 -> "🌆"
                else -> "🌙"
            }

            txtWelcome.text = "$emoji $greeting,"
            txtUserNameLabel.text = name
        }.addOnFailureListener {
            txtUserNameLabel.text = user.email
        }
    }

    private fun setupStats() {
        database.child("Hospitals").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val totalHospitals = snapshot.childrenCount
                var totalICU = 0
                
                for (hosp in snapshot.children) {
                    totalICU += hosp.child("icuBedsAvailable").getValue(Int::class.java) ?: 0
                }

                animateText(txtTotalHospitals, totalHospitals.toInt())
                animateText(txtICUBeds, totalICU)
                animateText(txtAmbulances, (totalHospitals * 2).toInt())
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun animateText(view: TextView, value: Int) {
        val current = view.text.toString().toIntOrNull() ?: 0
        if (current == value) return
        
        view.text = value.toString()
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(500).start()
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.cardViewHospitals).setOnClickListener {
            startActivity(Intent(this, HospitalListActivity::class.java))
        }
        findViewById<View>(R.id.cardRequestEmergency).setOnClickListener {
            startActivity(Intent(this, EmergencyRequestActivity::class.java))
        }
        findViewById<View>(R.id.cardMyRequests).setOnClickListener {
            startActivity(Intent(this, MyRequestsActivity::class.java))
        }
        findViewById<View>(R.id.cardBookAppointment).setOnClickListener {
            startActivity(Intent(this, AppointmentActivity::class.java))
        }
        findViewById<View>(R.id.cardSOS).setOnClickListener { sendEmergencyAlert() }
        fabSOS.setOnClickListener { sendEmergencyAlert() }
        
        findViewById<View>(R.id.imgProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        cardNearbyMap.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_hospitals -> {
                    startActivity(Intent(this, HospitalListActivity::class.java))
                    false // Don't highlight to keep Home as active
                }
                R.id.nav_requests -> {
                    startActivity(Intent(this, MyRequestsActivity::class.java))
                    false
                }
                R.id.nav_logout -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }
    }

    private fun logoutUser() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun sendEmergencyAlert() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        Toast.makeText(this, "Sending SOS Alert...", Toast.LENGTH_SHORT).show()

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                Toast.makeText(this, "Location error. Please enable GPS.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            
            val userId = auth.currentUser?.uid ?: return@addOnSuccessListener
            val sosRef = database.child("SOSAlerts").push()
            
            val sosMap = mapOf(
                "userId" to userId,
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "timestamp" to ServerValue.TIMESTAMP,
                "status" to "Active"
            )
            
            sosRef.setValue(sosMap).addOnCompleteListener {
                Toast.makeText(this, "🚨 SOS Alert Sent! Emergency services notified.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
