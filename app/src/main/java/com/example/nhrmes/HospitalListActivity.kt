package com.example.nhrmes

import android.Manifest
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
import com.google.firebase.database.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HospitalListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var database: DatabaseReference
    private lateinit var adapter: HospitalAdapter

    private val hospitalList = mutableListOf<Hospital>()
    private val fullHospitalList = mutableListOf<Hospital>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hospital_list)

        recyclerView = findViewById(R.id.recyclerHospitals)
        etSearch = findViewById(R.id.etSearch)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = HospitalAdapter(hospitalList)
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().getReference("Hospitals")

        requestLocationPermission()
        
        // You can choose between Firebase or Retrofit
        // By default, we try the API first
        loadHospitalsFromApi()
        
        setupSearch()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }
    }

    private fun loadHospitalsFromApi() {
        RetrofitClient.instance.getHospitals("YOUR_API_KEY").enqueue(object : Callback<List<Hospital>> {
            override fun onResponse(call: Call<List<Hospital>>, response: Response<List<Hospital>>) {
                if (response.isSuccessful) {
                    hospitalList.clear()
                    fullHospitalList.clear()
                    response.body()?.let {
                        hospitalList.addAll(it)
                        fullHospitalList.addAll(it)
                    }
                    sortByLocation()
                } else {
                    // Fallback to Firebase if API fails
                    loadHospitalsFromFirebase()
                    Toast.makeText(this@HospitalListActivity, "API Error, loading from Firebase", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Hospital>>, t: Throwable) {
                // Fallback to Firebase if request fails
                loadHospitalsFromFirebase()
                Toast.makeText(this@HospitalListActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HospitalListActivity, "Database error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sortByLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) return@addOnSuccessListener

                val userLat = location.latitude
                val userLng = location.longitude

                for (hospital in hospitalList) {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        userLat,
                        userLng,
                        hospital.latitude,
                        hospital.longitude,
                        results
                    )
                    hospital.distance = results[0] / 1000.0
                }

                hospitalList.sortBy { it.distance }
                adapter.notifyDataSetChanged()
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
        if (query.isEmpty()) {
            hospitalList.addAll(fullHospitalList)
        } else {
            for (hospital in fullHospitalList) {
                if (hospital.name.contains(query, true)
                    || hospital.location.contains(query, true)
                ) {
                    hospitalList.add(hospital)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }
}
