package com.example.nhrmes

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgetPassword : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var btnForget: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        auth = FirebaseAuth.getInstance()

        email = findViewById(R.id.email)
        btnForget = findViewById(R.id.btnforget)

        btnForget.setOnClickListener {
            val emailText = email.text.toString().trim()

            if (emailText.isEmpty()) {
                email.error = "Enter registered email"
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(emailText)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this, "Reset email sent", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this, it.exception?.message, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
