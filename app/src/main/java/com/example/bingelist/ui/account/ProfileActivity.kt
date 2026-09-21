package com.example.bingelist.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.bingelist.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
// ProfileActivity.kt to update user profile and password
class ProfileActivity : AppCompatActivity() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private lateinit var etDisplayName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var btnSaveProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnBack: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Bind Views
        etDisplayName = findViewById(R.id.etDisplayName)
        etEmail = findViewById(R.id.etEmail)
        etNewPassword = findViewById(R.id.etNewPassword)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnLogout = findViewById(R.id.btnLogout)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)

        loadUserData()
        setupListeners()
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        etEmail.setText(user.email ?: "")
        etDisplayName.setText(user.displayName ?: "")
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSaveProfile.setOnClickListener {
            saveChanges()
        }

        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun saveChanges() {
        val user = auth.currentUser ?: return
        val newName = etDisplayName.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()

        progressBar.visibility = View.VISIBLE
        btnSaveProfile.isEnabled = false

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (newPassword.isNotEmpty()) {
                    if (newPassword.length < 6) {
                        progressBar.visibility = View.GONE
                        btnSaveProfile.isEnabled = true
                        Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    user.updatePassword(newPassword).addOnCompleteListener { passTask ->
                        progressBar.visibility = View.GONE
                        btnSaveProfile.isEnabled = true
                        if (passTask.isSuccessful) {
                            etNewPassword.setText("")
                            Toast.makeText(this, "Profile and password updated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to update password: ${passTask.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    progressBar.visibility = View.GONE
                    btnSaveProfile.isEnabled = true
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                }
            } else {
                progressBar.visibility = View.GONE
                btnSaveProfile.isEnabled = true
                Toast.makeText(this, "Failed to update profile: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to log out of BingeList?")
            .setPositiveButton("Log Out") { _, _ ->
                auth.signOut()
                goToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}