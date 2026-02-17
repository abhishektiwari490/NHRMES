package com.example.nhrmes

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EmergencyRequestActivity : AppCompatActivity() {

    private lateinit var spinnerBedType: Spinner
    private lateinit var btnSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_request)

        spinnerBedType = findViewById(R.id.spinnerBedType)
        btnSubmit = findViewById(R.id.btnSubmitRequest)

        val bedTypes = arrayOf("ICU", "Oxygen", "Ventilator")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bedTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBedType.adapter = adapter

        btnSubmit.setOnClickListener {

            val bedType = spinnerBedType.selectedItem.toString()
            val user = FirebaseAuth.getInstance().currentUser!!

            val requestRef = FirebaseDatabase.getInstance()
                .getReference("EmergencyRequests")

            val requestId = requestRef.push().key!!

            val requestMap = HashMap<String, Any>()
            requestMap["userId"] = user.uid
            requestMap["userEmail"] = user.email!!
            requestMap["hospitalId"] = "hospital1"
            requestMap["hospitalName"] = "AIIMS Delhi"
            requestMap["bedType"] = bedType
            requestMap["status"] = "Pending"
            requestMap["timestamp"] = System.currentTimeMillis()

            requestRef.child(requestId).setValue(requestMap)

            Toast.makeText(this,
                "Emergency Request Submitted Successfully",
                Toast.LENGTH_LONG).show()

            finish()
        }
    }
}
