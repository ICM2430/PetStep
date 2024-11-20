package com.example.petstep

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityCalificarBinding
import com.google.firebase.database.FirebaseDatabase
import com.example.petstep.model.ReviewWalker
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class CalificarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalificarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalificarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.enviarResena.setOnClickListener {
            val walkerId = binding.editTextWalkerId.text.toString()
            val reviewText = binding.editTextResena.text.toString()
            val rating = binding.ratingBar.rating.toInt()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            val review = ReviewWalker(
                UUID.randomUUID().toString(),
                walkerId,
                userId,
                reviewText,
                rating,
                Date()
            )

            val database = FirebaseDatabase.getInstance()
            val reviewsRef = database.getReference("reviews").child(walkerId).push()
            reviewsRef.setValue(review).addOnCompleteListener {
                if (it.isSuccessful) {
                    updateWalkerRating(walkerId)
                }
            }
        }
    }

    private fun updateWalkerRating(walkerId: String) {
        val database = FirebaseDatabase.getInstance()
        val reviewsRef = database.getReference("reviews").child(walkerId)
        reviewsRef.get().addOnSuccessListener { dataSnapshot ->
            var totalRating = 0
            var count = 0
            for (reviewSnapshot in dataSnapshot.children) {
                val rating = reviewSnapshot.child("calification").getValue(Int::class.java) ?: 0
                totalRating += rating
                count++
            }
            val averageRating = if (count > 0) totalRating / count else 0
            val walkerRef = database.getReference("users/paseadores").child(walkerId)
            walkerRef.child("calificacion").setValue(averageRating)
        }
    }
}