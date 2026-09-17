package com.example.bingelist.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bingelist.R
import com.example.bingelist.ui.discover.DiscoverActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etFirstName = findViewById<EditText>(R.id.etRegisterFirstName)
        val etSurname = findViewById<EditText>(R.id.etRegisterSurname)
        val etPhone = findViewById<EditText>(R.id.etRegisterPhone)
        val etEmail = findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val progressBar = findViewById<ProgressBar>(R.id.registerProgressBar)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val surname = etSurname.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (firstName.isEmpty() || surname.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId == null) {
                            progressBar.visibility = View.GONE
                            btnRegister.isEnabled = true
                            Toast.makeText(this, "Failed to retrieve user ID", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }

                        // Prepare the user profile data to save in Cloud Firestore
                        val userProfile = hashMapOf(
                            "firstName" to firstName,
                            "surname" to surname,
                            "phone" to phone,
                            "email" to email,
                            "fullName" to "$firstName $surname"
                        )

                        // Save under "users/{userId}"
                        db.collection("users").document(userId).set(userProfile)
                            .addOnSuccessListener {
                                progressBar.visibility = View.GONE
                                btnRegister.isEnabled = true
                                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

                                val intent = Intent(this, DiscoverActivity::class.java)
                                startActivity(intent)
                                finishAffinity()
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                btnRegister.isEnabled = true
                                Toast.makeText(
                                    this,
                                    "Account created, but failed to save profile: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Still proceed to home if auth succeeded
                                startActivity(Intent(this, DiscoverActivity::class.java))
                                finishAffinity()
                            }
                    } else {
                        progressBar.visibility = View.GONE
                        btnRegister.isEnabled = true
                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage ?: "Registration failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}