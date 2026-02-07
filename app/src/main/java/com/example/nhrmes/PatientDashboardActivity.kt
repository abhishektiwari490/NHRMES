package com.example.nhrmes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

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
        FirebaseFirestore.getInstance()
            .collection("hospitals")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    hospitalList.clear()
                    for (doc in snapshot.documents) {
                        val hospital = doc.toObject(Hospital::class.java)
                        if (hospital != null) {
                            hospitalList.add(hospital)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }
}

