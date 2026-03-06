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

    private val myList = mutableListOf<Any>() // can store both types

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_requests)

        recyclerView = findViewById(R.id.recyclerMyRequests)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = MyRequestAdapter(myList)
        recyclerView.adapter = adapter

        loadEmergencyRequests()
        loadAppointments()
    }

    private fun loadEmergencyRequests() {

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        FirebaseDatabase.getInstance()
            .getReference("EmergencyRequests")
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    myList.removeAll { it is EmergencyRequest }

                    for (child in snapshot.children) {

                        val request =
                            child.getValue(EmergencyRequest::class.java)

                        if (request != null) {
                            myList.add(request)
                        }
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadAppointments() {

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        FirebaseDatabase.getInstance()
            .getReference("Appointments")
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    myList.removeAll { it is Appointment }

                    for (child in snapshot.children) {

                        val appointment =
                            child.getValue(Appointment::class.java)

                        if (appointment != null) {
                            myList.add(appointment)
                        }
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}