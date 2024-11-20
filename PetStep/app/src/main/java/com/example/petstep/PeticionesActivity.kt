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
    // Cambiar el tipo de la lista requests
    private val requests = mutableListOf<RequestsAdapter.RequestWithDetails>()
    private val requestsAdapter = RequestsAdapter(requests) { request, action ->
        // Cambiar para usar el request interno
        handleRequestAction(request.request, action)
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

    private fun loadRequestDetails(request: Request, callback: (RequestsAdapter.RequestWithDetails) -> Unit) {
        val requestWithDetails = RequestsAdapter.RequestWithDetails(request)
        
        // Corregir la ruta para incluir "users/duenos"
        database.getReference("users/duenos").child(request.userId)
            .get()
            .addOnSuccessListener { ownerSnapshot ->
                println("DEBUG: Owner snapshot: ${ownerSnapshot.exists()}")
                requestWithDetails.ownerName = ownerSnapshot.child("nombre").getValue(String::class.java) ?: "Desconocido"
                println("DEBUG: Owner name loaded: ${requestWithDetails.ownerName}")
                
                // Cargar información de la mascota
                database.getReference("pets").child(request.petId)
                    .get()
                    .addOnSuccessListener { petSnapshot ->
                        requestWithDetails.petName = petSnapshot.child("nombre").getValue(String::class.java) ?: "Desconocido"
                        requestWithDetails.petAge = petSnapshot.child("edad").getValue(String::class.java) ?: "?"
                        requestWithDetails.petWeight = petSnapshot.child("peso").getValue(String::class.java) ?: "?"
                        
                        println("DEBUG: Pet details loaded - Name: ${requestWithDetails.petName}")
                        callback(requestWithDetails)
                    }
            }
    }

    private fun loadPendingRequests() {
        val walkerId = auth.currentUser!!.uid
        println("DEBUG: Buscando peticiones para walkerId: $walkerId")

        // Cambiar "requests" por "walkRequests"
        database.getReference("walkRequests")
            .get()
            .addOnSuccessListener { snapshot ->
                println("DEBUG: Snapshot recibido: ${snapshot.exists()}")
                
                val newRequests = mutableListOf<RequestsAdapter.RequestWithDetails>()
                var pendingDetailsCount = 0
                
                for (requestSnapshot in snapshot.children) {
                    try {
                        println("DEBUG: Raw distance value: ${requestSnapshot.child("distance").getValue()}")
                        val request = requestSnapshot.getValue(Request::class.java)
                        println("DEBUG: Converted distance value: ${request?.distance}")
                        println("DEBUG: Processing snapshot: ${requestSnapshot.key}")
                        println("DEBUG: Converted to Request object: $request")
                        
                        request?.let {
                            it.id = requestSnapshot.key ?: ""
                            if (it.walkerId == walkerId && it.status == "pending") {
                                pendingDetailsCount++
                                loadRequestDetails(it) { requestWithDetails ->
                                    newRequests.add(requestWithDetails)
                                    pendingDetailsCount--
                                    
                                    if (pendingDetailsCount == 0) {
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
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("ERROR: Error al convertir request: ${e.message}")
                        e.printStackTrace()
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
        
        if (action == "accept") {
            // Actualizar tanto el estado de la solicitud como el estado del paseador
            val updates = hashMapOf<String, Any>(
                "walkRequests/${request.id}/status" to newStatus,
                // Corrección de la ruta para el paseador
                "users/paseadores/${auth.currentUser!!.uid}/activeServiceId" to request.id,
                "users/paseadores/${auth.currentUser!!.uid}/activeService" to true
            )

            database.reference.updateChildren(updates)
                .addOnSuccessListener {
                    println("DEBUG: Actualizando campos del paseador ${auth.currentUser!!.uid}")
                    Toast.makeText(this, "Solicitud aceptada", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MapsActivityPaseador::class.java).apply {
                        putExtra("requestId", request.id)
                    }
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    println("ERROR: Fallo al actualizar campos del paseador: ${e.message}")
                    Toast.makeText(this, "Error al actualizar solicitud", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Para rechazos, solo actualizar el estado de la solicitud
            database.getReference("walkRequests")
                .child(request.id)
                .child("status")
                .setValue(newStatus)
                .addOnSuccessListener {
                    Toast.makeText(this, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
                    loadPendingRequests()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al actualizar solicitud", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
