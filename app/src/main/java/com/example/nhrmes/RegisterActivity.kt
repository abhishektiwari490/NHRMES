package com.example.nhrmes

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var name: EditText
    private lateinit var phone: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var registerBtn: Button
    private lateinit var roleGroup: RadioGroup
    private lateinit var roleSelectionLayout: View
    private lateinit var txtTitle: TextView
    
    private var selectedEntryPoint: String = "patient"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        selectedEntryPoint = intent.getStringExtra("SELECTED_ROLE") ?: "patient"

        name = findViewById(R.id.name)
        phone = findViewById(R.id.phone)
        email = findViewById(R.id.email)
        password = findViewById(R.id.pwd)
        registerBtn = findViewById(R.id.register)
        roleGroup = findViewById(R.id.roleGroup)
        roleSelectionLayout = findViewById(R.id.roleSelectionLayout)
        txtTitle = findViewById(R.id.registerTitle)

        // Adjust UI for Government entry
        if (selectedEntryPoint == "government") {
            roleSelectionLayout.visibility = View.GONE
            txtTitle.text = "Government Official Registration"
        }

        registerBtn.setOnClickListener {
            val nameText = name.text.toString().trim()
            val phoneText = phone.text.toString().trim()
            val emailText = email.text.toString().trim()
            val pwdText = password.text.toString().trim()
            
            // Determine role
            val role = if (selectedEntryPoint == "government") {
                "government"
            } else {
                if (roleGroup.checkedRadioButtonId == R.id.radioDoctor) "doctor" else "patient"
            }

            if (nameText.isEmpty() || phoneText.isEmpty() || emailText.isEmpty() || pwdText.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(emailText, pwdText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser!!.uid
                        val database = FirebaseDatabase.getInstance().getReference("Users")

                        val userMap = HashMap<String, Any>()
                        userMap["name"] = nameText
                        userMap["phone"] = phoneText
                        userMap["email"] = emailText
                        userMap["role"] = role

                        database.child(userId).setValue(userMap).addOnSuccessListener {
                            Toast.makeText(this, "Registered Successfully as $role", Toast.LENGTH_SHORT).show()
                            
                            when (role) {
                                "government" -> startActivity(Intent(this, GovernmentDashboardActivity::class.java))
                                "doctor" -> {
                                    val intent = Intent(this, AddDoctorActivity::class.java)
                                    intent.putExtra("isFirstTime", true)
                                    startActivity(intent)
                                }
                                else -> startActivity(Intent(this, PatientDashboardActivity::class.java))
                            }
                            finish()
                        }
                    } else {
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
