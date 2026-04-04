package com.example.nhrmes

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        findViewById<CardView>(R.id.cardPatient).setOnClickListener {
            navigateToLogin("patient")
        }

        findViewById<CardView>(R.id.cardDoctor).setOnClickListener {
            navigateToLogin("doctor")
        }

        findViewById<CardView>(R.id.cardGov).setOnClickListener {
            navigateToLogin("government")
        }
    }

    private fun navigateToLogin(role: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("SELECTED_ROLE", role)
        startActivity(intent)
    }
}
