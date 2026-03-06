package com.example.nhrmes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyRequestsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyRequestAdapter

    // Adapter now expects Pair<String, Any>
    private val requestList = mutableListOf<Pair<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_requests)

        recyclerView = findViewById(R.id.recyclerMyRequests)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MyRequestAdapter(requestList)
        recyclerView.adapter = adapter

        loadEmergencyRequests()
        loadAppointments()
    }

    // ===================================
    // Load Emergency Requests
    // ===================================
    private fun loadEmergencyRequests() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance()
            .getReference("EmergencyRequests")
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    // remove old emergency requests
                    requestList.removeAll { it.second is EmergencyRequest }

                    for (child in snapshot.children) {

                        val request =
                            child.getValue(EmergencyRequest::class.java)

                        val requestId = child.key ?: continue

                        if (request != null) {

                            requestList.add(
                                Pair(requestId, request)
                            )
                        }
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ===================================
    // Load Appointments
    // ===================================
    private fun loadAppointments() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance()
            .getReference("Appointments")
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    // remove old appointments
                    requestList.removeAll { it.second is Appointment }

                    for (child in snapshot.children) {

                        val appointment =
                            child.getValue(Appointment::class.java)

                        val appointmentId = child.key ?: continue

                        if (appointment != null) {

                            requestList.add(
                                Pair(appointmentId, appointment)
                            )
                        }
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}