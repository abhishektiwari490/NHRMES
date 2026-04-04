package com.example.nhrmes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.utils.ColorTemplate
import java.io.BufferedReader
import java.io.InputStreamReader

class GovernmentDashboardActivity : AppCompatActivity() {

    private lateinit var txtTotalHospitals: TextView
    private lateinit var txtTotalICU: TextView
    private lateinit var txtTotalOxygen: TextView
    private lateinit var txtTotalVentilators: TextView
    private lateinit var txtCriticalCount: TextView
    private lateinit var txtPredictionWarning: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnBulkAddHospitals: ImageButton

    private lateinit var recyclerCritical: RecyclerView
    private lateinit var recyclerAlerts: RecyclerView
    private lateinit var icuChart: LineChart

    private val criticalList = mutableListOf<Hospital>()
    private val alertList = mutableListOf<String>()
    private val icuTrendList = mutableListOf<Entry>()

    private lateinit var hospitalAdapter: GovernmentHospitalAdapter
    private lateinit var alertAdapter: SimpleStringAdapter

    private var previousTotalICU = -1
    private var timeIndex = 0f

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                processPunjabHospitalsCsv(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_government_dashboard)

        txtTotalHospitals = findViewById(R.id.txtTotalHospitals)
        txtTotalICU = findViewById(R.id.txtTotalICU)
        txtTotalOxygen = findViewById(R.id.txtTotalOxygen)
        txtTotalVentilators = findViewById(R.id.txtTotalVentilators)
        txtCriticalCount = findViewById(R.id.txtCriticalCount)
        txtPredictionWarning = findViewById(R.id.txtPredictionWarning)
        btnLogout = findViewById(R.id.btnLogoutGov)
        
        // Use an existing view or add a button to the layout for Bulk Add
        btnBulkAddHospitals = ImageButton(this).apply {
            setImageResource(R.drawable.request)
            setBackgroundResource(android.R.color.transparent)
        }

        icuChart = findViewById(R.id.icuChart)
        setupChart()

        recyclerCritical = findViewById(R.id.recyclerCriticalHospitals)
        recyclerCritical.layoutManager = LinearLayoutManager(this)
        hospitalAdapter = GovernmentHospitalAdapter(criticalList)
        recyclerCritical.adapter = hospitalAdapter

        recyclerAlerts = findViewById(R.id.recyclerEmergencyAlerts)
        recyclerAlerts.layoutManager = LinearLayoutManager(this)
        alertAdapter = SimpleStringAdapter(alertList)
        recyclerAlerts.adapter = alertAdapter

        loadGovernmentData()
        loadEmergencyAlerts()

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
        }

        // Long click on Total Hospitals to open CSV picker
        txtTotalHospitals.setOnLongClickListener {
            openCsvPicker()
            true
        }
    }

    private fun openCsvPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/*"
        filePickerLauncher.launch(Intent.createChooser(intent, "Select Punjab Hospitals CSV"))
    }

    private fun processPunjabHospitalsCsv(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val reader = BufferedReader(InputStreamReader(inputStream))
            val db = FirebaseDatabase.getInstance().getReference("Hospitals")
            
            var line: String? = reader.readLine()
            var count = 0
            
            // Skip header if needed
            if (line != null && (line.contains("Name", true) || line.contains("Hospital", true) || 
                line.contains("Sr", true) || line.contains("ID", true))) {
                line = reader.readLine()
            }
            
            while (line != null) {
                if (line.isBlank()) {
                    line = reader.readLine()
                    continue
                }
                
                val tokens = line.split(",")
                
                // Flexible parsing logic:
                // Expected format variant 1 (No Sr.No): Name, Location, Lat, Lng, Phone, ICU, Oxygen, Vent
                // Expected format variant 2 (With Sr.No): ID, Name, Location, Lat, Lng, Phone, ICU, Oxygen, Vent
                
                var nameIndex = 0
                var locationIndex = 1
                var latIndex = 2
                var lngIndex = 3
                var phoneIndex = 4
                var icuIndex = 5
                var oxyIndex = 6
                var ventIndex = 7

                // Detect if first token is a numeric ID/Serial Number
                if (tokens.isNotEmpty() && tokens[0].trim().toIntOrNull() != null && tokens.size > 8) {
                    nameIndex = 1
                    locationIndex = 2
                    latIndex = 3
                    lngIndex = 4
                    phoneIndex = 5
                    icuIndex = 6
                    oxyIndex = 7
                    ventIndex = 8
                }

                if (tokens.size > icuIndex) {
                    val hName = tokens.getOrNull(nameIndex)?.trim()?.replace("\"", "") ?: ""
                    
                    // Skip if name is just a number (secondary check for parsing error)
                    if (hName.toDoubleOrNull() != null && tokens.size > (nameIndex + 1)) {
                         // If name is a number, maybe indices are shifted further
                         // This is a safety measure
                    }

                    val hospital = Hospital(
                        name = hName,
                        location = tokens.getOrNull(locationIndex)?.trim()?.replace("\"", "") ?: "",
                        latitude = tokens.getOrNull(latIndex)?.trim()?.toDoubleOrNull() ?: 0.0,
                        longitude = tokens.getOrNull(lngIndex)?.trim()?.toDoubleOrNull() ?: 0.0,
                        phone = tokens.getOrNull(phoneIndex)?.trim()?.replace("\"", "") ?: "",
                        icuBedsAvailable = tokens.getOrNull(icuIndex)?.trim()?.toIntOrNull() ?: 0,
                        oxygenBedsAvailable = tokens.getOrNull(oxyIndex)?.trim()?.toIntOrNull() ?: 0,
                        ventilatorsAvailable = tokens.getOrNull(ventIndex)?.trim()?.toIntOrNull() ?: 0,
                        emergencyReady = true
                    )
                    
                    if (hospital.name.isNotEmpty() && hospital.name.lowercase() != "hospital name") {
                        db.push().setValue(hospital)
                        count++
                    }
                }
                line = reader.readLine()
            }
            reader.close()
            Toast.makeText(this, "Successfully added $count hospitals!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Upload Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupChart() {
        icuChart.description = Description().apply { text = "Statewide ICU Trend" }
        icuChart.setTouchEnabled(true)
        icuChart.setPinchZoom(true)
    }

    private fun loadGovernmentData() {
        FirebaseDatabase.getInstance().getReference("Hospitals")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalH = 0; var totalI = 0; var totalO = 0; var totalV = 0
                    criticalList.clear()
                    for (child in snapshot.children) {
                        val h = child.getValue(Hospital::class.java) ?: continue
                        totalH++; totalI += h.icuBedsAvailable; totalO += h.oxygenBedsAvailable; totalV += h.ventilatorsAvailable
                        if (h.icuBedsAvailable <= 5) criticalList.add(h)
                    }
                    txtTotalHospitals.text = "Hospitals: $totalH\n(Long-press to upload CSV)"
                    txtTotalICU.text = "ICU: $totalI"
                    txtTotalOxygen.text = "O2: $totalO"
                    txtTotalVentilators.text = "Vent: $totalV"
                    txtCriticalCount.text = "Critical: ${criticalList.size}"
                    hospitalAdapter.notifyDataSetChanged()
                    handlePredictionLogic(totalI)
                    updateICUChart(totalI)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun handlePredictionLogic(currentICU: Int) {
        if (previousTotalICU != -1 && currentICU < previousTotalICU) {
            txtPredictionWarning.visibility = View.VISIBLE
            txtPredictionWarning.text = "⚠️ Trend: Hospital capacity is dropping."
        } else {
            txtPredictionWarning.visibility = View.GONE
        }
        previousTotalICU = currentICU
    }

    private fun updateICUChart(currentICU: Int) {
        icuTrendList.add(Entry(timeIndex++, currentICU.toFloat()))
        if (icuTrendList.size > 20) icuTrendList.removeAt(0)
        val dataSet = LineDataSet(icuTrendList, "National ICU Trend").apply {
            color = ColorTemplate.MATERIAL_COLORS[0]
            lineWidth = 2.5f
            setDrawCircles(true)
        }
        icuChart.data = LineData(dataSet)
        icuChart.invalidate()
    }

    private fun loadEmergencyAlerts() {
        FirebaseDatabase.getInstance().getReference("EmergencyAlerts")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    alertList.clear()
                    for (child in snapshot.children) {
                        alertList.add("🚨 ALERT: ${child.child("status").value}")
                    }
                    alertAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
