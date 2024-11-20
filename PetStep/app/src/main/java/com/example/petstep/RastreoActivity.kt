package com.example.petstep

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityRastreoBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.FirebaseDatabase
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import com.google.maps.android.PolyUtil
import android.graphics.Color
import android.util.Log
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline
import android.widget.Toast
import android.view.View
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class RastreoActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityRastreoBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var elevationChart: LineChart
    private var geoApiContext: GeoApiContext? = null
    private val database = FirebaseDatabase.getInstance()
    private var walkerMarker: Marker? = null
    private var petMarker: Marker? = null
    private val routePoints = mutableListOf<LatLng>()
    private lateinit var polyline: Polyline
    private var serviceStatus = ""
    private var walkerId = ""
    private val mainHandler = Handler(Looper.getMainLooper())
    private val deferredInitializer = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRastreoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        walkerId = intent.getStringExtra("walkerId") ?: ""
        
        if (walkerId.isEmpty()) {
            Log.e(TAG, "No se recibió walkerId")
            Toast.makeText(this, "Error: No se recibió ID del paseador", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Log.d(TAG, "⭐ onCreate - WalkerId recibido: $walkerId")

        // Primero hacer una consulta simple para verificar la conexión
        database.getReference("walkRequests")
            .orderByChild("walkerId")
            .equalTo(walkerId)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "🔵 Consulta inicial - documentos encontrados: ${snapshot.childrenCount}")
                snapshot.children.forEach { child ->
                    Log.d(TAG, "📄 Documento encontrado - ID: ${child.key}")
                    Log.d(TAG, "📄 Status: ${child.child("status").value}")
                    Log.d(TAG, "📄 Data: ${child.value}")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error en consulta inicial: ${e.message}")
                Toast.makeText(this, "Error al consultar la base de datos", Toast.LENGTH_LONG).show()
            }

        // Inicializar el mapa primero
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configurar los demás componentes
        setupElevationChart()
        setupGeoContext()
        
        // Iniciar la escucha de cambios
        startListeningToService()
    }

    private fun startListeningToService() {
        Log.d(TAG, "🎯 Iniciando escucha de servicio para walkerId: $walkerId")
        
        database.getReference("walkRequests")
            .orderByChild("walkerId")
            .equalTo(walkerId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "⚠️ No se encontraron documentos activos")
                        return
                    }

                    snapshot.children.forEach { child ->
                        val status = child.child("status").getValue(String::class.java)
                        if (status == "accepted" || status == "in_progress") {
                            processServiceUpdate(child)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Error escuchando cambios: ${error.message}")
                }
            })
    }

    // Single implementation of processServiceUpdate
    private fun processServiceUpdate(snapshot: DataSnapshot) {
        Log.d(TAG, "⚙️ Procesando actualización de servicio")
        
        val serviceData = ServiceData(
            id = snapshot.key ?: "",
            status = snapshot.child("status").getValue(String::class.java) ?: "",
            ownerLat = snapshot.child("ownerLat").getValue(Double::class.java),
            ownerLng = snapshot.child("ownerLng").getValue(Double::class.java),
            walkerLat = snapshot.child("paseadorLat").getValue(Double::class.java),
            walkerLng = snapshot.child("paseadorLng").getValue(Double::class.java)
        )

        serviceStatus = serviceData.status
        Log.d(TAG, "📊 Status: ${serviceData.status}")

        Log.d(TAG, """
            📍 Ubicaciones:
            Owner: ${serviceData.ownerLat}, ${serviceData.ownerLng}
            Walker: ${serviceData.walkerLat}, ${serviceData.walkerLng}
        """.trimIndent())

        runOnUiThread {
            updateUIWithServiceData(serviceData)
        }
    }

    private suspend fun setupMap() {
        withContext(Dispatchers.Main) {
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this@RastreoActivity)
        }
    }

    private fun setupGeoContext() {
        geoApiContext = GeoApiContext.Builder()
            .apiKey(BuildConfig.MAPS_API_KEY)
            .build()
    }

    private fun setupElevationChart() {
        elevationChart = binding.elevationChart
        elevationChart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            legend.isEnabled = false
            axisRight.isEnabled = false
            
            axisLeft.apply {
                setDrawGridLines(true)
                textColor = Color.BLACK
                textSize = 12f
                axisMinimum = -10f
                axisMaximum = 10f
                setDrawZeroLine(true)
                gridColor = Color.LTGRAY
                gridLineWidth = 0.5f
                granularity = 1f
            }
            
            xAxis.apply {
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.BLACK
                textSize = 12f
                setLabelCount(4, true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${(value/60).toInt()}m"
                    }
                }
            }

            // Initial empty data
            val entries = ArrayList<Entry>()
            val dataSet = LineDataSet(entries, "Elevación")
            dataSet.apply {
                color = Color.BLUE
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.2f
                setDrawFilled(true)
                fillAlpha = 50
                fillColor = Color.BLUE
            }
            data = LineData(dataSet)
            
            // Initial animation
            animateY(1000)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        startTracking()
    }

    private fun loadServiceStatus() {
        Log.d(TAG, "loadServiceStatus - Buscando servicio activo para walkerId: $walkerId")
        Toast.makeText(this, "Cargando servicio...", Toast.LENGTH_SHORT).show()

        database.getReference("walkRequests")
            .orderByChild("walkerId")
            .equalTo(walkerId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "⚠️ No se encontraron documentos activos")
                        return
                    }

                    snapshot.children.forEach { child ->
                        val status = child.child("status").getValue(String::class.java)
                        if (status == "accepted" || status == "in_progress") {
                            processServiceUpdate(child)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Error escuchando cambios: ${error.message}")
                }
            })
    }

    private fun updateUIWithServiceData(serviceData: ServiceData) {
        serviceStatus = serviceData.status
        updateUIVisibility(serviceData.status)

        if (serviceData.ownerLat != null && serviceData.ownerLng != null) {
            val petLocation = LatLng(serviceData.ownerLat, serviceData.ownerLng)
            showPetMarker(petLocation)
        }

        if (serviceData.walkerLat != null && serviceData.walkerLng != null) {
            val walkerLocation = LatLng(serviceData.walkerLat, serviceData.walkerLng)
            updateWalkerLocation(walkerLocation)
        }

        if (serviceStatus == "in_progress") {
            loadWalkData(serviceData.id)
        }
    }

    private fun updateUIVisibility(status: String) {
        Log.d(TAG, "Actualizando UI para status: $status")
        when (status) {
            "accepted" -> {
                binding.stepCounter.visibility = View.GONE
                binding.tempCounter.visibility = View.VISIBLE
                binding.elevationCard.visibility = View.VISIBLE
            }
            "in_progress" -> {
                binding.stepCounter.visibility = View.VISIBLE
                binding.tempCounter.visibility = View.VISIBLE
                binding.elevationCard.visibility = View.VISIBLE
            }
            else -> {
                binding.stepCounter.visibility = View.GONE
                binding.tempCounter.visibility = View.GONE
                binding.elevationCard.visibility = View.GONE
            }
        }
    }

    private fun trackWalkerLocation(requestId: String) {
        Log.d(TAG, "Iniciando seguimiento de ubicación para requestId: $requestId")
        database.getReference("walkRequests").child(requestId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val walkerLat = snapshot.child("paseadorLat").getValue(Double::class.java)
                    val walkerLng = snapshot.child("paseadorLng").getValue(Double::class.java)
                    Log.d(TAG, "Nueva ubicación del paseador - Lat: $walkerLat, Lng: $walkerLng")

                    if (walkerLat != null && walkerLng != null) {
                        val walkerLatLng = LatLng(walkerLat, walkerLng)
                        updateWalkerLocation(walkerLatLng)

                        if (serviceStatus == "accepted") {
                            Log.d(TAG, "Calculando ruta hacia la mascota")
                            petMarker?.position?.let { petPos ->
                                getDirectionsToLocation(walkerLatLng, petPos)
                            }
                        }
                    } else {
                        Log.e(TAG, "Ubicación del paseador no válida")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error al seguir ubicación: ${error.message}")
                }
            })
    }

    private fun loadWalkData(requestId: String) {
        Log.d(TAG, "Cargando datos del paseo para requestId: $requestId")
        
        val stepsRef = database.getReference("walkRequests").child(requestId).child("steps")
        val tempRef = database.getReference("walkRequests").child(requestId).child("temperature")
        
        deferredInitializer.launch {
            try {
                // Modificar para manejar los tipos correctamente
                val steps = loadSteps(stepsRef)
                val temp = loadTemperature(tempRef)
                val elevation = loadElevation(requestId)
                val route = loadRoute(requestId)

                withContext(Dispatchers.Main) {
                    updateUIWithWalkData(steps, temp, elevation, route)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading walk data: ${e.message}")
            }
        }
    }

    private suspend fun loadSteps(stepsRef: DatabaseReference): Long {
        val snapshot = stepsRef.orderByKey().limitToLast(1).get().await()
        return snapshot.children.firstOrNull()?.child("steps")?.getValue(Long::class.java) ?: 0L
    }

    private suspend fun loadTemperature(tempRef: DatabaseReference): Double {
        val snapshot = tempRef.orderByKey().limitToLast(1).get().await()
        return snapshot.children.firstOrNull()?.child("temperature")?.getValue(Double::class.java) ?: 0.0
    }

    private suspend fun loadElevation(requestId: String): List<Entry> {
        val snapshot = database.getReference("walkRequests")
            .child(requestId)
            .child("elevationHistory")
            .orderByKey()
            .get()
            .await()
        
        return snapshot.children.mapNotNull { doc ->
            val time = doc.child("timeSeconds").getValue(Double::class.java)?.toFloat()
            val elevation = doc.child("elevation").getValue(Double::class.java)?.toFloat()
            if (time != null && elevation != null) {
                Entry(time, elevation)
            } else null
        }
    }

    private suspend fun loadRoute(requestId: String): List<LatLng> {
        val snapshot = database.getReference("walkRequests")
            .child(requestId)
            .child("route")
            .orderByKey()
            .get()
            .await()
        
        return snapshot.children.mapNotNull { doc ->
            val lat = doc.child("latitude").getValue(Double::class.java)
            val lng = doc.child("longitude").getValue(Double::class.java)
            if (lat != null && lng != null) {
                LatLng(lat, lng)
            } else null
        }
    }

    private fun updateUIWithWalkData(steps: Long, temp: Double, elevation: List<Entry>, route: List<LatLng>) {
        binding.stepCounter.text = "Steps: $steps"
        binding.tempCounter.text = String.format("Temp: %.1f°C", temp)
        updateElevationChart(elevation)
        routePoints.clear()
        routePoints.addAll(route)
        drawWalkingRoute()
    }

    private fun updateElevationChart(entries: List<Entry>) {
        val dataSet = LineDataSet(entries, "Elevación").apply {
            color = Color.BLUE
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawFilled(true)
            fillAlpha = 50
            fillColor = Color.BLUE
        }

        elevationChart.data = LineData(dataSet)
        
        // Adjust Y axis range
        val yMin = dataSet.yMin
        val yMax = dataSet.yMax
        val yPadding = 2f
        
        elevationChart.axisLeft.apply {
            axisMinimum = (yMin - yPadding).coerceAtMost(-2f)
            axisMaximum = (yMax + yPadding).coerceAtLeast(2f)
        }
        
        elevationChart.invalidate()
    }

    private fun startTracking() {
        val bounds = LatLngBounds.builder()
        if (petMarker?.position != null) {
            bounds.include(petMarker!!.position)
        }
        if (walkerMarker?.position != null) {
            bounds.include(walkerMarker!!.position)
        }
        try {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
        } catch (e: Exception) {
            Log.e("RastreoActivity", "Error adjusting map bounds: ${e.message}")
        }
    }

    private fun showPetMarker(location: LatLng) {
        Log.d(TAG, "Mostrando marcador de mascota en: ${location.latitude}, ${location.longitude}")
        if (petMarker == null) {
            petMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Ubicación inicial")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )
        } else {
            petMarker?.position = location
        }
    }

    private fun updateWalkerLocation(location: LatLng) {
        Log.d(TAG, "Actualizando ubicación del paseador: ${location.latitude}, ${location.longitude}")
        if (walkerMarker == null) {
            walkerMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Paseador")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        } else {
            walkerMarker?.position = location
        }

        // Adjust map bounds to show both markers
        val bounds = LatLngBounds.builder()
        if (petMarker?.position != null) {
            bounds.include(petMarker!!.position)
        }
        bounds.include(location)
        
        try {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
        } catch (e: Exception) {
            Log.e("RastreoActivity", "Error adjusting map bounds: ${e.message}")
        }
    }

    private fun getDirectionsToLocation(origin: LatLng, destination: LatLng) {
        Thread {
            try {
                val result = DirectionsApi.newRequest(geoApiContext)
                    .mode(TravelMode.WALKING)
                    .origin("${origin.latitude},${origin.longitude}")
                    .destination("${destination.latitude},${destination.longitude}")
                    .await()

                runOnUiThread {
                    drawDirectionsRoute(result)
                }
            } catch (e: Exception) {
                Log.e("RastreoActivity", "Error getting directions: ${e.message}")
            }
        }.start()
    }

    private fun drawDirectionsRoute(result: DirectionsResult) {
        if (result.routes.isNotEmpty()) {
            val decodedPath = PolyUtil.decode(result.routes[0].overviewPolyline.encodedPath)
            
            runOnUiThread {
                if (!::polyline.isInitialized) {
                    polyline = googleMap.addPolyline(
                        PolylineOptions()
                            .addAll(decodedPath)
                            .color(Color.BLUE)
                            .width(5f)
                    )
                } else {
                    polyline.points = decodedPath
                }
            }
        }
    }

    private fun drawWalkingRoute() {
        if (!::polyline.isInitialized) {
            polyline = googleMap.addPolyline(
                PolylineOptions()
                    .addAll(routePoints)
                    .color(Color.RED)
                    .width(5f)
            )
        } else {
            polyline.points = routePoints
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        geoApiContext?.shutdown()
        Log.d(TAG, "onDestroy called")
    }

    companion object {
        private const val TAG = "RastreoActivity"
    }

    // Helper data class
    private data class ServiceData(
        val id: String,
        val status: String,
        val ownerLat: Double?,
        val ownerLng: Double?,
        val walkerLat: Double?,
        val walkerLng: Double?
    )
}
