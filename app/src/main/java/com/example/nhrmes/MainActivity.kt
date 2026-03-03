package com.example.nhrmes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginBtn: Button
    private lateinit var googleCustomBtn: Button
    private lateinit var registerTxt: TextView
    private lateinit var forgotTxt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Firebase
        auth = FirebaseAuth.getInstance()

        // Views
        email = findViewById(R.id.email)
        password = findViewById(R.id.pwd)
        loginBtn = findViewById(R.id.loginbtn)
        googleCustomBtn = findViewById(R.id.googleCustomBtn)
        registerTxt = findViewById(R.id.registerTxt)
        forgotTxt = findViewById(R.id.forgotTxt)

        // Google Sign-In configuration
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Toast.makeText(this, "Google Sign-In Failed: " + e.statusCode, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Email/Password Login
        loginBtn.setOnClickListener {
            val emailText = email.text.toString().trim()
            val pwdText = password.text.toString().trim()

            if (emailText.isEmpty() || pwdText.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(emailText, pwdText)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                        goToDashboard()
                    } else {
                        Toast.makeText(
                            this,
                            it.exception?.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Google Sign-In (CUSTOM BUTTON)
        googleCustomBtn.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        // Register
        registerTxt.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Forgot Password
        forgotTxt.setOnClickListener {
            startActivity(Intent(this, ForgetPassword::class.java))
        }

        addSampleHospitals()
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Google Login Successful",
                        Toast.LENGTH_SHORT
                    ).show()
                    goToDashboard()
                } else {
                    Toast.makeText(
                        this,
                        "Authentication Failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, PatientDashboardActivity::class.java))
        finish()
    }

    private fun addSampleHospitals() {
        val db = FirebaseDatabase.getInstance().getReference("hospitals")
        val hospitals = listOf(
            Hospital("City General Hospital", 5.2, 20, 15, true, "9876543210"),
            Hospital("Green Valley Medical Center", 12.8, 10, 8, false, "1234567890"),
            Hospital("St. Luke's Hospital", 8.5, 30, 25, true, "0987654321"),
            Hospital("Community Hospital", 3.1, 15, 12, true, "1122334455"),
            Hospital("Hope Medical Clinic", 15.2, 5, 4, false, "5566778899")
        )

        hospitals.forEach { hospital ->
            db.push().setValue(hospital)
        }
    }
}
