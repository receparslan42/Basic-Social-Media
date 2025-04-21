package com.receparslan.basicsocialmedia.views

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.receparslan.basicsocialmedia.R
import com.receparslan.basicsocialmedia.databinding.ActivityPostBinding
import com.receparslan.basicsocialmedia.helpers.Internet
import com.receparslan.basicsocialmedia.models.Post
import java.util.UUID

class PostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostBinding // ViewBinding

    // Firebase
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var storageReference: StorageReference
    private lateinit var user: FirebaseUser

    // Views
    private lateinit var selectedImageView: ImageView
    private lateinit var commentEditText: EditText

    // Initialize Post object to store post data
    private var post = Post()

    // Activity Result Launcher for selecting image from gallery
    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                data.data?.let { imageUri ->
                    selectedImageView.setImageURI(imageUri)
                    post.imageUri = imageUri
                }
            }
        }
    }

    // Request Permission Launcher for gallery permission
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission granted
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        } else {
            // Permission denied show a toast message to user to inform
            Toast.makeText(this, "Permission is required to access the gallery.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        selectedImageView = binding.selectedImageView
        commentEditText = binding.commentEditText

        // Set click listeners for views
        selectedImageView.setOnClickListener(::selectImageFromGallery)
        binding.uploadButton.setOnClickListener(::uploadPost)

        Internet.checkConnection(this) // Check internet connection

        // Initialize Firebase
        firebaseFirestore = FirebaseFirestore.getInstance()
        storageReference = FirebaseStorage.getInstance().reference
        Firebase.auth.currentUser?.let { user = it }
    }

    // Function to select image from gallery
    private fun selectImageFromGallery(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requestPermission(view, android.Manifest.permission.READ_MEDIA_IMAGES)
        else
            requestPermission(view, android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun uploadPost(view: View) {
        // Check internet connection before uploading post
        Internet.checkConnection(this)

        // Set comment to post object
        post.comment = commentEditText.text.toString()

        // Check if image is selected
        if (post.imageUri == null && post.comment.isNullOrEmpty()) {
            Snackbar.make(view, "Please select an image or write a comment!", Snackbar.LENGTH_LONG).setAction("Ok", null).show()
            return
        }

        // Create a map to store post data
        val postData = HashMap<String, Any>()
        user.displayName?.let { postData["displayName"] = it }
        user.email?.let { postData["email"] = it }
        post.comment?.let { postData["comment"] = it }
        postData["date"] = FieldValue.serverTimestamp()

        // Check if image is selected
        post.imageUri?.let {
            // Set image name to post object as UUID
            val imagePath = "images/" + UUID.randomUUID() + ".jpg"

            // Upload image to Firebase Storage
            storageReference.child(imagePath).putFile(post.imageUri!!).addOnSuccessListener {

                storageReference.child(imagePath).downloadUrl.addOnSuccessListener { uri ->
                    postData["imageUri"] = uri.toString() // Set image URL to post data

                    // Save post data to Firestore with image URL
                    firebaseFirestore.collection("Posts").add(postData).addOnSuccessListener {
                        Toast.makeText(this, "Post uploaded successfully.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to upload post.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to get download URL.", Toast.LENGTH_SHORT).show()
                }

            }.addOnFailureListener {
                Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            // Save post data to Firestore with no image URL
            firebaseFirestore.collection("Posts").add(postData).addOnSuccessListener {
                Toast.makeText(this, "Post uploaded successfully.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to upload post.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to request permission for gallery
    private fun requestPermission(view: View, permission: String) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        } else if (shouldShowRequestPermissionRationale(permission)) {
            // Show rationale to user
            Snackbar.make(view, "Permission is required to access the gallery.", Snackbar.LENGTH_LONG).setAction("OK") {
                requestPermissionLauncher.launch(permission)
            }.show()
        } else {
            // Permission denied, request permission
            requestPermissionLauncher.launch(permission)
        }
    }
}
