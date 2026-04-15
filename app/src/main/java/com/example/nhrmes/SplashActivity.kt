package com.example.nhrmes

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.Executor

class SplashActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logoImage)
        val name = findViewById<TextView>(R.id.appName)
        val tagline = findViewById<TextView>(R.id.tagline)
        val glow = findViewById<View>(R.id.logoGlow)

        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse)
        
        // Start animations
        logo.startAnimation(slideUp)
        name.startAnimation(fadeIn)
        tagline.startAnimation(fadeIn)
        glow.startAnimation(pulse)

        Handler(Looper.getMainLooper()).postDelayed({
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                showBiometricPrompt()
            } else {
                startActivity(Intent(this, RoleSelectionActivity::class.java))
                finish()
            }
        }, 2500)
    }

    private fun showBiometricPrompt() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    checkUserRole()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    checkUserRole()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your fingerprint")
            .setNegativeButtonText("Use Password")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun checkUserRole() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid
        FirebaseDatabase.getInstance().getReference("Users").child(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val role = snapshot.child("role").value?.toString() ?: "patient"
                when (role) {
                    "admin" -> startActivity(Intent(this, AdminDashboardActivity::class.java))
                    "government" -> startActivity(Intent(this, GovernmentDashboardActivity::class.java))
                    else -> startActivity(Intent(this, PatientDashboardActivity::class.java))
                }
                finish()
            }.addOnFailureListener {
                startActivity(Intent(this, RoleSelectionActivity::class.java))
                finish()
            }
    }
}
