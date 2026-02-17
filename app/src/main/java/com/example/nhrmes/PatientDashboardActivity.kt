package com.example.nhrmes

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PatientDashboardActivity : AppCompatActivity() {

    private lateinit var txtTotalHospitals: TextView
    private lateinit var txtAvailableICU: TextView
    private lateinit var btnViewHospitals: Button
    private lateinit var btnLogout: Button

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_dashboard)

        txtTotalHospitals = findViewById(R.id.txtTotalHospitals)
        txtAvailableICU = findViewById(R.id.txtAvailableICU)
        btnViewHospitals = findViewById(R.id.btnViewHospitals)
        btnLogout = findViewById(R.id.btnLogout)

        database = FirebaseDatabase.getInstance().getReference("Hospitals")

        loadDashboardData()

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnViewHospitals.setOnClickListener {
            startActivity(Intent(this, HospitalListActivity::class.java))
        }
    }

    private fun loadDashboardData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                var totalHospitals = 0
                var totalICU = 0

                for (hospitalSnapshot in snapshot.children) {
                    totalHospitals++
                    val icuBeds =
                        hospitalSnapshot.child("icuBedsAvailable")
                            .getValue(Int::class.java) ?: 0
                    totalICU += icuBeds
                }

                txtTotalHospitals.text = totalHospitals.toString()
                txtAvailableICU.text = totalICU.toString()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
