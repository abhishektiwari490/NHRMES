package com.example.nhrmes

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var name: EditText
    private lateinit var phone: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var specialist: EditText
    private lateinit var workingHospital: EditText
    private lateinit var doctorFields: View
    private lateinit var registerBtn: Button
    private lateinit var roleGroup: RadioGroup
    private lateinit var roleSelectionLayout: View
    private lateinit var txtTitle: TextView
    private lateinit var cbPrivacyPolicy: CheckBox
    
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
        specialist = findViewById(R.id.specialist)
        workingHospital = findViewById(R.id.workingHospital)
        doctorFields = findViewById(R.id.doctorFields)
        
        registerBtn = findViewById(R.id.register)
        roleGroup = findViewById(R.id.roleGroup)
        roleSelectionLayout = findViewById(R.id.roleSelectionLayout)
        txtTitle = findViewById(R.id.registerTitle)
        cbPrivacyPolicy = findViewById(R.id.cbPrivacyPolicy)

        cbPrivacyPolicy.text = Html.fromHtml("I agree to the <font color='#0D6EFD'><u>Privacy Policy</u></font>")
        cbPrivacyPolicy.setOnLongClickListener {
            showPrivacyPolicyDialog()
            true
        }

        // Handle role selection changes to show/hide doctor fields
        roleGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioDoctor) {
                doctorFields.visibility = View.VISIBLE
            } else {
                doctorFields.visibility = View.GONE
            }
        }

        // Adjust UI for pre-selected role
        if (selectedEntryPoint == "government") {
            roleSelectionLayout.visibility = View.GONE
            txtTitle.text = "Government Official Registration"
            doctorFields.visibility = View.GONE
        } else if (selectedEntryPoint == "doctor") {
            roleGroup.check(R.id.radioDoctor)
            doctorFields.visibility = View.VISIBLE
        }

        registerBtn.setOnClickListener {
            val nameText = name.text.toString().trim()
            val phoneText = phone.text.toString().trim()
            val emailText = email.text.toString().trim()
            val pwdText = password.text.toString().trim()
            val specialistText = specialist.text.toString().trim()
            val hospitalText = workingHospital.text.toString().trim()
            
            if (!cbPrivacyPolicy.isChecked) {
                Toast.makeText(this, "Please accept the Privacy Policy to continue", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
            
            if (role == "doctor" && (specialistText.isEmpty() || hospitalText.isEmpty())) {
                Toast.makeText(this, "Doctors must provide specialist and hospital info", Toast.LENGTH_SHORT).show()
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
                        
                        if (role == "doctor") {
                            userMap["specialist"] = specialistText
                            userMap["hospital"] = hospitalText
                        }

                        database.child(userId).setValue(userMap).addOnSuccessListener {
                            Toast.makeText(this, "Registered Successfully as $role", Toast.LENGTH_SHORT).show()
                            
                            when (role) {
                                "government" -> startActivity(Intent(this, GovernmentDashboardActivity::class.java))
                                "doctor" -> {
                                    val intent = Intent(this, DoctorDashboardActivity::class.java)
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

    private fun showPrivacyPolicyDialog() {
        val policy = """
            <b>Privacy Policy for NHRMES</b><br><br>
            1. <b>Data Collection:</b> We collect your name, phone number, and location only during emergencies to provide life-saving services.<br><br>
            2. <b>Location Tracking:</b> Live location is used exclusively for ambulance dispatch and tracking. It is not shared with third parties.<br><br>
            3. <b>Security:</b> Your health data is stored securely in encrypted databases.<br><br>
            4. <b>Consent:</b> By using this app, you consent to our emergency protocols.
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setMessage(Html.fromHtml(policy))
            .setPositiveButton("I Accept") { _, _ -> cbPrivacyPolicy.isChecked = true }
            .setNegativeButton("Close", null)
            .show()
    }
}
