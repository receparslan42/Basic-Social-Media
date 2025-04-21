package com.receparslan.basicsocialmedia.views

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.receparslan.basicsocialmedia.R
import com.receparslan.basicsocialmedia.databinding.ActivityRegisterBinding
import com.receparslan.basicsocialmedia.helpers.Internet

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding // ViewBinding

    private lateinit var auth: FirebaseAuth // Firebase Auth

    // EditTexts for name, surname, email and password input
    private lateinit var nameEditText: EditText
    private lateinit var surnameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var secondPasswordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize EditTexts
        nameEditText = binding.nameEditText
        surnameEditText = binding.surnameEditText
        emailEditText = binding.emailEditText
        passwordEditText = binding.passwordEditText
        secondPasswordEditText = binding.secondPasswordEditText

        // Set onClickListeners for buttons
        binding.saveButton.setOnClickListener(::saveUser)

        // Check internet connection for initialize Firebase
        Internet.checkConnection(this)

        // Initialize Firebase Auth
        auth = Firebase.auth
    }

    private fun saveUser(view: View) {
        Internet.checkConnection(this) // Check internet connection for register

        // Get user inputs
        val name = nameEditText.text.toString()
        val surname = surnameEditText.text.toString()
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()
        val secondPassword = secondPasswordEditText.text.toString()

        // Check if the fields are empty
        if (name.isNotEmpty() && surname.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && secondPassword.isNotEmpty()) {
            // Check if the passwords are the same
            if (password == secondPassword) {
                // Register the user
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success
                            val user = auth.currentUser

                            // Update user profile
                            val profileUpdateRequest = UserProfileChangeRequest.Builder()
                                .setDisplayName("$name $surname")
                                .build()
                            user?.let { user.updateProfile(profileUpdateRequest) } // Update user profile

                            // Show a dialog to user to inform that user registered successfully
                            AlertDialog.Builder(this)
                                .setTitle("User Registered")
                                .setMessage("User registered successfully. Please login.")
                                .setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                    finish()
                                }
                                .setCancelable(false)
                                .show()
                        } else {
                            // If sign in fails, display a message to the user.
                            Snackbar.make(view, task.exception?.localizedMessage!!, Snackbar.LENGTH_LONG).show()
                        }
                    }
            } else {
                // Show a toast message to user to inform
                Toast.makeText(this, "Passwords are not the same!", Toast.LENGTH_LONG).show()
            }
        } else {
            // Show a toast message to user to inform
            Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_LONG).show()
        }
    }
}