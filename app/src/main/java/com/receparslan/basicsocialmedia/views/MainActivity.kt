package com.receparslan.basicsocialmedia.views

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.receparslan.basicsocialmedia.R
import com.receparslan.basicsocialmedia.adapter.RecyclerAdapter
import com.receparslan.basicsocialmedia.databinding.ActivityMainBinding
import com.receparslan.basicsocialmedia.helpers.Internet
import com.receparslan.basicsocialmedia.models.Post

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // ViewBinding

    private val postArraylist = ArrayList<Post>() // ArrayList<Post> for Post model

    private lateinit var recyclerView: RecyclerView // RecyclerView for Post

    // ExtendedFloatingActionButton for UI
    private lateinit var moreEFAB: ExtendedFloatingActionButton
    private lateinit var logoutEFAB: ExtendedFloatingActionButton
    private lateinit var deleteAccountEFAB: ExtendedFloatingActionButton
    private lateinit var addPostEFAB: ExtendedFloatingActionButton

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var user: FirebaseUser
    private lateinit var firebaseFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize the views
        moreEFAB = binding.moreEFAB
        logoutEFAB = binding.logoutEFAB
        deleteAccountEFAB = binding.deleteAccountEFAB
        addPostEFAB = binding.addPostEFAB

        // Set the layout for the first time
        moreEFAB.shrink()
        logoutEFAB.hide()
        logoutEFAB.shrink()
        deleteAccountEFAB.hide()
        deleteAccountEFAB.shrink()
        addPostEFAB.hide()
        addPostEFAB.shrink()

        // Set the click listeners for the ExtendedFloatingActionButton
        addPostEFAB.setOnClickListener { addPost() }
        deleteAccountEFAB.setOnClickListener { deleteAccount() }
        logoutEFAB.setOnClickListener { logout() }
        moreEFAB.setOnClickListener { setMoreEFAB() }

        Internet.checkConnection(this) // Check internet connection

        // Firebase
        auth = Firebase.auth
        firebaseFirestore = Firebase.firestore

        // Check if the user is logged in
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            user = auth.currentUser!! // Get current user

            getData() // Get data from Firestore

            recyclerView = binding.recyclerView
            recyclerView.adapter = RecyclerAdapter(postArraylist)
            recyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    // Function to add post
    private fun addPost() {
        val intent = Intent(this, PostActivity::class.java)
        startActivity(intent)
    }

    // Function to delete account
    private fun deleteAccount() {
        // Create an EditText to get the password from the user for re-authentication
        val passwordEditText = EditText(this)
        passwordEditText.hint = "Confirm Password"
        passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordEditText.textAlignment = EditText.TEXT_ALIGNMENT_CENTER

        // Create an AlertDialog to confirm the deletion of the account
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account?")
            .setView(passwordEditText)
            .setPositiveButton("Yes") { dialog, _ ->
                // Check if the password is correct and re-authenticate the user
                user.reauthenticate(EmailAuthProvider.getCredential(user.email!!, passwordEditText.text.toString())).addOnSuccessListener {
                    // Delete the posts from Firestore and the images from Firebase Storage
                    firebaseFirestore.collection("Posts").whereEqualTo("email", user.email).get().addOnCompleteListener {
                        if (it.isSuccessful) {
                            val totalPosts = it.result?.size() ?: 0
                            var deletedPost = 0

                            // Delete the posts from Firebase Storage
                            val reference = FirebaseStorage.getInstance().reference

                            for (documentSnapshot in it.result.documents) {
                                val imageUri = documentSnapshot.getString("imageUri")
                                // Check the imageUri for the post has a image
                                imageUri?.let {
                                    val path = "images/${imageUri.substring(imageUri.indexOf("%2F") + 3, imageUri.indexOf("?"))}"
                                    reference.child(path).delete().addOnSuccessListener {
                                        documentSnapshot.reference.delete().addOnSuccessListener {
                                            deletedPost++

                                            if (deletedPost == totalPosts) {
                                                // Delete the user from Firestore
                                                user.delete().addOnSuccessListener {
                                                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_LONG).show()
                                                    val intent = Intent(this, LoginActivity::class.java)
                                                    startActivity(intent)
                                                    finish()
                                                }
                                            }
                                        }
                                    }
                                } ?: run {
                                    documentSnapshot.reference.delete().addOnSuccessListener {
                                        deletedPost++

                                        if (deletedPost == totalPosts) {
                                            // Delete the user from Firestore
                                            user.delete().addOnSuccessListener {
                                                Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_LONG).show()
                                                val intent = Intent(this, LoginActivity::class.java)
                                                startActivity(intent)
                                                finish()
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                }.addOnFailureListener {
                    // Show a toast message if the password is incorrect
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    // Function to logout
    private fun logout() {
        auth.signOut() // Sign out the user

        // Redirect user to the login page
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Function to set moreEFAB
    private fun setMoreEFAB() {
        if (moreEFAB.isExtended) {
            moreEFAB.shrink()
            moreEFAB.icon = AppCompatResources.getDrawable(this, android.R.drawable.ic_input_add)
            logoutEFAB.hide()
            logoutEFAB.shrink()
            deleteAccountEFAB.hide()
            deleteAccountEFAB.shrink()
            addPostEFAB.hide()
            addPostEFAB.shrink()
        } else {
            moreEFAB.extend()
            moreEFAB.icon = AppCompatResources.getDrawable(this, android.R.drawable.ic_delete)
            logoutEFAB.show()
            logoutEFAB.extend()
            deleteAccountEFAB.show()
            deleteAccountEFAB.extend()
            addPostEFAB.show()
            addPostEFAB.extend()
        }
    }

    // Function to get data from Firestore
    private fun getData() {
        firebaseFirestore.collection("Posts").orderBy("date", Query.Direction.DESCENDING).addSnapshotListener { value, error ->
            if (error == null && value != null) {
                for (documentSnapshot in value.documents) {
                    val data = documentSnapshot.data

                    data?.let { it ->
                        val post = Post()
                        post.displayName = it["displayName"].toString()
                        post.email = it["email"].toString()
                        post.comment = it["comment"].toString()
                        post.imageUri = it["imageUri"].toString().toUri()

                        // Convert Timestamp to String
                        it["date"]?.let {
                            val ts = it as Timestamp
                            post.date = java.sql.Timestamp(ts.toDate().time).toString().split(".")[0]
                        }

                        postArraylist.add(post)
                        recyclerView.adapter?.notifyItemInserted(postArraylist.size - 1)
                    }
                }
            }
        }
    }
}