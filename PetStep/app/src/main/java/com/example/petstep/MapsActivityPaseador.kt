package com.example.petstep

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.petstep.databinding.ActivityMapsPaseadorBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth  // Agregar este import
import com.google.firebase.database.ServerValue
import java.util.Timer
import java.util.TimerTask
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import com.google.maps.android.PolyUtil
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.formatter.ValueFormatter

class MapsActivityPaseador : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    private lateinit var binding: ActivityMapsPaseadorBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentMarker: Marker? = null
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()  // Agregar esta línea
    private var petMarker: Marker? = null
    private var petLocation: LatLng? = null
    private lateinit var polyline: Polyline
    private var lastLocationUpdate = 0L
    private val MIN_UPDATE_INTERVAL = 30000L // 30 segundos entre actualizaciones
    private var isFirstLocation = true  // Añadir esta propiedad
    private var pendingPetMarker = false
    private var lastSavedLocation: Location? = null
    private val MIN_DISTANCE_CHANGE = 10f  // 10 metros
    private var serviceStatus = ""
    private var activeRequestId = ""
    private var timer: Timer? = null
    private var timerSeconds = 0
    private var remainingSeconds = 0
    private var routePoints = mutableListOf<LatLng>()
    private lateinit var geoApiContext: GeoApiContext
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepCount = 0
    private var lastStepSaveTime = 0L
    private val STEP_SAVE_INTERVAL = 10000L // Cambiar a 10 segundos
    private var lastAcceleration = 0f
    private var currentAcceleration = 0f
    private val STEP_THRESHOLD = 6.0f  // Ajusta este valor según necesites
    private var lastStepCount = 0  // Añadir esta variable
    private var tempSensor: Sensor? = null
    private var lastTempSaveTime = 0L
    private val TEMP_SAVE_INTERVAL = 30000L // 30 segundos para temperatura
    private var currentTemp = 0f
    private var pressureSensor: Sensor? = null
    private var baselinePressure: Float = 0f
    private val pressureReadings = mutableListOf<Entry>()
    private lateinit var elevationChart: LineChart
    private var lastElevation = 0f
    private var chartEntryCount = 0
    private var startTime: Long = 0L  // Añadir para tracking del tiempo desde inicio
    private var lastElevationSaveTime = 0L
    private val ELEVATION_SAVE_INTERVAL = 10000L // 10 segundos entre actualizaciones

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsPaseadorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Instead of getting requestId from intent, get it from user's activeServiceId
        loadActiveServiceFromUser()

        binding.actionButton.setOnClickListener {
            when (serviceStatus) {
                "accepted" -> startWalk()
                "in_progress" -> finishService(activeRequestId)
            }
        }

        geoApiContext = GeoApiContext.Builder()
            .apiKey(BuildConfig.MAPS_API_KEY)
            .build()

        // Inicializar sensores
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        if (stepSensor == null) {
            Toast.makeText(this, "No se encontró acelerómetro", Toast.LENGTH_SHORT).show()
        }

        if (tempSensor == null) {
            Toast.makeText(this, "No se encontró sensor de temperatura", Toast.LENGTH_SHORT).show()
            binding.tempCounter.text = "Temp: N/A"
        }

        if (pressureSensor == null) {
            Toast.makeText(this, "No se encontró barómetro", Toast.LENGTH_SHORT).show()
            binding.elevationCard.visibility = View.GONE
        }

        // Configurar gráfica
        setupElevationChart()
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
                axisMinimum = -10f  // Ajustar rango para cambios más pequeños
                axisMaximum = 10f
                setDrawZeroLine(true)
                gridColor = Color.LTGRAY
                gridLineWidth = 0.5f
                granularity = 1f    // Mostrar valores enteros
            }
            
            xAxis.apply {
                setDrawGridLines(false)
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.BLACK
                textSize = 12f
                setLabelCount(4, true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        // Convertir a minutos desde el inicio
                        return "${(value/60).toInt()}m"
                    }
                }
            }

            // Datos iniciales
            val entries = ArrayList<Entry>()
            val dataSet = LineDataSet(entries, "Elevación")
            dataSet.apply {
                color = Color.BLUE
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
                mode = LineDataSet.Mode.CUBIC_BEZIER  // Línea suave
                cubicIntensity = 0.2f
                setDrawFilled(true)
                fillAlpha = 50
                fillColor = Color.BLUE
            }
            data = LineData(dataSet)
            
            // Animación inicial
            animateY(1000)
        }
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        tempSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        pressureSensor?.let {
            // Aumentar la frecuencia de muestreo para el sensor de presión
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if (serviceStatus == "in_progress") {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    currentAcceleration = kotlin.math.sqrt(x * x + y * y + z * z)

                    if (kotlin.math.abs(currentAcceleration - lastAcceleration) > STEP_THRESHOLD) {
                        stepCount++
                        binding.stepCounter.text = "Steps: $stepCount"

                        // Solo guardar si los pasos han cambiado y ha pasado el tiempo mínimo
                        val currentTime = System.currentTimeMillis()
                        if (stepCount > lastStepCount && 
                            currentTime - lastStepSaveTime >= STEP_SAVE_INTERVAL) {
                            saveStepsToFirebase()
                            lastStepSaveTime = currentTime
                            lastStepCount = stepCount
                        }
                    }
                    lastAcceleration = currentAcceleration
                }
            }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                currentTemp = event.values[0]
                // Redondear a 1 decimal
                val roundedTemp = kotlin.math.round(currentTemp * 10) / 10f
                binding.tempCounter.text = String.format("Temp: %.1f°C", roundedTemp)

                if (serviceStatus == "in_progress") {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTempSaveTime >= TEMP_SAVE_INTERVAL) {
                        // También guardar el valor redondeado en Firebase
                        currentTemp = roundedTemp
                        saveTemperatureToFirebase()
                        lastTempSaveTime = currentTime
                    }
                }
            }
            Sensor.TYPE_PRESSURE -> {
                if (startTime == 0L) {
                    // No procesar antes de iniciar el paseo
                    binding.elevationText.text = "Elevación: -- m"
                    return
                }
                
                val pressure = event.values[0]
                if (baselinePressure == 0f) {
                    baselinePressure = pressure
                    Log.d(TAG, "Presión base establecida: $baselinePressure hPa")
                }

                // Calcular elevación usando la fórmula barométrica
                val elevation = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure)
                val baselineElevation = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, baselinePressure)
                val relativeElevation = elevation - baselineElevation

                // Aplicar un filtro simple para reducir el ruido
                val filteredElevation = if (lastElevation == 0f) {
                    relativeElevation
                } else {
                    lastElevation * 0.7f + relativeElevation * 0.3f
                }

                // Actualizar texto siempre
                binding.elevationText.text = String.format("Elevación: %.2f m", filteredElevation)

                // Pero actualizar gráfica y guardar en Firebase solo si está en progreso
                if (serviceStatus == "in_progress") {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastElevationSaveTime >= ELEVATION_SAVE_INTERVAL) {
                        val elapsedSeconds = (currentTime - startTime) / 1000f
                        updateElevationChart(elapsedSeconds, filteredElevation)
                        saveElevationToFirebase(elapsedSeconds, filteredElevation)
                        lastElevationSaveTime = currentTime
                        Log.d(TAG, "Elevación guardada: $filteredElevation m a los $elapsedSeconds s")
                    }
                }
                
                lastElevation = filteredElevation
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Implementación requerida pero no necesitamos hacer nada aquí
        when(sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Opcional: Log cambios de precisión del acelerómetro
            }
            Sensor.TYPE_PRESSURE -> {
                // Opcional: Log cambios de precisión del barómetro
            }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                // Opcional: Log cambios de precisión del sensor de temperatura
            }
        }
    }

    private fun updateElevationChart(timeSeconds: Float, elevation: Float) {
        val data = elevationChart.data ?: return
        val dataSet = data.getDataSetByIndex(0) as LineDataSet

        // Agregar nuevo punto con tiempo real
        dataSet.addEntry(Entry(timeSeconds, elevation))

        // Notificar cambios
        data.notifyDataChanged()
        elevationChart.notifyDataSetChanged()
        
        // Ajustar rango Y dinámicamente
        val yMin = dataSet.yMin
        val yMax = dataSet.yMax
        val yPadding = 2f
        
        elevationChart.axisLeft.apply {
            axisMinimum = (yMin - yPadding).coerceAtMost(-2f)
            axisMaximum = (yMax + yPadding).coerceAtLeast(2f)
        }
        
        // Ajustar para mostrar toda la gráfica
        elevationChart.fitScreen()
        elevationChart.invalidate()
    }

    private fun saveElevationToFirebase(timeSeconds: Float, elevation: Float) {
        val elevationData = mapOf(
            "elevation" to elevation,
            "timeSeconds" to timeSeconds,
            "timestamp" to System.currentTimeMillis()
        )

        database.getReference("walkRequests")
            .child(activeRequestId)
            .child("elevationHistory")  // Nuevo nodo para histórico completo
            .child(timeSeconds.toInt().toString())
            .setValue(elevationData)
    }

    private fun saveStepsToFirebase() {
        val currentTime = System.currentTimeMillis()
        val stepsData = mapOf(
            "steps" to stepCount,
            "timestamp" to currentTime
        )

        database.getReference("walkRequests")
            .child(activeRequestId)
            .child("steps")
            .child(currentTime.toString())
            .setValue(stepsData)
            .addOnSuccessListener {
                Log.d(TAG, "Pasos guardados: $stepCount")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar pasos: ", e)
            }
    }

    private fun saveTemperatureToFirebase() {
        val currentTime = System.currentTimeMillis()
        val tempData = mapOf(
            "temperature" to currentTemp,
            "timestamp" to currentTime
        )

        database.getReference("walkRequests")
            .child(activeRequestId)
            .child("temperature")
            .child(currentTime.toString())
            .setValue(tempData)
            .addOnSuccessListener {
                Log.d(TAG, "Temperatura guardada: $currentTemp°C")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar temperatura: ", e)
            }
    }

    private fun loadActiveServiceFromUser() {
        val userId = auth.currentUser!!.uid
        database.getReference("users/paseadores")
            .child(userId)
            .get()
            .addOnSuccessListener { userSnapshot ->
                val serviceId = userSnapshot.child("activeServiceId").getValue(String::class.java)
                if (serviceId != null) {
                    activeRequestId = serviceId
                    loadServiceStatus(serviceId)
                    loadPetLocation(serviceId) {
                        startLocationUpdates(serviceId)
                    }
                } else {
                    Toast.makeText(this, "No hay servicio activo", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading active service: ${e.message}")
                Toast.makeText(this, "Error al cargar servicio", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadServiceStatus(requestId: String) {
        database.getReference("walkRequests").child(requestId)
            .get()
            .addOnSuccessListener { snapshot ->
                serviceStatus = snapshot.child("status").getValue(String::class.java) ?: ""
                val durationMinutes = snapshot.child("duration").getValue(Int::class.java) ?: 0

                if (serviceStatus == "in_progress") {
                    val startTime = snapshot.child("startTime").getValue(Long::class.java) ?: 0L
                    remainingSeconds = calculateRemainingSeconds(startTime, durationMinutes)
                    startTimer()
                } else {
                    remainingSeconds = durationMinutes * 60
                }

                updateUIForStatus()
            }
    }

    private fun calculateRemainingSeconds(startTime: Long, durationMinutes: Int): Int {
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = ((currentTime - startTime) / 1000).toInt()
        val totalSeconds = durationMinutes * 60
        return maxOf(0, totalSeconds - elapsedSeconds)
    }

    private fun loadPetLocation(requestId: String, onComplete: () -> Unit) {
        database.getReference("walkRequests")
            .child(requestId)
            .get()
            .addOnSuccessListener { snapshot ->
                val ownerLat = snapshot.child("ownerLat").getValue(Double::class.java) ?: 0.0
                val ownerLng = snapshot.child("ownerLng").getValue(Double::class.java) ?: 0.0
                println("DEBUG: Pet location loaded - Lat: $ownerLat, Lng: $ownerLng")

                petLocation = LatLng(ownerLat, ownerLng)

                // Verificar si el mapa está listo o necesitamos esperar
                if (::googleMap.isInitialized || pendingPetMarker) {
                    showPetMarker()
                    pendingPetMarker = false
                }

                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading pet location: ${e.message}")
            }
    }

    private fun showPetMarker() {
        petLocation?.let { location ->
            Log.d(TAG, "Adding pet marker at: ${location.latitude}, ${location.longitude}")
            petMarker?.remove()
            petMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Ubicación de la mascota")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Si ya tenemos la ubicación de la mascota, mostrarla inmediatamente
        if (petLocation != null) {
            showPetMarker()
        } else {
            pendingPetMarker = true
        }
    }

    private fun startLocationUpdates(serviceId: String) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationOnMap(location)
                    updateLocationInFirebase(location, serviceId)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun updateLocationOnMap(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)

        if (currentMarker == null) {
            currentMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(currentLatLng)
                    .title("Tu ubicación")
            )
            // Si es la primera ubicación y ya tenemos el marcador de la mascota, ajustar zoom
            if (isFirstLocation && petMarker != null) {
                adjustMapZoom()
                isFirstLocation = false
            }
        } else {
            currentMarker!!.position = currentLatLng
        }

        when (serviceStatus) {
            "accepted" -> {
                // Obtener ruta por calles entre paseador y mascota
                petLocation?.let { petLoc ->
                    getDirectionsToLocation(currentLatLng, petLoc)
                }
            }
            "in_progress" -> {
                routePoints.add(currentLatLng)
                drawWalkingRoute()
            }
        }

        if (isFirstLocation) {
            isFirstLocation = false
            adjustMapZoom()
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
                Log.e(TAG, "Error getting directions: ${e.message}")
            }
        }.start()
    }

    private fun drawDirectionsRoute(result: DirectionsResult) {
        if (result.routes.isNotEmpty()) {
            val decodedPath = PolyUtil.decode(result.routes[0].overviewPolyline.encodedPath)

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

    private fun drawDirectRoute(start: LatLng, end: LatLng) {
        val points = listOf(start, end)
        if (!::polyline.isInitialized) {
            polyline = googleMap.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .color(Color.BLUE)
                    .width(5f)
            )
        } else {
            polyline.points = points
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

    private fun loadExistingRoute(requestId: String) {
        database.getReference("walkRequests")
            .child(requestId)
            .child("route")
            .get()
            .addOnSuccessListener { snapshot ->
                routePoints.clear()
                snapshot.children.forEach { point ->
                    val lat = point.child("latitude").getValue(Double::class.java)
                    val lng = point.child("longitude").getValue(Double::class.java)
                    if (lat != null && lng != null) {
                        routePoints.add(LatLng(lat, lng))
                    }
                }
                if (routePoints.isNotEmpty() && serviceStatus == "in_progress") {
                    drawWalkingRoute()
                }
            }
    }

    private fun adjustMapZoom() {
        try {
            val builder = LatLngBounds.Builder()
            if (currentMarker?.position != null && petMarker?.position != null) {
                builder.include(currentMarker!!.position)
                builder.include(petMarker!!.position)

                val bounds = builder.build()
                val padding = 200 // padding in pixels
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                googleMap.animateCamera(cameraUpdate)
                Log.d(TAG, "Zoom ajustado para incluir ambos marcadores")
            } else {
                Log.d(TAG, "No se pudo ajustar el zoom: faltan marcadores")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ajustando zoom: ${e.message}")
        }
    }

    private fun updateLocationInFirebase(location: Location, requestId: String) {
        // Solo guardar ubicaciones si el servicio está en progreso
        if (serviceStatus != "in_progress") return

        val currentTime = System.currentTimeMillis()

        // Verificar si ha pasado suficiente tiempo desde la última actualización
        if (currentTime - lastLocationUpdate < MIN_UPDATE_INTERVAL) {
            return
        }

        // Verificar si la ubicación ha cambiado significativamente
        if (lastSavedLocation != null &&
            location.distanceTo(lastSavedLocation!!) < MIN_DISTANCE_CHANGE) {
            return
        }

        val geoPoint = mapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "timestamp" to currentTime
        )

        database.getReference("walkRequests")
            .child(requestId)
            .child("route")
            .child(currentTime.toString())
            .setValue(geoPoint)
            .addOnSuccessListener {
                lastLocationUpdate = currentTime
                lastSavedLocation = location
                Log.d(TAG, "Ubicación guardada: lat=${location.latitude}, lng=${location.longitude}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar ubicación: ", e)
            }
    }

    private fun finishService(requestId: String) {
        val totalDistance = calculateTotalDistance()
        val elevationStats = calculateElevationStats()

        // Get request details to send notification
        database.getReference("walkRequests").child(requestId).get()
            .addOnSuccessListener { requestSnapshot ->
                val userId = requestSnapshot.child("userId").getValue(String::class.java)
                
                // Get walker's name
                database.getReference("users/paseadores/${auth.currentUser!!.uid}")
                    .get()
                    .addOnSuccessListener { walkerSnapshot ->
                        val walkerName = "${walkerSnapshot.child("nombre").getValue(String::class.java)} ${
                            walkerSnapshot.child("apellido").getValue(String::class.java)
                        }"
                        
                        val updates = hashMapOf<String, Any?>(
                            "walkRequests/$requestId/status" to "completed",
                            "walkRequests/$requestId/endTime" to ServerValue.TIMESTAMP,
                            "walkRequests/$requestId/distance" to totalDistance,
                            "walkRequests/$requestId/totalSteps" to stepCount,
                            "walkRequests/$requestId/elevationStats" to elevationStats,
                            "users/paseadores/${auth.currentUser!!.uid}/activeServiceId" to null,
                            "users/paseadores/${auth.currentUser!!.uid}/activeService" to false
                        )

                        database.reference.updateChildren(updates)
                            .addOnSuccessListener {
                                // Send notification to owner
                                userId?.let {
                                    NotificationService().sendWalkCompletedNotification(it, walkerName, totalDistance)
                                }
                                
                                println("DEBUG: Servicio finalizado - Distancia: $totalDistance km")
                                Toast.makeText(this, "Servicio finalizado", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                println("ERROR: Fallo al finalizar servicio: ${e.message}")
                                Toast.makeText(this, "Error al finalizar servicio", Toast.LENGTH_SHORT).show()
                            }
                    }
            }
    }

    private fun calculateTotalDistance(): Double {
        var totalDistance = 0.0
        for (i in 0 until routePoints.size - 1) {
            val results = FloatArray(1)
            Location.distanceBetween(
                routePoints[i].latitude,
                routePoints[i].longitude,
                routePoints[i + 1].latitude,
                routePoints[i + 1].longitude,
                results
            )
            totalDistance += results[0]
        }
        // Convert to kilometers and round to 3 decimal places
        return (totalDistance / 1000 * 1000).toInt() / 1000.0
    }

    private fun calculateElevationStats(): Map<String, Any> {
        val dataSet = elevationChart.data.getDataSetByIndex(0) as LineDataSet
        return mapOf(
            "maxElevation" to dataSet.yMax,
            "minElevation" to dataSet.yMin,
            "totalAscent" to calculateTotalAscent(dataSet),
            "totalDescent" to calculateTotalDescent(dataSet)
        )
    }

    private fun calculateTotalAscent(dataSet: LineDataSet): Float {
        var totalAscent = 0f
        for (i in 1 until dataSet.entryCount) {
            val diff = dataSet.getEntryForIndex(i).y - dataSet.getEntryForIndex(i-1).y
            if (diff > 0) totalAscent += diff
        }
        return totalAscent
    }

    private fun calculateTotalDescent(dataSet: LineDataSet): Float {
        var totalDescent = 0f
        for (i in 1 until dataSet.entryCount) {
            val diff = dataSet.getEntryForIndex(i).y - dataSet.getEntryForIndex(i-1).y
            if (diff < 0) totalDescent -= diff
        }
        return totalDescent
    }

    private fun startWalk() {
        startTime = System.currentTimeMillis()
        val updates = hashMapOf<String, Any>(
            "walkRequests/$activeRequestId/status" to "in_progress",
            "walkRequests/$activeRequestId/startTime" to startTime
        )

        // Get request details to send notification
        database.getReference("walkRequests").child(activeRequestId).get()
            .addOnSuccessListener { requestSnapshot ->
                val userId = requestSnapshot.child("userId").getValue(String::class.java)
                
                // Get walker's name
                database.getReference("users/paseadores/${auth.currentUser!!.uid}")
                    .get()
                    .addOnSuccessListener { walkerSnapshot ->
                        val walkerName = "${walkerSnapshot.child("nombre").getValue(String::class.java)} ${
                            walkerSnapshot.child("apellido").getValue(String::class.java)
                        }"
                        
                        // Update status and send notification
                        database.reference.updateChildren(updates)
                            .addOnSuccessListener {
                                serviceStatus = "in_progress"
                                routePoints.clear()
                                updateUIForStatus()
                                startTimer()
                                
                                // Send notification to owner
                                userId?.let {
                                    NotificationService().sendWalkStartedNotification(it, walkerName)
                                }
                            }
                    }
            }
    }

    private fun updateUIForStatus() {
        when (serviceStatus) {
            "accepted" -> {
                binding.actionButton.text = "Ya llegué"
                binding.timerTextView.visibility = View.GONE
                binding.stepCounter.visibility = View.GONE
                binding.tempCounter.visibility = View.VISIBLE  // Temperatura siempre visible
                binding.elevationCard.visibility = View.VISIBLE
            }
            "in_progress" -> {
                binding.actionButton.text = "Terminar servicio"
                binding.timerTextView.visibility = View.VISIBLE
                binding.stepCounter.visibility = View.VISIBLE
                binding.tempCounter.visibility = View.VISIBLE
                binding.stepCounter.text = "Steps: $stepCount"
                binding.elevationCard.visibility = View.VISIBLE
                // Resetear línea base de presión
                baselinePressure = 0f
            }
        }
    }

    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (remainingSeconds > 0) {
                    remainingSeconds--
                    runOnUiThread {
                        val hours = remainingSeconds / 3600
                        val minutes = (remainingSeconds % 3600) / 60
                        val seconds = remainingSeconds % 60
                        binding.timerTextView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                        // Si llegamos a cero, notificar al usuario
                        if (remainingSeconds == 0) {
                            Toast.makeText(applicationContext, "¡Tiempo del paseo completado!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }, 0, 1000)
    }

    override fun onStop() {
        super.onStop()
        timer?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "MapsActivityPaseador"
    }
}
