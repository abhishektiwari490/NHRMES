package com.example.nhrmes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PatientDashboardActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val hospitalList = mutableListOf<Hospital>()
    private lateinit var adapter: HospitalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_dashboard)

        recyclerView = findViewById(R.id.hospitalRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HospitalAdapter(hospitalList)
        recyclerView.adapter = adapter

        fetchHospitals()
    }

    private fun fetchHospitals() {
        FirebaseDatabase.getInstance().getReference("hospitals")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    hospitalList.clear()
                    for (dataSnapshot in snapshot.children) {
                        val hospital = dataSnapshot.getValue(Hospital::class.java)
                        if (hospital != null) {
                            hospitalList.add(hospital)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
}
