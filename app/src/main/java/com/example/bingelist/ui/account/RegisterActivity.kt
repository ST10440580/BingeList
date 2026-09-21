package com.example.bingelist.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bingelist.databinding.ActivityRegisterBinding
import com.example.bingelist.ui.discover.DiscoverActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val name = binding.etRegisterName.text.toString().trim()
            val email = binding.etRegisterEmail.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.registerProgressBar.visibility = View.VISIBLE
            binding.btnRegister.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Save Display Name to Firebase Profile
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        auth.currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener {
                            binding.registerProgressBar.visibility = View.GONE
                            binding.btnRegister.isEnabled = true
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DiscoverActivity::class.java))
                            finishAffinity()
                        }
                    } else {
                        binding.registerProgressBar.visibility = View.GONE
                        binding.btnRegister.isEnabled = true
                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage ?: "Registration failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}