package com.example.petstep

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petstep.adapters.RequestsAdapter
import com.example.petstep.databinding.ActivityPeticionesBinding
import com.example.petstep.adapters.com.example.petstep.model.Request
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PeticionesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPeticionesBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val requests = mutableListOf<Request>()
    private val requestsAdapter = RequestsAdapter(requests) { request, action ->
        handleRequestAction(request, action)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPeticionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadPendingRequests()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PeticionesActivity)
            adapter = requestsAdapter
        }
    }

    private fun loadPendingRequests() {
        val walkerId = auth.currentUser!!.uid
        db.collection("requests").whereEqualTo("walkerId", walkerId).whereEqualTo("status", "pending").get()
            .addOnSuccessListener { documents ->
                requests.clear()
                for (doc in documents) {
                    val request = doc.toObject(Request::class.java).copy(id = doc.id)
                    requests.add(request)
                }
                requestsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar peticiones", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleRequestAction(request: Request, action: String) {
        val newStatus = if (action == "accept") "accepted" else "rejected"
        db.collection("requests").document(request.id).update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud $newStatus", Toast.LENGTH_SHORT).show()
                if (action == "accept") navigateToTracking(request.ownerId)
                loadPendingRequests()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar solicitud", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToTracking(ownerId: String) {
        val intent = Intent(this, RastreoActivity::class.java).apply {
            putExtra("walkerId", ownerId)
        }
        startActivity(intent)
    }
}
