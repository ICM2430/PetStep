package com.example.petstep.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.petstep.databinding.ItemRequestBinding
import com.example.petstep.adapters.com.example.petstep.model.Request

class RequestsAdapter(
    private val requests: MutableList<RequestWithDetails>,
    private val onRequestAction: (RequestWithDetails, String) -> Unit
) : RecyclerView.Adapter<RequestsAdapter.ViewHolder>() {

    data class RequestWithDetails(
        val request: Request,
        var ownerName: String = "",
        var petName: String = "",
        var petAge: String = "",
        var petWeight: String = ""
    )

    inner class ViewHolder(private val binding: ItemRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(requestWithDetails: RequestWithDetails) {
            with(binding) {
                // Mostrar información del dueño
                ownerNameText.text = "Dueño: ${requestWithDetails.ownerName}"
                
                // Mostrar información de la mascota
                petDetailsText.text = "Mascota: ${requestWithDetails.petName} " +
                    "(${requestWithDetails.petAge} años, ${requestWithDetails.petWeight} kg)"
                
                // Mostrar distancia y duración (sin dividir por 1000)
                val distanceKm = requestWithDetails.request.distance
                println("DEBUG: Distance in km: $distanceKm")
                distanceText.text = "Distancia: %.2f km • Duración: ${requestWithDetails.request.duration} min".format(distanceKm)
                
                // Calcular tiempo transcurrido
                val timeDiff = System.currentTimeMillis() - requestWithDetails.request.timestamp
                val minutesAgo = timeDiff / (1000 * 60)
                timeText.text = "Solicitado hace ${minutesAgo} minutos"

                acceptButton.setOnClickListener { onRequestAction(requestWithDetails, "accept") }
                rejectButton.setOnClickListener { onRequestAction(requestWithDetails, "reject") }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount() = requests.size
}
