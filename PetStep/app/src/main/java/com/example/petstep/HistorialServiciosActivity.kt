package com.example.petstep

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petstep.databinding.ActivityHistorialServiciosBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import java.text.SimpleDateFormat
import java.util.*
import com.example.petstep.adapters.HistorialServiciosAdapter  // Añadir este import

class HistorialServiciosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialServiciosBinding
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: HistorialServiciosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialServiciosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInitialUI()
        checkUserRoleAndLoadHistory()
    }

    private fun setupInitialUI() {
        println("DEBUG: Setting up initial UI")
        binding.recyclerViewHistorial.apply {
            layoutManager = LinearLayoutManager(this@HistorialServiciosActivity).apply {
                initialPrefetchItemCount = 3 // Optimización de carga inicial
            }
            setHasFixedSize(true)
            setItemViewCacheSize(20) // Aumentar cache para mejor rendimiento
            adapter = HistorialServiciosAdapter(ArrayList(20), false).also {
                adapter = it
            }
        }
        println("DEBUG: Initial UI setup complete")
    }

    private fun checkUserRoleAndLoadHistory() {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateText.visibility = View.GONE

        database.getReference("users")
            .child("duenos")
            .child(userId)
            .get()
            .addOnSuccessListener { ownerSnapshot ->
                if (ownerSnapshot.exists()) {
                    handleUserRole(true)
                } else {
                    database.getReference("users")
                        .child("paseadores")
                        .child(userId)
                        .get()
                        .addOnSuccessListener { walkerSnapshot ->
                            if (walkerSnapshot.exists()) {
                                handleUserRole(false)
                            } else {
                                handleUserNotFound()
                            }
                        }
                }
            }
    }

    private fun handleUserRole(isOwner: Boolean) {
        (binding.recyclerViewHistorial.adapter as? HistorialServiciosAdapter)?.apply {
            setIsOwner(isOwner)
            if (isOwner) loadOwnerServiceHistory() else loadWalkerServiceHistory()
        }
    }

    private fun handleUserNotFound() {
        runOnUiThread {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadWalkerServiceHistory() {
        val userId = auth.currentUser?.uid ?: return
        println("DEBUG: Loading walker history for userId: $userId")
        binding.progressBar.visibility = View.VISIBLE

        database.getReference("walkRequests")
            .get()
            .addOnSuccessListener { snapshot ->
                println("DEBUG: All requests snapshot received: ${snapshot.exists()}")
                val filtered = mutableListOf<ServicioHistorial>()
                
                snapshot.children.forEach { child ->
                    if (child.child("walkerId").getValue(String::class.java) == userId) {
                        println("DEBUG: Found matching request: ${child.key}")
                        createServicioFromSnapshot(child)?.let { servicio ->
                            filtered.add(servicio)
                        }
                    }
                }
                
                println("DEBUG: Filtered requests count: ${filtered.size}")
                if (filtered.isEmpty()) {
                    updateUIForEmptyState()
                } else {
                    processServices(filtered)
                }
            }
            .addOnFailureListener { exception ->
                println("ERROR: Failed to load walker history: ${exception.message}")
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar el historial: ${exception.message}", 
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadOwnerServiceHistory() {
        val userId = auth.currentUser?.uid ?: return
        println("DEBUG: Loading owner history for userId: $userId")
        binding.progressBar.visibility = View.VISIBLE

        database.getReference("walkRequests")
            .get()
            .addOnSuccessListener { snapshot ->
                println("DEBUG: All requests snapshot received: ${snapshot.exists()}")
                val filtered = mutableListOf<ServicioHistorial>()
                
                snapshot.children.forEach { child ->
                    if (child.child("userId").getValue(String::class.java) == userId) {
                        println("DEBUG: Found matching request: ${child.key}")
                        createServicioFromSnapshot(child)?.let { servicio ->
                            filtered.add(servicio)
                        }
                    }
                }
                
                println("DEBUG: Filtered requests count: ${filtered.size}")
                if (filtered.isEmpty()) {
                    updateUIForEmptyState()
                } else {
                    processServices(filtered)
                }
            }
            .addOnFailureListener { exception ->
                println("ERROR: Failed to load owner history: ${exception.message}")
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar el historial: ${exception.message}", 
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun processServices(servicios: List<ServicioHistorial>) {
        println("DEBUG: Processing ${servicios.size} services")
        var pendingDetailsCount = servicios.size
        val processedServicios = mutableListOf<ServicioHistorial>()

        servicios.forEach { servicio ->
            loadServicioDetails(servicio) { servicioCompleto ->
                processedServicios.add(servicioCompleto)
                pendingDetailsCount--
                
                if (pendingDetailsCount == 0) {
                    updateUIWithServices(processedServicios)
                }
            }
        }
    }

    private fun processServiceSnapshots(snapshot: DataSnapshot) {
        println("DEBUG: Processing ${snapshot.childrenCount} service snapshots")
        val servicios = ArrayList<ServicioHistorial>(snapshot.childrenCount.toInt())
        var pendingDetailsCount = snapshot.childrenCount.toInt()

        if (pendingDetailsCount == 0) {
            println("DEBUG: No services found")
            updateUIForEmptyState()
            return
        }

        snapshot.children.forEach { servicioSnapshot ->
            println("DEBUG: Processing service: ${servicioSnapshot.key}")
            try {
                createServicioFromSnapshot(servicioSnapshot)?.let { servicio ->
                    println("DEBUG: Created service object: $servicio")
                    loadServicioDetails(servicio) { servicioCompleto ->
                        println("DEBUG: Service details loaded: ${servicioCompleto.id}")
                        println("DEBUG: Owner name: ${servicioCompleto.ownerName}")
                        println("DEBUG: Walker name: ${servicioCompleto.walkerName}")
                        println("DEBUG: Pet name: ${servicioCompleto.petName}")
                        servicios.add(servicioCompleto)
                        pendingDetailsCount--
                        
                        println("DEBUG: Remaining details to load: $pendingDetailsCount")
                        if (pendingDetailsCount == 0) {
                            println("DEBUG: All services loaded, updating UI with ${servicios.size} services")
                            updateUIWithServices(servicios)
                        }
                    }
                } ?: run {
                    println("ERROR: Failed to create service from snapshot")
                    pendingDetailsCount--
                }
            } catch (e: Exception) {
                println("ERROR: Exception processing service: ${e.message}")
                e.printStackTrace()
                pendingDetailsCount--
            }
        }
    }

    private fun updateUIForEmptyState() {
        runOnUiThread {
            binding.progressBar.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE
        }
    }

    private fun updateUIWithServices(servicios: List<ServicioHistorial>) {
        println("DEBUG: Updating UI with ${servicios.size} services")
        runOnUiThread {
            servicios.sortedByDescending { it.timestamp }.let { sortedServicios ->
                println("DEBUG: Sorted services: ${sortedServicios.map { it.id }}")
                (binding.recyclerViewHistorial.adapter as? HistorialServiciosAdapter)?.let { adapter ->
                    println("DEBUG: Adapter found, updating services")
                    adapter.updateServicios(sortedServicios)
                } ?: println("ERROR: Adapter is null")
            }
            binding.progressBar.visibility = View.GONE
            binding.emptyStateText.visibility = if (servicios.isEmpty()) View.VISIBLE else View.GONE
            println("DEBUG: UI update complete")
        }
    }

    private fun createServicioFromSnapshot(servicioSnapshot: DataSnapshot): ServicioHistorial? {
        return try {
            ServicioHistorial(
                id = servicioSnapshot.key ?: "",
                distance = servicioSnapshot.child("distance").getValue(Double::class.java) ?: 0.0,
                duration = servicioSnapshot.child("duration").getValue(Int::class.java) ?: 0,
                endTime = servicioSnapshot.child("endTime").getValue(Long::class.java) ?: 0L,
                startTime = servicioSnapshot.child("startTime").getValue(Long::class.java) ?: 0L,
                ownerLat = servicioSnapshot.child("ownerLat").getValue(Double::class.java) ?: 0.0,
                ownerLng = servicioSnapshot.child("ownerLng").getValue(Double::class.java) ?: 0.0,
                petId = servicioSnapshot.child("petId").getValue(String::class.java) ?: "",
                price = servicioSnapshot.child("price").getValue(Double::class.java) ?: 0.0,
                status = servicioSnapshot.child("status").getValue(String::class.java) ?: "",
                timestamp = servicioSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L,
                userId = servicioSnapshot.child("userId").getValue(String::class.java) ?: "",
                walkerId = servicioSnapshot.child("walkerId").getValue(String::class.java) ?: ""
            )
        } catch (e: Exception) {
            println("ERROR: Failed to create service from snapshot: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun loadServicioDetails(servicio: ServicioHistorial, callback: (ServicioHistorial) -> Unit) {
        println("DEBUG: Loading details for service ${servicio.id}")
        
        // Cargar información del dueño
        database.getReference("users/duenos/${servicio.userId}")
            .get()
            .addOnSuccessListener { ownerSnapshot ->
                println("DEBUG: Owner data loaded for service ${servicio.id}")
                servicio.ownerName = ownerSnapshot.child("nombre").getValue(String::class.java) ?: "Desconocido"
                
                // Cargar información del paseador
                database.getReference("users/paseadores/${servicio.walkerId}")
                    .get()
                    .addOnSuccessListener { walkerSnapshot ->
                        println("DEBUG: Walker data loaded for service ${servicio.id}")
                        servicio.walkerName = walkerSnapshot.child("nombre").getValue(String::class.java) ?: "Desconocido"
                        
                        // Cargar información de la mascota
                        database.getReference("pets/${servicio.petId}")
                            .get()
                            .addOnSuccessListener { petSnapshot ->
                                println("DEBUG: Pet data loaded for service ${servicio.id}")
                                servicio.petName = petSnapshot.child("nombre").getValue(String::class.java) ?: "Desconocido"
                                callback(servicio)
                            }
                            .addOnFailureListener { e ->
                                println("ERROR: Failed to load pet data: ${e.message}")
                                callback(servicio)
                            }
                    }
                    .addOnFailureListener { e ->
                        println("ERROR: Failed to load walker data: ${e.message}")
                        callback(servicio)
                    }
            }
            .addOnFailureListener { e ->
                println("ERROR: Failed to load owner data: ${e.message}")
                callback(servicio)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpiar referencias
        binding.recyclerViewHistorial.adapter = null
    }
}

data class ServicioHistorial(
    var id: String = "",
    val distance: Double = 0.0,
    val duration: Int = 0,
    val endTime: Long = 0,
    val startTime: Long = 0,
    val ownerLat: Double = 0.0,
    val ownerLng: Double = 0.0,
    val petId: String = "",
    val price: Double = 0.0,
    val status: String = "",
    val timestamp: Long = 0,
    val userId: String = "",
    val walkerId: String = "",
    // Campos adicionales para UI
    var ownerName: String = "",
    var walkerName: String = "",
    var petName: String = ""
)