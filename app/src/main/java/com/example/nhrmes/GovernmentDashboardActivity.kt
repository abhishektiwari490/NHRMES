package com.example.nhrmes

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.utils.ColorTemplate

class GovernmentDashboardActivity : AppCompatActivity() {

    private lateinit var txtTotalHospitals: TextView
    private lateinit var txtTotalICU: TextView
    private lateinit var txtTotalOxygen: TextView
    private lateinit var txtTotalVentilators: TextView
    private lateinit var txtCriticalCount: TextView
    private lateinit var txtPredictionWarning: TextView
    private lateinit var btnLogout: Button

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_government_dashboard)

        // TextViews
        txtTotalHospitals = findViewById(R.id.txtTotalHospitals)
        txtTotalICU = findViewById(R.id.txtTotalICU)
        txtTotalOxygen = findViewById(R.id.txtTotalOxygen)
        txtTotalVentilators = findViewById(R.id.txtTotalVentilators)
        txtCriticalCount = findViewById(R.id.txtCriticalCount)
        txtPredictionWarning = findViewById(R.id.txtPredictionWarning)
        btnLogout = findViewById(R.id.btnLogoutGov)

        // Chart
        icuChart = findViewById(R.id.icuChart)
        setupChart()

        // RecyclerViews
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
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupChart() {
        icuChart.description = Description().apply {
            text = "ICU Availability Trend"
        }
        icuChart.setTouchEnabled(true)
        icuChart.setPinchZoom(true)
    }

    private fun loadGovernmentData() {

        FirebaseDatabase.getInstance()
            .getReference("Hospitals")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    var totalHospitals = 0
                    var totalICU = 0
                    var totalOxygen = 0
                    var totalVent = 0

                    criticalList.clear()

                    for (child in snapshot.children) {

                        val hospital = child.getValue(Hospital::class.java)

                        if (hospital != null) {

                            totalHospitals++
                            totalICU += hospital.icuBedsAvailable
                            totalOxygen += hospital.oxygenBedsAvailable
                            totalVent += hospital.ventilatorsAvailable

                            if (hospital.icuBedsAvailable <= 2 ||
                                hospital.oxygenBedsAvailable <= 3 ||
                                hospital.ventilatorsAvailable <= 1
                            ) {
                                criticalList.add(hospital)
                            }
                        }
                    }

                    // Update UI
                    txtTotalHospitals.text = "Hospitals\n$totalHospitals"
                    txtTotalICU.text = "ICU Beds\n$totalICU"
                    txtTotalOxygen.text = "Oxygen Beds\n$totalOxygen"
                    txtTotalVentilators.text = "Ventilators\n$totalVent"
                    txtCriticalCount.text = "Critical Hospitals: ${criticalList.size}"

                    hospitalAdapter.notifyDataSetChanged()

                    handlePredictionLogic(totalICU)
                    updateICUChart(totalICU)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun handlePredictionLogic(currentICU: Int) {

        if (previousTotalICU != -1) {

            when {
                currentICU <= 10 -> {
                    txtPredictionWarning.visibility = View.VISIBLE
                    txtPredictionWarning.text =
                        "🚨 CRITICAL: National ICU Level Very Low"
                }

                currentICU < previousTotalICU -> {
                    txtPredictionWarning.visibility = View.VISIBLE
                    txtPredictionWarning.text =
                        "⚠ ICU Trend Decreasing - Shortage Predicted"
                }

                else -> {
                    txtPredictionWarning.visibility = View.GONE
                }
            }
        }

        previousTotalICU = currentICU
    }

    private fun updateICUChart(currentICU: Int) {

        icuTrendList.add(Entry(timeIndex, currentICU.toFloat()))
        timeIndex++

        val dataSet = LineDataSet(icuTrendList, "ICU Beds")

        dataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        dataSet.valueTextSize = 10f
        dataSet.lineWidth = 2f
        dataSet.setCircleColor(android.graphics.Color.RED)

        val lineData = LineData(dataSet)

        icuChart.data = lineData
        icuChart.invalidate()
    }

    private fun loadEmergencyAlerts() {

        FirebaseDatabase.getInstance()
            .getReference("EmergencyAlerts")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    alertList.clear()

                    for (child in snapshot.children) {
                        val status =
                            child.child("status").value?.toString() ?: "Unknown"
                        alertList.add("🚨 $status")
                    }

                    alertAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}