package com.example.petstep.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.petstep.ServicioHistorial
import com.example.petstep.databinding.ItemHistorialServicioBinding
import java.text.SimpleDateFormat
import java.util.*

class HistorialServiciosAdapter(
    private val servicios: MutableList<ServicioHistorial>,
    private var isOwner: Boolean
) : RecyclerView.Adapter<HistorialServiciosAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val viewPool = RecyclerView.RecycledViewPool()

    fun setIsOwner(value: Boolean) {
        if (isOwner != value) {
            isOwner = value
            notifyDataSetChanged()
        }
    }

    inner class ViewHolder(private val binding: ItemHistorialServicioBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(servicio: ServicioHistorial) {
            with(binding) {
                // Mostrar información relevante según el tipo de usuario
                if (isOwner) {
                    ownerNameText.text = "Paseador: ${servicio.walkerName}"
                } else {
                    ownerNameText.text = "Dueño: ${servicio.ownerName}"
                }
                
                petNameText.text = "Mascota: ${servicio.petName}"
                durationText.text = "Duración: ${servicio.duration} min"
                priceText.text = "Precio: $${String.format("%.2f", servicio.price)}"
                
                // Mostrar estado del servicio
                statusText.text = when(servicio.status) {
                    "completed" -> "Completado"
                    "in_progress" -> "En curso"
                    "pending" -> "Pendiente"
                    else -> servicio.status
                }
                
                // Mostrar fecha y hora del servicio
                val date = Date(servicio.timestamp)
                dateText.text = "Fecha: ${dateFormat.format(date)}"
                
                // Si el servicio está completado, mostrar duración real
                if (servicio.status == "completed" && servicio.endTime > 0) {
                    val realDuration = (servicio.endTime - servicio.startTime) / 1000 / 60 // convertir a minutos
                    durationText.text = "Duración real: ${realDuration} min"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemHistorialServicioBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        println("DEBUG: Binding service at position $position")
        val servicio = servicios[position]
        println("DEBUG: Service details - ID: ${servicio.id}, Owner: ${servicio.ownerName}, Walker: ${servicio.walkerName}")
        holder.bind(servicio)
    }

    override fun getItemCount() = servicios.size

    fun updateServicios(newServicios: List<ServicioHistorial>) {
        println("DEBUG: Updating adapter with ${newServicios.size} services")
        with(servicios) {
            clear()
            addAll(newServicios)
        }
        println("DEBUG: Services updated, notifying adapter")
        notifyDataSetChanged()
    }
}
