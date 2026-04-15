package com.example.nhrmes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nhrmes.network.RetrofitClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HospitalListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var fabMap: FloatingActionButton
    private lateinit var database: DatabaseReference
    private lateinit var adapter: HospitalAdapter

    private val hospitalList = mutableListOf<Hospital>()
    private val fullHospitalList = mutableListOf<Hospital>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hospital_list)

        recyclerView = findViewById(R.id.recyclerHospitals)
        etSearch = findViewById(R.id.etSearch)
        chipGroup = findViewById(R.id.chipGroupFilters)
        fabMap = findViewById(R.id.fabMap)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = HospitalAdapter(hospitalList)
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().getReference("Hospitals")

        requestLocationPermission()
        loadHospitalsFromFirebase()
        
        setupSearch()
        setupFilters()
        
        fabMap.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        }
    }

    private fun loadHospitalsFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                hospitalList.clear()
                fullHospitalList.clear()

                for (child in snapshot.children) {
                    val hospital = child.getValue(Hospital::class.java)
                    if (hospital != null) {
                        hospitalList.add(hospital)
                        fullHospitalList.add(hospital)
                    }
                }
                sortByLocation()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupFilters() {
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedChipId = checkedIds.firstOrNull() ?: R.id.chipAll
            applyFilter(selectedChipId)
        }
    }

    private fun applyFilter(chipId: Int) {
        hospitalList.clear()
        when (chipId) {
            R.id.chipAll -> hospitalList.addAll(fullHospitalList)
            R.id.chipICU -> {
                hospitalList.addAll(fullHospitalList.filter { it.icuBedsAvailable > 0 })
            }
            R.id.chipOxygen -> {
                hospitalList.addAll(fullHospitalList.filter { it.oxygenBedsAvailable > 0 })
            }
            R.id.chipBlood -> {
                hospitalList.addAll(fullHospitalList.filter { 
                    it.bloodStock.values.any { stock -> stock > 0 } 
                })
            }
            R.id.chipNearby -> {
                hospitalList.addAll(fullHospitalList.filter { it.distance < 10.0 })
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun sortByLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) return@addOnSuccessListener
            for (hospital in fullHospitalList) {
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, hospital.latitude, hospital.longitude, results)
                hospital.distance = results[0] / 1000.0
            }
            fullHospitalList.sortBy { it.distance }
            applyFilter(chipGroup.checkedChipId)
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                filterHospitals(text.toString())
            }
        })
    }

    private fun filterHospitals(query: String) {
        hospitalList.clear()
        val baseList = fullHospitalList // Apply search on all data
        if (query.isEmpty()) {
            hospitalList.addAll(baseList)
        } else {
            for (hospital in baseList) {
                if (hospital.name.contains(query, true) || hospital.location.contains(query, true)) {
                    hospitalList.add(hospital)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }
}
