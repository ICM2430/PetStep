package com.example.petstep

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petstep.databinding.ActivityHistorialServiciosBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class HistorialServiciosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialServiciosBinding
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: HistorialServiciosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialServiciosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Determinar si es dueño o paseador
        val isOwner = intent.getBooleanExtra("isOwner", false)

        setupRecyclerView()
        if (isOwner) {
            loadOwnerServiceHistory()
        } else {
            loadWalkerServiceHistory()
        }
    }

    private fun setupRecyclerView() {
        adapter = HistorialServiciosAdapter(mutableListOf())
        binding.recyclerViewHistorial.apply {
            layoutManager = LinearLayoutManager(this@HistorialServiciosActivity)
            adapter = this@HistorialServiciosActivity.adapter
        }
    }

    private fun loadWalkerServiceHistory() {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        database.getReference("walkRequests")
            .orderByChild("walkerId")
            .equalTo(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                processServiceSnapshots(snapshot)
            }
            .addOnFailureError { exception ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar el historial: ${exception.message}", 
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadOwnerServiceHistory() {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        database.getReference("walkRequests")
            .orderByChild("userId")
            .equalTo(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                processServiceSnapshots(snapshot)
            }
            .addOnFailureError { exception ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al cargar el historial: ${exception.message}", 
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun processServiceSnapshots(snapshot: DataSnapshot) {
        val servicios = mutableListOf<ServicioHistorial>()
        var pendingDetailsCount = snapshot.childrenCount.toInt()

        if (pendingDetailsCount == 0) {
            binding.progressBar.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE
            return
        }

        for (servicioSnapshot in snapshot.children) {
            val servicio = servicioSnapshot.getValue(ServicioHistorial::class.java)
            servicio?.let { srv ->
                srv.id = servicioSnapshot.key ?: ""
                loadServicioDetails(srv) { servicioCompleto ->
                    servicios.add(servicioCompleto)
                    pendingDetailsCount--

                    if (pendingDetailsCount == 0) {
                        servicios.sortByDescending { it.timestamp }
                        adapter.updateServicios(servicios)
                        binding.progressBar.visibility = View.GONE
                        binding.emptyStateText.visibility = 
                            if (servicios.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun loadServicioDetails(servicio: ServicioHistorial, callback: (ServicioHistorial) -> Unit) {
        // Cargar información del dueño y del paseador
        val userRef = database.getReference("users")
        
        // Cargar información del dueño
        userRef.child("duenos/${servicio.userId}")
            .get()
            .addOnSuccessListener { ownerSnapshot ->
                servicio.ownerName = ownerSnapshot.child("nombre").getValue(String::class.java) ?: "Desconocido"
                
                // Cargar información del paseador
                userRef.child("paseadores/${servicio.walkerId}")
                    .get()
                    .addOnSuccessListener { walkerSnapshot ->
                        servicio.walkerName = walkerSnapshot.child("nombre").getValue(String::class.java) ?: "Desconocido"
                        
                        // Cargar información de la mascota
                        database.getReference("pets/${servicio.petId}")
                            .get()
                            .addOnSuccessListener { petSnapshot ->
                                servicio.petName = petSnapshot.child("nombre").getValue(String::class.java) ?: "Desconocido"
                                callback(servicio)
                            }
                    }
            }
    }
}

data class ServicioHistorial(
    var id: String = "",
    val distance: Double = 0.0,
    val duration: Int = 0,
    val price: Double = 0.0,
    val status: String = "",
    val timestamp: Long = 0,
    val userId: String = "",
    val petId: String = "",
    var ownerName: String = "",
    var petName: String = "",
    val startTime: Long = 0,
    val endTime: Long = 0,
    var walkerName: String = "",  // Añadir nombre del paseador
    var formattedDate: String = ""  // Añadir fecha formateada
)