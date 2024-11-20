package com.example.petstep.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.petstep.databinding.ItemRequestBinding
import com.example.petstep.adapters.com.example.petstep.model.Request

class RequestsAdapter(
    private val requests: List<Request>,
    private val onActionClick: (Request, String) -> Unit
) : RecyclerView.Adapter<RequestsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.bind(request)
    }

    override fun getItemCount() = requests.size

    inner class ViewHolder(private val binding: ItemRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: Request) {
            binding.ownerName.text = "Solicitud de: ${request.ownerName}"
            binding.petDetails.text = "Mascota: ${request.petName} (${request.petType})"

            binding.acceptButton.setOnClickListener {
                onActionClick(request, "accept")
            }
            binding.rejectButton.setOnClickListener {
                onActionClick(request, "reject")
            }
        }
    }
}
