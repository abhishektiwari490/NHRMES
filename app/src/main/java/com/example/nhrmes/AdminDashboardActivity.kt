package com.example.nhrmes

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminCombinedAdapter

    private lateinit var txtTotal: TextView
    private lateinit var txtPending: TextView
    private lateinit var txtApproved: TextView
    private lateinit var txtRejected: TextView
    private lateinit var btnLogoutAdmin: Button

    // 🔥 Master & Filtered List
    private val masterList = mutableListOf<AdminItem>()
    private val filteredList = mutableListOf<AdminItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        recyclerView = findViewById(R.id.recyclerAdminRequests)
        txtTotal = findViewById(R.id.txtTotal)
        txtPending = findViewById(R.id.txtPending)
        txtApproved = findViewById(R.id.txtApproved)
        txtRejected = findViewById(R.id.txtRejected)
        btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminCombinedAdapter(filteredList)
        recyclerView.adapter = adapter

        setupFilters()
        loadAllData()

        btnLogoutAdmin.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // =============================
    // LOAD ALL DATA
    // =============================
    private fun loadAllData() {

        loadEmergencyRequests()
        loadAppointments()
    }

    // =============================
    // LOAD EMERGENCY REQUESTS
    // =============================
    private fun loadEmergencyRequests() {

        FirebaseDatabase.getInstance()
            .getReference("EmergencyRequests")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    // Remove old REQUEST items
                    masterList.removeAll { it.type == "REQUEST" }

                    var total = 0
                    var pending = 0
                    var approved = 0
                    var rejected = 0

                    for (child in snapshot.children) {

                        val request =
                            child.getValue(EmergencyRequest::class.java)

                        val requestId = child.key ?: continue

                        if (request != null) {

                            total++

                            when (request.status) {
                                "Pending" -> pending++
                                "Approved" -> approved++
                                "Rejected" -> rejected++
                            }

                            masterList.add(
                                AdminItem(
                                    id = requestId,
                                    type = "REQUEST",
                                    request = request
                                )
                            )
                        }
                    }

                    updateCounters(total, pending, approved, rejected)
                    applyFilter("ALL")
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // =============================
    // LOAD APPOINTMENTS
    // =============================
    private fun loadAppointments() {

        FirebaseDatabase.getInstance()
            .getReference("Appointments")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    // Remove old APPOINTMENT items
                    masterList.removeAll { it.type == "APPOINTMENT" }

                    for (child in snapshot.children) {

                        val appointment =
                            child.getValue(Appointment::class.java)

                        val appointmentId = child.key ?: continue

                        if (appointment != null) {

                            masterList.add(
                                AdminItem(
                                    id = appointmentId,
                                    type = "APPOINTMENT",
                                    appointment = appointment
                                )
                            )
                        }
                    }

                    applyFilter("ALL")
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // =============================
    // FILTER FUNCTION
    // =============================
    private fun applyFilter(status: String) {

        filteredList.clear()

        if (status == "ALL") {
            filteredList.addAll(masterList)
        } else {
            filteredList.addAll(
                masterList.filter {
                    when (it.type) {
                        "REQUEST" -> it.request?.status == status
                        "APPOINTMENT" -> it.appointment?.status == status
                        else -> false
                    }
                }
            )
        }

        adapter.notifyDataSetChanged()
    }

    // =============================
    // CLICKABLE FILTERS
    // =============================
    private fun setupFilters() {

        txtTotal.setOnClickListener {
            highlightSelected(txtTotal)
            applyFilter("ALL")
        }

        txtPending.setOnClickListener {
            highlightSelected(txtPending)
            applyFilter("Pending")
        }

        txtApproved.setOnClickListener {
            highlightSelected(txtApproved)
            applyFilter("Approved")
        }

        txtRejected.setOnClickListener {
            highlightSelected(txtRejected)
            applyFilter("Rejected")
        }
    }

    // =============================
    // HIGHLIGHT SELECTED FILTER
    // =============================
    private fun highlightSelected(selected: TextView) {

        txtTotal.alpha = 0.5f
        txtPending.alpha = 0.5f
        txtApproved.alpha = 0.5f
        txtRejected.alpha = 0.5f

        selected.alpha = 1f
    }

    // =============================
    // UPDATE COUNTERS
    // =============================
    private fun updateCounters(
        total: Int,
        pending: Int,
        approved: Int,
        rejected: Int
    ) {
        txtTotal.text = "Total\n$total"
        txtPending.text = "Pending\n$pending"
        txtApproved.text = "Approved\n$approved"
        txtRejected.text = "Rejected\n$rejected"
    }
}