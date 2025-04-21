package com.receparslan.basicsocialmedia.views

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.receparslan.basicsocialmedia.R
import com.receparslan.basicsocialmedia.databinding.ActivityLoginBinding
import com.receparslan.basicsocialmedia.helpers.Internet

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding // ViewBinding

    private lateinit var auth: FirebaseAuth // Firebase Auth

    // EditTexts for email and password input
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize EditTexts
        emailEditText = binding.emailEditText
        passwordEditText = binding.passwordEditText

        // Set onClickListeners for buttons
        binding.loginButton.setOnClickListener { login() }
        binding.registerButton.setOnClickListener { register() }

        // Check internet connection for initialize Firebase
        Internet.checkConnection(this)

        // Initialize Firebase Auth
        auth = Firebase.auth
        auth.signOut()
    }

    // Login function
    private fun login() {
        // Check internet connection for login
        Internet.checkConnection(this)

        // Get email and password from EditTexts
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()

        // Check if email and password are not empty
        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, redirect user to home page
                        loginIntent()
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(baseContext, "User not found. Please check your e-mail and password!", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            // If email or password is empty, display a message to the user.
            Toast.makeText(baseContext, "Please fill all fields!", Toast.LENGTH_LONG).show()
        }
    }

    // Register function to redirect user to register page
    private fun register() {
        // Check internet connection for register
        Internet.checkConnection(this)

        // Intent for redirect user to the register page
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun loginIntent() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}