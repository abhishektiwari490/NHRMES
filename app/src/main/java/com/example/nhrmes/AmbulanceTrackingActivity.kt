package com.example.nhrmes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class AmbulanceTrackingActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var txtEta: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtDriverName: TextView
    private lateinit var txtVehicleNumber: TextView
    private lateinit var btnCallDriver: ExtendedFloatingActionButton
    private lateinit var btnShareTracking: FloatingActionButton
    private lateinit var btnBack: FloatingActionButton
    
    private lateinit var database: DatabaseReference
    private var ambulanceMarker: Marker? = null
    
    private var requestId: String? = null
    private var driverPhone: String? = null
    private var currentVehicle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        
        setContentView(R.layout.activity_ambulance_tracking)

        requestId = intent.getStringExtra("REQUEST_ID")
        
        map = findViewById(R.id.mapTracking)
        txtEta = findViewById(R.id.txtTrackingEta)
        txtStatus = findViewById(R.id.txtTrackingStatus)
        txtDriverName = findViewById(R.id.txtDriverName)
        txtVehicleNumber = findViewById(R.id.txtVehicleNumber)
        btnCallDriver = findViewById(R.id.btnCallDriver)
        btnShareTracking = findViewById(R.id.btnShareTracking)
        btnBack = findViewById(R.id.btnBack)

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(17.0)

        database = FirebaseDatabase.getInstance().getReference("EmergencyRequests").child(requestId!!)
        
        btnBack.setOnClickListener { finish() }
        
        btnCallDriver.setOnClickListener {
            if (!driverPhone.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$driverPhone")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Driver contact not available yet", Toast.LENGTH_SHORT).show()
            }
        }

        btnShareTracking.setOnClickListener {
            shareTrackingDetails()
        }
        
        startTracking()
    }

    private fun startTracking() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("ambulanceLat").getValue(Double::class.java) ?: 0.0
                val lng = snapshot.child("ambulanceLng").getValue(Double::class.java) ?: 0.0
                val eta = snapshot.child("ambulanceETA").value?.toString() ?: "Calculating..."
                val status = snapshot.child("status").value?.toString() ?: ""
                val driverName = snapshot.child("driverName").value?.toString() ?: "Assigned Driver"
                currentVehicle = snapshot.child("vehicleNumber").value?.toString() ?: "Ambulance"
                
                driverPhone = snapshot.child("driverPhone").value?.toString()

                txtEta.text = "ETA: $eta"
                txtStatus.text = status
                txtDriverName.text = "Driver: $driverName"
                txtVehicleNumber.text = "Vehicle: $currentVehicle"

                if (lat != 0.0 && lng != 0.0) {
                    val pos = GeoPoint(lat, lng)
                    updateMarker(pos)
                    map.controller.animateTo(pos)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun shareTrackingDetails() {
        val shareMessage = "Emergency Alert: Ambulance ($currentVehicle) is on the way. \n" +
                "Status: ${txtStatus.text}\n" +
                "ETA: ${txtEta.text}\n" +
                "Track here: [Live Tracking link would go here]"
        
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        startActivity(Intent.createChooser(intent, "Share Tracking with Family"))
    }

    private fun updateMarker(pos: GeoPoint) {
        if (ambulanceMarker == null) {
            ambulanceMarker = Marker(map)
            ambulanceMarker?.title = "Ambulance"
            ambulanceMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(ambulanceMarker)
        }
        ambulanceMarker?.position = pos
        map.invalidate()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}