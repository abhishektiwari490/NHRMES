package com.example.nhrmes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.Executor

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
    private lateinit var btnBiometric: ImageButton
    private lateinit var txtLoginTitle: TextView

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    
    private var selectedRole: String = "patient"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        selectedRole = intent.getStringExtra("SELECTED_ROLE") ?: "patient"

        email = findViewById(R.id.email)
        password = findViewById(R.id.pwd)
        loginBtn = findViewById(R.id.loginbtn)
        googleCustomBtn = findViewById(R.id.googleCustomBtn)
        registerTxt = findViewById(R.id.registerTxt)
        forgotTxt = findViewById(R.id.forgotTxt)
        btnBiometric = findViewById(R.id.btnBiometric)
        txtLoginTitle = findViewById(R.id.loginTitle)

        updateUIForRole()
        setupBiometric()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)!!
                        firebaseAuthWithGoogle(account.idToken!!)
                    } catch (e: ApiException) {
                        Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        loginBtn.setOnClickListener {
            val emailText = email.text.toString().trim()
            val pwdText = password.text.toString().trim()

            if (emailText.isEmpty() || pwdText.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(emailText, pwdText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        verifyRoleAndNavigate()
                    } else {
                        Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                    }
                }
        }

        btnBiometric.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }

        googleCustomBtn.setOnClickListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        registerTxt.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("SELECTED_ROLE", selectedRole)
            startActivity(intent)
        }

        forgotTxt.setOnClickListener {
            startActivity(Intent(this, ForgetPassword::class.java))
        }
    }

    private fun updateUIForRole() {
        txtLoginTitle.text = when (selectedRole) {
            "doctor" -> "Doctor Login"
            "government" -> "Government Official Login"
            else -> "Patient Login"
        }
        
        // Show register text for all roles now
        registerTxt.visibility = View.VISIBLE
        googleCustomBtn.visibility = if (selectedRole == "patient") View.VISIBLE else View.GONE
    }

    private fun verifyRoleAndNavigate() {
        val userId = auth.currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("Users").child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val actualRole = snapshot.child("role").value?.toString() ?: "patient"
                
                if (actualRole == selectedRole || (selectedRole == "doctor" && actualRole == "admin")) {
                    when (actualRole) {
                        "admin" -> startActivity(Intent(this, AdminDashboardActivity::class.java))
                        "doctor" -> startActivity(Intent(this, DoctorDashboardActivity::class.java))
                        "government" -> startActivity(Intent(this, GovernmentDashboardActivity::class.java))
                        else -> startActivity(Intent(this, PatientDashboardActivity::class.java))
                    }
                    finish()
                } else {
                    auth.signOut()
                    Toast.makeText(this, "Access Denied: You are not authorized for this portal", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (auth.currentUser != null) verifyRoleAndNavigate()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser!!
                    val userRef = FirebaseDatabase.getInstance().getReference("Users").child(user.uid)
                    userRef.get().addOnSuccessListener { snapshot ->
                        if (!snapshot.exists()) {
                            val userMap = HashMap<String, Any>()
                            userMap["email"] = user.email ?: ""
                            userMap["role"] = selectedRole
                            userRef.setValue(userMap)
                        }
                        verifyRoleAndNavigate()
                    }
                }
            }
    }
}
