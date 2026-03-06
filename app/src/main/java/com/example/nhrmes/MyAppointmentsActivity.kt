package com.example.nhrmes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyAppointmentsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val appointmentList = mutableListOf<Appointment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_appointments)

        recyclerView = findViewById(R.id.recyclerMyAppointments)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadAppointments()
    }

    private fun loadAppointments() {

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        FirebaseDatabase.getInstance()
            .getReference("Appointments")
            .orderByChild("userId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    appointmentList.clear()

                    for (child in snapshot.children) {

                        val appointment =
                            child.getValue(Appointment::class.java)

                        if (appointment != null) {
                            appointmentList.add(appointment)
                        }
                    }

                    recyclerView.adapter =
                        MyAppointmentAdapter(appointmentList)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}