package com.example.nhrmes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class HospitalListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var hospitalList: ArrayList<Hospital>
    private lateinit var database: DatabaseReference
    private lateinit var adapter: HospitalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hospital_list_activity)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        hospitalList = ArrayList()
        adapter = HospitalAdapter(hospitalList)
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().getReference("Hospitals")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                hospitalList.clear()

                for (data in snapshot.children) {
                    val hospital = data.getValue(Hospital::class.java)
                    hospital?.let { hospitalList.add(it) }
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
