package com.example.metrogo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        LoginButton.OnClickListener{LoginButton} leads to LoginPage
        val LoginButton = findViewById<Button>(R.id.LoginButton)
        LoginButton.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)

        val SignUpButton = findViewById<Button>(R.id.SignUpbutton)
            SignUpButton.setOnClickListener {
                val intent = Intent(this, RegisterPage::class.java)
                startActivity(intent)
            }

        }
    }
}