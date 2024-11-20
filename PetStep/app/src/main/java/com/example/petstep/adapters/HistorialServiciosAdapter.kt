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
    private val isOwner: Boolean = false
) : RecyclerView.Adapter<HistorialServiciosAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

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
                statusText.text = when(servicio.status) {
                    "completed" -> "Completado"
                    "in_progress" -> "En curso"
                    "pending" -> "Pendiente"
                    else -> servicio.status
                }
                
                // Formatear y mostrar fecha
                val date = Date(servicio.timestamp)
                dateText.text = "Fecha: ${dateFormat.format(date)}"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistorialServicioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(servicios[position])
    }

    override fun getItemCount() = servicios.size

    fun updateServicios(newServicios: List<ServicioHistorial>) {
        servicios.clear()
        servicios.addAll(newServicios)
        notifyDataSetChanged()
    }
}
