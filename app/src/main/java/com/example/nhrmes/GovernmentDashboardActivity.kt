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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.utils.ColorTemplate
import okhttp3.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class GovernmentDashboardActivity : AppCompatActivity() {

    private lateinit var txtTotalHospitals: TextView
    private lateinit var txtTotalICU: TextView
    private lateinit var txtTotalOxygen: TextView
    private lateinit var txtTotalVentilators: TextView
    private lateinit var txtCriticalCount: TextView
    private lateinit var txtPredictionWarning: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnManageData: ImageButton

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
        btnManageData = findViewById(R.id.btnManageData)

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

        btnManageData.setOnClickListener {
            showDeleteDataOptions()
        }

        txtTotalHospitals.setOnLongClickListener {
            showDeleteDataOptions()
            true
        }

        txtTotalICU.setOnLongClickListener {
            fetchHospitalsFromOSM()
            true
        }
    }

    private fun showDeleteDataOptions() {
        val options = arrayOf("Delete All Hospitals", "Delete Emergency Alerts", "Import Hospitals via CSV")
        AlertDialog.Builder(this)
            .setTitle("Government Data Management")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmDeletion("Hospitals", "This will delete all hospital records from the database.")
                    1 -> confirmDeletion("EmergencyAlerts", "This will clear all active emergency alerts.")
                    2 -> openCsvPicker()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeletion(node: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage(message)
            .setPositiveButton("Delete Everything") { _, _ ->
                FirebaseDatabase.getInstance().getReference(node).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Data cleared successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to clear data", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Keep Data", null)
            .show()
    }

    private fun openCsvPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/*"
        filePickerLauncher.launch(Intent.createChooser(intent, "Select Punjab Hospitals CSV"))
    }

    private fun fetchHospitalsFromOSM() {
        AlertDialog.Builder(this)
            .setTitle("Fetch Real-World Data")
            .setMessage("This will fetch thousands of hospitals from OpenStreetMap and add them to your database. Continue?")
            .setPositiveButton("Yes, Fetch All") { _, _ ->
                performOsmFetch()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performOsmFetch() {
        Toast.makeText(this, "Fetching real hospitals from OpenStreetMap...", Toast.LENGTH_SHORT).show()
        
        val url = "https://overpass-api.de/api/interpreter?data=[out:json];area[\"name\"=\"Punjab\"]->.a;(node[\"amenity\"=\"hospital\"](area.a);way[\"amenity\"=\"hospital\"](area.a););out center;"

        val request = Request.Builder().url(url).build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body()?.string() ?: return
                try {
                    val json = JSONObject(bodyString)
                    val elements = json.getJSONArray("elements")
                    val db = FirebaseDatabase.getInstance().getReference("Hospitals")

                    var count = 0
                    for (i in 0 until elements.length()) {
                        val obj = elements.getJSONObject(i)
                        val tags = obj.optJSONObject("tags") ?: continue
                        
                        val name = tags.optString("name", "Public Hospital")
                        val phone = tags.optString("phone", tags.optString("contact:phone", "N/A"))
                        val addr = tags.optString("addr:street", tags.optString("addr:full", "Punjab Region"))

                        val lat = if (obj.has("lat")) obj.getDouble("lat") else obj.optJSONObject("center")?.optDouble("lat") ?: 0.0
                        val lon = if (obj.has("lon")) obj.getDouble("lon") else obj.optJSONObject("center")?.optDouble("lon") ?: 0.0

                        if (lat == 0.0 || lon == 0.0) continue

                        val hospital = Hospital(
                            name = name,
                            location = addr,
                            latitude = lat,
                            longitude = lon,
                            phone = phone,
                            icuBedsAvailable = (2..15).random(),
                            oxygenBedsAvailable = (10..40).random(),
                            ventilatorsAvailable = (1..5).random(),
                            emergencyReady = true
                        )

                        db.push().setValue(hospital)
                        count++
                    }

                    runOnUiThread {
                        Toast.makeText(this@GovernmentDashboardActivity, "Added $count real hospitals from OSM!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this@GovernmentDashboardActivity, "Parsing Error", Toast.LENGTH_SHORT).show() }
                } finally {
                    response.close()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@GovernmentDashboardActivity, "Network Error", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun processPunjabHospitalsCsv(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val reader = BufferedReader(InputStreamReader(inputStream))
            val db = FirebaseDatabase.getInstance().getReference("Hospitals")
            var line: String? = reader.readLine()
            var count = 0
            if (line != null && (line.contains("Name", true) || line.contains("Hospital", true))) {
                line = reader.readLine()
            }
            while (line != null) {
                if (line.isNotBlank()) {
                    val tokens = line.split(",")
                    if (tokens.size >= 8) {
                        val hospital = Hospital(
                            name = tokens[0].trim().replace("\"", ""),
                            location = tokens[1].trim().replace("\"", ""),
                            latitude = tokens[2].toDoubleOrNull() ?: 0.0,
                            longitude = tokens[3].toDoubleOrNull() ?: 0.0,
                            phone = tokens[4].trim(),
                            icuBedsAvailable = tokens[5].toIntOrNull() ?: 0,
                            oxygenBedsAvailable = tokens[6].toIntOrNull() ?: 0,
                            ventilatorsAvailable = tokens[7].toIntOrNull() ?: 0,
                            emergencyReady = true
                        )
                        db.push().setValue(hospital)
                        count++
                    }
                }
                line = reader.readLine()
            }
            reader.close()
            Toast.makeText(this, "Added $count hospitals from CSV", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "CSV Error", Toast.LENGTH_SHORT).show()
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
                    txtTotalHospitals.text = "Hospitals: $totalH"
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
