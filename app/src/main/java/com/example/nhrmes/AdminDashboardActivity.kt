package com.example.nhrmes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.BufferedReader
import java.io.InputStreamReader

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminCombinedAdapter

    private lateinit var txtTotal: TextView
    private lateinit var txtPending: TextView
    private lateinit var txtApproved: TextView
    private lateinit var txtRejected: TextView
    private lateinit var btnLogoutAdmin: Button
    private lateinit var btnUpdateResources: ImageButton
    private lateinit var btnBulkUpload: ImageButton
    
    // Resource views
    private lateinit var txtAdminICU: TextView
    private lateinit var txtAdminOxygen: TextView
    private lateinit var txtAdminVent: TextView

    private val masterList = mutableListOf<AdminItem>()
    private val filteredList = mutableListOf<AdminItem>()
    
    private var myHospitalId: String? = null
    private lateinit var database: DatabaseReference
    private val auth = FirebaseAuth.getInstance()

    // CSV File Picker
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                processCsvFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        database = FirebaseDatabase.getInstance().reference

        // Initialize Views
        recyclerView = findViewById(R.id.recyclerAdminRequests)
        txtTotal = findViewById(R.id.txtTotal)
        txtPending = findViewById(R.id.txtPending)
        txtApproved = findViewById(R.id.txtApproved)
        txtRejected = findViewById(R.id.txtRejected)
        btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin)
        btnUpdateResources = findViewById(R.id.btnUpdateResources)
        btnBulkUpload = findViewById(R.id.btnBulkUpload)
        
        txtAdminICU = findViewById(R.id.txtAdminICU)
        txtAdminOxygen = findViewById(R.id.txtAdminOxygen)
        txtAdminVent = findViewById(R.id.txtAdminVent)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdminCombinedAdapter(filteredList)
        recyclerView.adapter = adapter

        setupFilters()
        fetchAdminHospital()

        btnLogoutAdmin.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnUpdateResources.setOnClickListener {
            showUpdateResourcesDialog()
        }

        btnBulkUpload.setOnClickListener {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/*" // Target CSV/Text files
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        filePickerLauncher.launch(Intent.createChooser(intent, "Select CSV File"))
    }

    private fun processCsvFile(uri: Uri) {
        val hospId = myHospitalId ?: return
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            // Format: resource_type,count
            // Example line: icu,25
            
            val updates = mutableMapOf<String, Any>()
            var line: String? = reader.readLine()
            while (line != null) {
                val tokens = line.split(",")
                if (tokens.size >= 2) {
                    val key = tokens[0].lowercase().trim()
                    val value = tokens[1].trim().toIntOrNull() ?: 0
                    
                    when (key) {
                        "icu" -> updates["icuBedsAvailable"] = value
                        "oxygen" -> updates["oxygenBedsAvailable"] = value
                        "vent" -> updates["ventilatorsAvailable"] = value
                    }
                }
                line = reader.readLine()
            }
            
            if (updates.isNotEmpty()) {
                database.child("Hospitals").child(hospId).updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Bulk Resources Updated!", Toast.LENGTH_SHORT).show()
                    }
            }
            reader.close()
        } catch (e: Exception) {
            Toast.makeText(this, "File Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAdminHospital() {
        val uid = auth.currentUser?.uid ?: return
        database.child("Users").child(uid).get().addOnSuccessListener { snapshot ->
            myHospitalId = snapshot.child("hospitalId").value?.toString()
            
            if (myHospitalId != null) {
                loadHospitalResources()
                loadAllData()
            } else {
                Toast.makeText(this, "Hospital ID not found for this admin", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadHospitalResources() {
        val hospId = myHospitalId ?: return
        database.child("Hospitals").child(hospId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val icu = snapshot.child("icuBedsAvailable").value?.toString() ?: "0"
                val oxy = snapshot.child("oxygenBedsAvailable").value?.toString() ?: "0"
                val vent = snapshot.child("ventilatorsAvailable").value?.toString() ?: "0"
                
                txtAdminICU.text = icu
                txtAdminOxygen.text = oxy
                txtAdminVent.text = vent
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showUpdateResourcesDialog() {
        val hospId = myHospitalId ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_resources, null)
        val etICU = dialogView.findViewById<EditText>(R.id.etUpdateICU)
        val etOxy = dialogView.findViewById<EditText>(R.id.etUpdateOxygen)
        val etVent = dialogView.findViewById<EditText>(R.id.etUpdateVent)

        etICU.setText(txtAdminICU.text)
        etOxy.setText(txtAdminOxygen.text)
        etVent.setText(txtAdminVent.text)

        AlertDialog.Builder(this)
            .setTitle("Update Resource Availability")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val updates = mapOf(
                    "icuBedsAvailable" to (etICU.text.toString().toIntOrNull() ?: 0),
                    "oxygenBedsAvailable" to (etOxy.text.toString().toIntOrNull() ?: 0),
                    "ventilatorsAvailable" to (etVent.text.toString().toIntOrNull() ?: 0)
                )
                database.child("Hospitals").child(hospId).updateChildren(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Resources Updated", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadAllData() {
        loadEmergencyRequests()
        loadAppointments()
    }

    private fun loadEmergencyRequests() {
        val hospId = myHospitalId ?: return
        database.child("EmergencyRequests")
            .orderByChild("hospitalId")
            .equalTo(hospId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    masterList.removeAll { it.type == "REQUEST" }

                    var total = 0
                    var pending = 0
                    var active = 0 
                    var closed = 0

                    for (child in snapshot.children) {
                        val request = child.getValue(EmergencyRequest::class.java)
                        val requestId = child.key ?: continue

                        if (request != null) {
                            total++
                            when (request.status) {
                                "Pending" -> pending++
                                "Ambulance On The Way", "Reached" -> active++
                                "Completed", "Rejected" -> closed++
                            }

                            masterList.add(AdminItem(id = requestId, type = "REQUEST", request = request))
                        }
                    }

                    updateCounters(total, pending, active, closed)
                    applyFilter("ALL")
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadAppointments() {
        val hospId = myHospitalId ?: return
        database.child("Appointments")
            .orderByChild("hospitalId")
            .equalTo(hospId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    masterList.removeAll { it.type == "APPOINTMENT" }
                    for (child in snapshot.children) {
                        val appointment = child.getValue(Appointment::class.java)
                        val appointmentId = child.key ?: continue
                        if (appointment != null) {
                            masterList.add(AdminItem(id = appointmentId, type = "APPOINTMENT", appointment = appointment))
                        }
                    }
                    applyFilter("ALL")
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun applyFilter(status: String) {
        filteredList.clear()
        if (status == "ALL") {
            filteredList.addAll(masterList)
        } else {
            filteredList.addAll(masterList.filter {
                when (it.type) {
                    "REQUEST" -> {
                        if (status == "Active") (it.request?.status == "Ambulance On The Way" || it.request?.status == "Reached")
                        else if (status == "Closed") (it.request?.status == "Completed" || it.request?.status == "Rejected")
                        else it.request?.status == status
                    }
                    "APPOINTMENT" -> it.appointment?.status == status
                    else -> false
                }
            })
        }
        adapter.notifyDataSetChanged()
    }

    private fun setupFilters() {
        txtTotal.setOnClickListener { highlightSelected(txtTotal); applyFilter("ALL") }
        txtPending.setOnClickListener { highlightSelected(txtPending); applyFilter("Pending") }
        txtApproved.setOnClickListener { highlightSelected(txtApproved); applyFilter("Active") }
        txtRejected.setOnClickListener { highlightSelected(txtRejected); applyFilter("Closed") }
    }

    private fun highlightSelected(selected: TextView) {
        txtTotal.alpha = 0.5f; txtPending.alpha = 0.5f; txtApproved.alpha = 0.5f; txtRejected.alpha = 0.5f
        selected.alpha = 1f
    }

    private fun updateCounters(total: Int, pending: Int, active: Int, closed: Int) {
        txtTotal.text = "Total\n$total"
        txtPending.text = "Pending\n$pending"
        txtApproved.text = "Active\n$active"
        txtRejected.text = "Closed\n$closed"
    }
}
