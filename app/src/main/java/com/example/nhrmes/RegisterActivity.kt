package com.example.nhrmes

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var registerBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        email = findViewById(R.id.email)
        password = findViewById(R.id.pwd)
        registerBtn = findViewById(R.id.register)

        registerBtn.setOnClickListener {

            val emailText = email.text.toString().trim()
            val pwdText = password.text.toString().trim()

            if (emailText.isEmpty() || pwdText.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(emailText, pwdText)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        val user = auth.currentUser
                        val userId = user!!.uid

                        val database = FirebaseDatabase.getInstance()
                            .getReference("Users")

                        val userMap = HashMap<String, Any>()
                        userMap["email"] = emailText
                        userMap["role"] = "patient"

                        database.child(userId).setValue(userMap)

                        Toast.makeText(this,
                            "Registered Successfully",
                            Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this, MainActivity::class.java))
                        finish()

                    } else {
                        Toast.makeText(this,
                            task.exception?.message,
                            Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
