package com.example.petstep

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petstep.adapters.WalkersAdapter
import com.example.petstep.databinding.ActivityPaseoBinding
import com.example.petstep.com.example.petstep.model.Walker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PaseoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityPaseoBinding
    private lateinit var googleMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()
    private val walkers = mutableListOf<Walker>()
    private val walkersAdapter = WalkersAdapter(walkers) { walker -> sendRequestToWalker(walker) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaseoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadAvailableWalkers()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PaseoActivity)
            adapter = walkersAdapter
        }
    }

    private fun loadAvailableWalkers() {
        db.collection("walkers").whereEqualTo("available", true).get()
            .addOnSuccessListener { documents ->
                walkers.clear()
                for (doc in documents) {
                    val walker = doc.toObject(Walker::class.java).apply { id = doc.id }
                    walkers.add(walker)
                    addWalkerMarkerToMap(walker)
                }
                if (walkers.isEmpty()) {
                    Toast.makeText(this, "No hay paseadores disponibles", Toast.LENGTH_SHORT).show()
                }
                walkersAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar paseadores disponibles", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addWalkerMarkerToMap(walker: Walker) {
        val location = LatLng(walker.latitude, walker.longitude)
        googleMap.addMarker(MarkerOptions().position(location).title("Paseador: ${walker.nombre}"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))
    }

    private fun sendRequestToWalker(walker: Walker) {
        val request = hashMapOf(
            "ownerId" to FirebaseAuth.getInstance().currentUser!!.uid,
            "walkerId" to walker.id,
            "status" to "pending"
        )
        db.collection("requests").add(request)
            .addOnSuccessListener {
                Toast.makeText(this, "Solicitud enviada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al enviar solicitud", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
    }
}
