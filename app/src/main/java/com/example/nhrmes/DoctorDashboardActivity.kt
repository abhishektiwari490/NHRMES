package com.example.nhrmes

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DoctorDashboardActivity : AppCompatActivity() {

    private lateinit var txtWelcome: TextView
    private lateinit var txtSpecialty: TextView
    private lateinit var txtTotal: TextView
    private lateinit var txtPending: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnLogout: Button
    private lateinit var switchAvailability: SwitchMaterial

    private lateinit var database: DatabaseReference
    private val auth = FirebaseAuth.getInstance()
    private val appointmentList = mutableListOf<Appointment>()
    private lateinit var adapter: MyAppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_dashboard)

        txtWelcome = findViewById(R.id.txtDoctorWelcome)
        txtSpecialty = findViewById(R.id.txtDoctorSpecialty)
        txtTotal = findViewById(R.id.txtTotalAppointments)
        txtPending = findViewById(R.id.txtPendingAppointments)
        recyclerView = findViewById(R.id.recyclerDoctorAppointments)
        btnLogout = findViewById(R.id.btnLogoutDoctor)
        switchAvailability = findViewById(R.id.switchAvailability)

        database = FirebaseDatabase.getInstance().reference
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MyAppointmentAdapter(appointmentList)
        recyclerView.adapter = adapter

        fetchDoctorData()
        setupAvailabilityToggle()

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
        }
    }

    private fun setupAvailabilityToggle() {
        val uid = auth.currentUser?.uid ?: return
        val statusRef = database.child("Doctors").child(uid).child("isOnline")
        
        // Initial state
        statusRef.get().addOnSuccessListener { snapshot ->
            switchAvailability.isChecked = snapshot.getValue(Boolean::class.java) ?: true
        }

        switchAvailability.setOnCheckedChangeListener { _, isChecked ->
            statusRef.setValue(isChecked).addOnSuccessListener {
                val msg = if (isChecked) "You are now Online" else "You are now Offline"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchDoctorData() {
        val uid = auth.currentUser?.uid ?: return
        
        database.child("Doctors").child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val name = snapshot.child("name").value?.toString() ?: "Doctor"
                val specialty = snapshot.child("specialist").value?.toString() ?: "General"
                txtWelcome.text = "Welcome, Dr. $name"
                txtSpecialty.text = specialty
                loadAppointments(uid)
            } else {
                database.child("Users").child(uid).get().addOnSuccessListener { userSnap ->
                    val name = userSnap.child("name").value?.toString() ?: "Doctor"
                    txtWelcome.text = "Welcome, Dr. $name"
                    loadAppointments(uid)
                }
            }
        }
    }

    private fun loadAppointments(doctorId: String) {
        database.child("Appointments").orderByChild("doctorId").equalTo(doctorId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    appointmentList.clear()
                    var pendingCount = 0
                    
                    for (child in snapshot.children) {
                        val appt = child.getValue(Appointment::class.java)
                        if (appt != null) {
                            appointmentList.add(appt)
                            if (appt.status == "Pending") pendingCount++
                        }
                    }
                    
                    txtTotal.text = appointmentList.size.toString()
                    txtPending.text = pendingCount.toString()
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DoctorDashboardActivity, "Error loading appointments", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
