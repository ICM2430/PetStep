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
            .get()
            .addOnSuccessListener { snapshot ->
                val matchingRequests = snapshot.children.filter { 
                    it.child("walkerId").getValue(String::class.java) == walkerId 
                }
                Log.d(TAG, "🔵 Consulta inicial - documentos encontrados: ${matchingRequests.size}")
                matchingRequests.forEach { child ->
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
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Log.d(TAG, "⚠️ No se encontraron documentos")
                        return
                    }

                    snapshot.children
                        .filter { it.child("walkerId").getValue(String::class.java) == walkerId }
                        .filter { child ->
                            val status = child.child("status").getValue(String::class.java)
                            status == "accepted" || status == "in_progress"
                        }
                        .forEach { processServiceUpdate(it) }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "❌ Error escuchando cambios: ${error.message}")
                }
            })
    }

    private fun loadServiceStatus() {
        Log.d(TAG, "loadServiceStatus - Buscando servicio activo para walkerId: $walkerId")
        Toast.makeText(this, "Cargando servicio...", Toast.LENGTH_SHORT).show()

        database.getReference("walkRequests")
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.children
                    .filter { it.child("walkerId").getValue(String::class.java) == walkerId }
                    .filter { child ->
                        val status = child.child("status").getValue(String::class.java)
                        status == "accepted" || status == "in_progress"
                    }
                    .forEach { processServiceUpdate(it) }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error en consulta inicial: ${e.message}")
                Toast.makeText(this, "Error al consultar la base de datos", Toast.LENGTH_LONG).show()
            }
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
        
        // Set default position (e.g. Mexico City)
        val defaultPosition = LatLng(19.4326, -99.1332)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPosition, 12f))
        
        // Initialize empty polyline
        polyline = googleMap.addPolyline(
            PolylineOptions()
                .color(Color.RED)
                .width(5f)
        )
        
        // Load existing data if available
        loadServiceStatus()
    }

    private fun startTracking() {
        if (!::googleMap.isInitialized) {
            Log.e(TAG, "Map not initialized")
            return
        }

        try {
            val boundsBuilder = LatLngBounds.builder()
            var hasValidMarker = false
            
            petMarker?.position?.let { 
                boundsBuilder.include(it)
                hasValidMarker = true
                Log.d(TAG, "Added pet marker to bounds: $it")
            }
            
            walkerMarker?.position?.let { 
                boundsBuilder.include(it)
                hasValidMarker = true
                Log.d(TAG, "Added walker marker to bounds: $it")
            }

            if (hasValidMarker) {
                val bounds = boundsBuilder.build()
                val padding = resources.getDimensionPixelSize(R.dimen.map_padding)
                Log.d(TAG, "Adjusting camera with bounds and padding: $padding")
                
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, padding),
                    1000,  // 1 second duration
                    object : GoogleMap.CancelableCallback {
                        override fun onFinish() {
                            Log.d(TAG, "Camera animation completed")
                        }
                        override fun onCancel() {
                            Log.d(TAG, "Camera animation cancelled")
                        }
                    }
                )
            } else {
                Log.d(TAG, "No valid markers, using default position")
                // Default to a central location with wider zoom if no markers
                val defaultPosition = LatLng(19.4326, -99.1332)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPosition, 8f))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adjusting map bounds: ${e.message}")
            val defaultPosition = LatLng(19.4326, -99.1332)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPosition, 8f))
        }
    }

    private fun updateUIWithServiceData(serviceData: ServiceData) {
        serviceStatus = serviceData.status
        updateUIVisibility(serviceData.status)

        // Calculate map center between walker and pet locations
        val mapCenter = calculateMapCenter(
            serviceData.ownerLat, serviceData.ownerLng,
            serviceData.walkerLat, serviceData.walkerLng
        )

        if (serviceData.ownerLat != null && serviceData.ownerLng != null) {
            val petLocation = LatLng(serviceData.ownerLat, serviceData.ownerLng)
            showPetMarker(petLocation)
        }

        if (serviceData.walkerLat != null && serviceData.walkerLng != null) {
            val walkerLocation = LatLng(serviceData.walkerLat, serviceData.walkerLng)
            updateWalkerLocation(walkerLocation)
        }

        // Center map after markers are placed
        mapCenter?.let { center ->
            val zoom = calculateZoomLevel(
                serviceData.ownerLat, serviceData.ownerLng,
                serviceData.walkerLat, serviceData.walkerLng
            )
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, zoom))
        }

        when (serviceData.status) {
            "accepted" -> {
                if (serviceData.walkerLat != null && serviceData.walkerLng != null &&
                    serviceData.ownerLat != null && serviceData.ownerLng != null) {
                    val walkerLocation = LatLng(serviceData.walkerLat, serviceData.walkerLng)
                    val petLocation = LatLng(serviceData.ownerLat, serviceData.ownerLng)
                    getDirectionsToLocation(walkerLocation, petLocation)
                }
            }
            "in_progress" -> {
                loadWalkData(serviceData.id)
            }
        }
    }

    private fun calculateMapCenter(ownerLat: Double?, ownerLng: Double?, 
                                 walkerLat: Double?, walkerLng: Double?): LatLng? {
        if (ownerLat == null || ownerLng == null || walkerLat == null || walkerLng == null) {
            return null
        }
        
        val centerLat = (ownerLat + walkerLat) / 2
        val centerLng = (ownerLng + walkerLng) / 2
        return LatLng(centerLat, centerLng)
    }

    private fun calculateZoomLevel(ownerLat: Double?, ownerLng: Double?,
                                 walkerLat: Double?, walkerLng: Double?): Float {
        if (ownerLat == null || ownerLng == null || walkerLat == null || walkerLng == null) {
            return 12f
        }

        val latDiff = Math.abs(ownerLat - walkerLat)
        val lngDiff = Math.abs(ownerLng - walkerLng)
        val maxDiff = Math.max(latDiff, lngDiff)

        return when {
            maxDiff > 10 -> 8f    // Very far apart
            maxDiff > 5 -> 10f    // Far apart
            maxDiff > 1 -> 12f    // Medium distance
            maxDiff > 0.1 -> 14f  // Closer
            else -> 16f           // Very close
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
        if (!::googleMap.isInitialized) {
            Log.e(TAG, "Google Map not initialized yet")
            return
        }

        runOnUiThread {
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

            // Update camera only if both markers exist
            if (petMarker != null && walkerMarker != null) {
                try {
                    val bounds = LatLngBounds.builder()
                        .include(petMarker!!.position)
                        .include(location)
                        .build()
                    val padding = resources.getDimensionPixelSize(R.dimen.map_padding)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating camera: ${e.message}")
                }
            }
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

    private fun loadWalkData(walkId: String) {
        Log.d(TAG, "Loading walk data for walkId: $walkId")
        
        // Load steps
        database.getReference("walkRequests")
            .child(walkId)
            .child("steps")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var latestSteps = 0L
                    snapshot.children.forEach { stepData ->
                        val steps = stepData.child("steps").getValue(Long::class.java) ?: 0L
                        if (steps > latestSteps) latestSteps = steps
                    }
                    binding.stepCounter.text = getString(R.string.step_count_format, latestSteps)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading steps: ${error.message}")
                }
            })
        
        // Load temperature
        database.getReference("walkRequests")
            .child(walkId)
            .child("temperature")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var latestTemp = 0f
                    try {
                        snapshot.children.forEach { tempData ->
                            // Try different ways to get the temperature value
                            val temp = when {
                                tempData.child("temperature").getValue(Float::class.java) != null ->
                                    tempData.child("temperature").getValue(Float::class.java)!!
                                tempData.child("temperature").getValue(Double::class.java) != null ->
                                    tempData.child("temperature").getValue(Double::class.java)!!.toFloat()
                                else -> null
                            }
                            
                            if (temp != null) {
                                latestTemp = temp
                                Log.d(TAG, "Temperature read: $latestTemp")
                            }
                        }
                        // Format with one decimal place
                        binding.tempCounter.text = String.format("Temp: %.1f°C", latestTemp)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing temperature: ${e.message}")
                        binding.tempCounter.text = "Temp: N/A"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading temperature: ${error.message}")
                    binding.tempCounter.text = "Temp: N/A"
                }
            })
        
        // Load elevation history
        database.getReference("walkRequests")
            .child(walkId)
            .child("elevationHistory")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val elevationEntries = mutableListOf<Entry>()
                    snapshot.children.forEach { elevationData ->
                        val time = elevationData.child("timeSeconds").getValue(Float::class.java)
                        val elevation = elevationData.child("elevation").getValue(Float::class.java)
                        if (time != null && elevation != null) {
                            elevationEntries.add(Entry(time, elevation))
                        }
                    }
                    
                    if (elevationEntries.isNotEmpty()) {
                        // Sort entries by time
                        elevationEntries.sortBy { it.x }
                        updateElevationChart(elevationEntries)
                        
                        // Update elevation text with latest value
                        val latestElevation = elevationEntries.last().y
                        binding.elevationText.text = String.format("Elevación: %.2f m", latestElevation)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading elevation data: ${error.message}")
                }
            })
            
        // Load route
        loadWalkRoute(walkId)
    }

    private fun loadWalkRoute(walkId: String) {
        database.getReference("walkRequests")
            .child(walkId)
            .child("route")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    routePoints.clear()
                    snapshot.children.forEach { point ->
                        val lat = point.child("latitude").getValue(Double::class.java)
                        val lng = point.child("longitude").getValue(Double::class.java)
                        if (lat != null && lng != null) {
                            routePoints.add(LatLng(lat, lng))
                        }
                    }
                    if (routePoints.isNotEmpty()) {
                        drawWalkingRoute()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading route: ${error.message}")
                }
            })
    }

    private fun updateUIWithWalkData(
        steps: Long,
        temperature: Double,
        elevationPoints: List<Entry>,
        route: List<LatLng>
    ) {
        Log.d(TAG, "Updating UI with walk data - Steps: $steps, Temp: $temperature")
        
        try {
            // Update step counter
            binding.stepCounter.text = getString(R.string.step_count_format, steps)
            
            // Update temperature
            binding.tempCounter.text = getString(R.string.temperature_format, temperature)
            
            // Update elevation chart
            if (elevationPoints.isNotEmpty()) {
                updateElevationChart(elevationPoints)
            }
            
            // Update route on map
            if (route.isNotEmpty()) {
                routePoints.clear()
                routePoints.addAll(route)
                drawWalkingRoute()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI with walk data: ${e.message}")
        }
    }

    private fun updateElevationChart(entries: List<Entry>) {
        Log.d(TAG, "Updating elevation chart with ${entries.size} points")
        
        try {
            val dataSet = LineDataSet(entries, "Elevation").apply {
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

            elevationChart.apply {
                data = LineData(dataSet)
                
                // Calculate Y axis range with padding
                val yMin = entries.minByOrNull { it.y }?.y ?: -1f
                val yMax = entries.maxByOrNull { it.y }?.y ?: 1f
                val yRange = yMax - yMin
                val yPadding = yRange * 0.1f
                
                axisLeft.apply {
                    axisMinimum = yMin - yPadding
                    axisMaximum = yMax + yPadding
                }
                
                // Refresh the chart
                invalidate()
            }
            
            Log.d(TAG, "Elevation chart updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating elevation chart: ${e.message}")
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

    private fun updateUIVisibility(status: String) {
        Log.d(TAG, "Actualizando visibilidad de UI para status: $status")
        when (status) {
            "accepted" -> {
                binding.apply {
                    stepCounter.visibility = View.GONE
                    tempCounter.visibility = View.VISIBLE
                    elevationChart.visibility = View.GONE
                    googleMap.uiSettings.isZoomControlsEnabled = true
                }
            }
            "in_progress" -> {
                binding.apply {
                    stepCounter.visibility = View.VISIBLE
                    tempCounter.visibility = View.VISIBLE
                    elevationChart.visibility = View.VISIBLE
                    googleMap.uiSettings.isZoomControlsEnabled = true
                }
            }
            else -> {
                binding.apply {
                    stepCounter.visibility = View.GONE
                    tempCounter.visibility = View.GONE
                    elevationChart.visibility = View.GONE
                    googleMap.uiSettings.isZoomControlsEnabled = false
                }
            }
        }
    }
}
