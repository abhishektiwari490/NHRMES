package com.example.nhrmes

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var editName: EditText
    private lateinit var editPhone: EditText
    private lateinit var txtEmail: TextView
    private lateinit var txtEmailTop: TextView
    private lateinit var editSpecialist: EditText
    private lateinit var editHospital: EditText
    private lateinit var doctorFields: View
    private lateinit var btnUpdate: Button
    private lateinit var userRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        editName = findViewById(R.id.profileName)
        editPhone = findViewById(R.id.profilePhone)
        txtEmail = findViewById(R.id.profileEmail)
        txtEmailTop = findViewById(R.id.txtProfileEmailTop)
        editSpecialist = findViewById(R.id.profileSpecialist)
        editHospital = findViewById(R.id.profileWorkingHospital)
        doctorFields = findViewById(R.id.profileDoctorFields)
        btnUpdate = findViewById(R.id.btnUpdateProfile)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        loadProfileData()

        btnUpdate.setOnClickListener {
            updateProfile()
        }
    }

    private fun loadProfileData() {
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").value?.toString() ?: ""
                val phone = snapshot.child("phone").value?.toString() ?: ""
                val email = snapshot.child("email").value?.toString() ?: ""
                val role = snapshot.child("role").value?.toString() ?: "patient"

                editName.setText(name)
                editPhone.setText(phone)
                txtEmail.text = email
                txtEmailTop.text = email
                
                if (role == "doctor") {
                    doctorFields.visibility = View.VISIBLE
                    editSpecialist.setText(snapshot.child("specialist").value?.toString() ?: "")
                    editHospital.setText(snapshot.child("hospital").value?.toString() ?: "")
                } else {
                    doctorFields.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateProfile() {
        val name = editName.text.toString().trim()
        val phone = editPhone.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = HashMap<String, Any>()
        updates["name"] = name
        updates["phone"] = phone
        
        if (doctorFields.visibility == View.VISIBLE) {
            updates["specialist"] = editSpecialist.text.toString().trim()
            updates["hospital"] = editHospital.text.toString().trim()
        }

        userRef.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Update Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
