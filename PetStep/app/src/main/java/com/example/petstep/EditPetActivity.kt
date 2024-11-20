package com.example.petstep

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.petstep.databinding.ActivityEditPetBinding
import com.example.petstep.model.MyPet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.UUID

class EditPetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPetBinding
    private lateinit var uri: Uri
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var petRef: DatabaseReference
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(), ActivityResultCallback {
            if (it != null) {
                loadImage(it)
                saveImageUri(it)
                sendImageUri(it)
            }
        }
    )

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
        ActivityResultCallback {
            if (it) {
                loadImage(uri)
                saveImageUri(uri)
                sendImageUri(uri)
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("EditPetActivity", "onCreate called")
        binding = ActivityEditPetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference
        val userId = auth.currentUser?.uid
        val petId = intent.getStringExtra("petId")
        Log.d("EditPetActivity", "Received userId: $userId, petId: $petId")
        if (userId != null && petId != null) {
            petRef = database.getReference("pets").child(petId) // Correct reference to petId
            loadPetData()
        } else {
            Log.e("EditPetActivity", "User ID or Pet ID is null")
            finish() // Close the activity if userId or petId is null
        }

        binding.gallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.takePic.setOnClickListener {
            val file = File(getFilesDir(), "petPic")
            uri = FileProvider.getUriForFile(baseContext, baseContext.packageName + ".fileprovider", file)
            cameraLauncher.launch(uri)
        }

        binding.savePetButton.setOnClickListener {
            if (::uri.isInitialized) {
                sendImageUri(uri)
            } else {
                updatePetProfile()
            }
        }
    }

    private fun loadPetData() {
        Log.d("EditPetActivity", "Loading pet data")
        petRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val pet = snapshot.getValue(MyPet::class.java)
                if (pet != null) {
                    binding.petNameEditText.setText(pet.nombre)
                    binding.petBreedEditText.setText(pet.raza)
                    binding.petAgeEditText.setText(pet.edad)
                    binding.petWeightEditText.setText(pet.peso)
                    if (pet.photoUrl.isNotEmpty()) {
                        val uri = Uri.parse(pet.photoUrl)
                        loadImage(uri)
                    }
                }
            } else {
                Log.e("EditPetActivity", "Pet data does not exist")
            }
        }.addOnFailureListener {
            Log.e("EditPetActivity", "Failed to load pet data", it)
        }
    }

    private fun loadImage(uri: Uri) {
        val imageStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(imageStream)
        binding.petPhotoImageView.setImageBitmap(bitmap)
    }

    private fun saveImageUri(uri: Uri) {
        val sharedPreferences = getSharedPreferences("PetPhotoPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("imageUri", uri.toString())
        editor.apply()
    }

    private fun sendImageUri(uri: Uri) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val photoRef = storageRef.child("pet_photos/${UUID.randomUUID()}.jpg")
            photoRef.putFile(uri).addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    petRef.child("photoUrl").setValue(downloadUri.toString()).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            updatePetProfile(downloadUri.toString())
                        } else {
                            // Handle error
                        }
                    }
                }.addOnFailureListener {
                    // Handle error
                }
            }.addOnFailureListener {
                // Handle error
            }
        } else {
            // Handle error: user not logged in
        }
    }

    private fun updatePetProfile(photoUrl: String? = null) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val petUpdates = mutableMapOf<String, Any>()
            if (binding.petNameEditText.text.isNotEmpty()) petUpdates["nombre"] = binding.petNameEditText.text.toString()
            if (binding.petBreedEditText.text.isNotEmpty()) petUpdates["raza"] = binding.petBreedEditText.text.toString()
            if (binding.petAgeEditText.text.isNotEmpty()) petUpdates["edad"] = binding.petAgeEditText.text.toString()
            if (binding.petWeightEditText.text.isNotEmpty()) petUpdates["peso"] = binding.petWeightEditText.text.toString()
            if (photoUrl != null) petUpdates["photoUrl"] = photoUrl
            petUpdates["userId"] = userId

            if (petUpdates.isNotEmpty()) {
                petRef.updateChildren(petUpdates).addOnCompleteListener {
                    if (it.isSuccessful) {
                        val intent = Intent(this, PetDetailActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Handle error
                    }
                }
            } else {
                val intent = Intent(this, PetDetailActivity::class.java)
                startActivity(intent)
            }
        } else {
            // Handle error: user not logged in
        }
    }
}