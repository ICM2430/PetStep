// AddPetActivity.kt
package com.example.petstep

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.petstep.databinding.ActivityAddPetBinding
import com.example.petstep.model.MyPet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.*

class AddPetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPetBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private var photoUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { uri ->
                binding.petPhotoImageView.setImageURI(uri)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            photoUri = it
            binding.petPhotoImageView.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AddPetActivity", "onCreate called")
        binding = ActivityAddPetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        database = FirebaseDatabase.getInstance().getReference("pets")
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        binding.takePic.setOnClickListener { openCamera() }
        binding.gallery.setOnClickListener { openGallery() }
        binding.savePetButton.setOnClickListener { savePet() }
    }

    private fun openCamera() {
        try {
            val file = File(filesDir, "petPhoto.jpg")
            if (file.exists() || file.createNewFile()) {
                val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                photoUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(this, "Failed to create file for the camera", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePet() {
        Log.d("AddPetActivity", "Saving pet")
        val petName = binding.petNameEditText.text.toString().trim()
        val petBreed = binding.petBreedEditText.text.toString().trim()
        val petAge = binding.petAgeEditText.text.toString().trim()
        val petWeight = binding.petWeightEditText.text.toString().trim()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.e("AddPetActivity", "User not authenticated")
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (petName.isNotEmpty() && petBreed.isNotEmpty() && petAge.isNotEmpty() && petWeight.isNotEmpty() && photoUri != null) {
            val photoRef = storageReference.child("pet_photos/${UUID.randomUUID()}.jpg")
            photoRef.putFile(photoUri!!).addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { uri ->
                    val petId = database.push().key ?: return@addOnSuccessListener
                    val pet = MyPet(petId, petName, petBreed, petAge, petWeight, userId, uri.toString())
                    database.child(petId).setValue(pet).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("AddPetActivity", "Pet added successfully")
                            Toast.makeText(this, "Pet added successfully", Toast.LENGTH_SHORT).show()
                            updatePetsList()
                            startActivity(Intent(baseContext, MascotasActivity::class.java))
                            finish()
                        } else {
                            Log.e("AddPetActivity", "Failed to add pet")
                            Toast.makeText(this, "Failed to add pet", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }.addOnFailureListener {
                Log.e("AddPetActivity", "Failed to upload photo", it)
                Toast.makeText(this, "Failed to upload photo", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("AddPetActivity", "Please fill all fields and select a photo")
            Toast.makeText(this, "Please fill all fields and select a photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePetsList() {
        val intent = Intent(this, MascotasActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}