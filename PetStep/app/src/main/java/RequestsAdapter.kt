package com.example.petstep.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.petstep.databinding.ItemRequestBinding
import com.example.petstep.adapters.com.example.petstep.model.Request

class RequestsAdapter(
    private val requests: List<Request>,
    private val onRequestAction: (Request, String) -> Unit // "accept" or "reject"
) : RecyclerView.Adapter<RequestsAdapter.RequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        holder.bind(request)
    }

    override fun getItemCount(): Int = requests.size

    inner class RequestViewHolder(private val binding: ItemRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(request: Request) {
            binding.ownerName.text = "Dueño: ${request.ownerName}"
            binding.petDetails.text = "Mascota: ${request.petName}, ${request.petType}"

            binding.acceptButton.setOnClickListener {
                onRequestAction(request, "accept")
            }
            binding.rejectButton.setOnClickListener {
                onRequestAction(request, "reject")
            }
        }
    }
}
