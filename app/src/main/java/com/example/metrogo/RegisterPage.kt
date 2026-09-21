package com.example.metrogo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegisterPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val etFirstName = findViewById<EditText>(R.id.etRegFirstName)
        val etSurname = findViewById<EditText>(R.id.etRegSurname)
        val etMobile = findViewById<EditText>(R.id.etRegMobile)
        val etEmail = findViewById<EditText>(R.id.etRegEmail)
        val etPassword = findViewById<EditText>(R.id.etRegPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etRegConfirmPassword)

        val loginToLogin = findViewById<Button>(R.id.LoginToLogin)
        loginToLogin.setOnClickListener {
            startActivity(Intent(this, LoginPage::class.java))
        }

        val signUpThree = findViewById<Button>(R.id.SignUpThree)
        signUpThree.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val surname = etSurname.text.toString().trim()
            val mobile = etMobile.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (firstName.isBlank() || surname.isBlank() || mobile.isBlank() || email.isBlank() ||
                password.isBlank() || confirmPassword.isBlank()
            ) {
                Toast.makeText(this, "Please fill in every field.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords don't match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signUpThree.isEnabled = false   // prevent double taps while the request runs
            UserManager.register(this, email, password, firstName, surname, mobile) { result ->
                when (result) {
                    is UserManager.AuthResult.Success -> {
                        Toast.makeText(this, "Welcome to MetroGO, ${result.user.fullName}!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, Dashboard::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    is UserManager.AuthResult.Failure -> {
                        signUpThree.isEnabled = true
                        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}