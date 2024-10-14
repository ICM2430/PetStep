package com.example.petstep

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityHistorialServiciosBinding

class HistorialServiciosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialServiciosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialServiciosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val servicios = listOf(
            Servicio("27/08/2024", "10:30 AM", "$18.500", "Chapinero Alto"),
            Servicio("25/08/2024", "03:45 PM", "$20.000", "Chapinero Alto"),
            Servicio("23/08/2024", "01:15 PM", "$15.500", "Chapinero Alto"),
            Servicio("21/08/2024", "09:00 AM", "$17.000", "Chapinero Alto"),
            Servicio("19/08/2024", "11:30 AM", "$19.000", "Chapinero Alto")
        )

        val adapter = ServicioAdapter(this, servicios)
        binding.listViewHistorial.adapter = adapter
    }
}