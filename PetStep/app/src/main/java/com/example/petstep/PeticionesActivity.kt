package com.example.petstep

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petstep.adapters.RequestsAdapter
import com.example.petstep.adapters.com.example.petstep.model.Request
import com.example.petstep.databinding.ActivityPeticionesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase  // Cambiar a Realtime Database
import com.google.firebase.database.DataSnapshot
import androidx.recyclerview.widget.DividerItemDecoration  // Añadir este import
import androidx.recyclerview.widget.RecyclerView  // Añadir este import

class PeticionesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPeticionesBinding
    private val database = FirebaseDatabase.getInstance()  // Cambiar a Realtime Database
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
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        
        // Corrección de la sintaxis del observer
        val dataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                println("DEBUG: Adapter notified of data change")
                println("DEBUG: Current adapter items: ${requests.size}")
            }
        }
        requestsAdapter.registerAdapterDataObserver(dataObserver)
    }

    private fun loadPendingRequests() {
        val walkerId = auth.currentUser!!.uid
        println("DEBUG: Buscando peticiones para walkerId: $walkerId")

        database.getReference("requests")
            .get()
            .addOnSuccessListener { snapshot ->
                println("DEBUG: Snapshot recibido: ${snapshot.exists()}")
                
                val newRequests = mutableListOf<Request>()  // Crear nueva lista temporal
                
                for (requestSnapshot in snapshot.children) {
                    try {
                        println("DEBUG: Processing snapshot: ${requestSnapshot.key}")
                        val request = requestSnapshot.getValue(Request::class.java)
                        println("DEBUG: Converted to Request object: $request")
                        
                        request?.let {
                            it.id = requestSnapshot.key ?: ""
                            if (it.walkerId == walkerId && it.status == "pending") {
                                newRequests.add(it)  // Añadir a la lista temporal
                                println("DEBUG: Added to new requests list: ${it.id}")
                            }
                        }
                    } catch (e: Exception) {
                        println("ERROR: Error al convertir request: ${e.message}")
                        e.printStackTrace()
                    }
                }
                
                // Actualizar la lista principal y notificar en el hilo principal
                runOnUiThread {
                    requests.clear()
                    requests.addAll(newRequests)
                    requestsAdapter.notifyDataSetChanged()
                    println("DEBUG: Adapter notified with ${requests.size} items")
                    
                    // Mostrar mensaje si no hay peticiones
                    if (requests.isEmpty()) {
                        Toast.makeText(this, "No hay peticiones pendientes", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { exception ->
                println("ERROR: Fallo al cargar peticiones: ${exception.message}")
                exception.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error al cargar peticiones: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleRequestAction(request: Request, action: String) {
        val newStatus = if (action == "accept") "accepted" else "rejected"
        database.getReference("requests")
            .child(request.id)
            .child("status")
            .setValue(newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud $newStatus", Toast.LENGTH_SHORT).show()
                if (action == "accept") {
                    val intent = Intent(this, MapsActivityPaseador::class.java).apply {
                        putExtra("requestId", request.id)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    loadPendingRequests()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar solicitud", Toast.LENGTH_SHORT).show()
            }
    }
}
