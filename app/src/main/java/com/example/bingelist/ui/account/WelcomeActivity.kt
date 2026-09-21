package com.example.bingelist.ui.account

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bingelist.databinding.ActivityWelcomeBinding
import com.example.bingelist.ui.discover.DiscoverActivity
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Automatically bypass welcome if user is already authenticated
        if (auth.currentUser != null) {
            startActivity(Intent(this, DiscoverActivity::class.java))
            finish()
            return
        }

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navigate to Register Screen
        binding.btnWelcomeRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Navigate to Login Screen
        binding.btnWelcomeLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Continue without signing in (direct to Discover)
        binding.tvContinueWithoutAccount.setOnClickListener {
            startActivity(Intent(this, DiscoverActivity::class.java))
            finish()
        }
    }
}