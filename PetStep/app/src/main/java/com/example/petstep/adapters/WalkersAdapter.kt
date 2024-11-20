package com.example.petstep.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petstep.com.example.petstep.model.Walker
import com.example.petstep.databinding.ItemWalkerBinding

class WalkersAdapter(
    private val walkers: List<Walker>,
    private val onWalkerClicked: (Walker) -> Unit
) : RecyclerView.Adapter<WalkersAdapter.WalkerViewHolder>() {

    inner class WalkerViewHolder(private val binding: ItemWalkerBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(walker: Walker) {
            binding.nombrePaseador.text = "${walker.nombre} ${walker.apellido}"
            binding.distanciaPaseador.text = "A %.2f km".format(walker.distancia)
            binding.precioPaseador.text = "$${walker.precioPorHora}/hora"

            // Cargar imagen del paseador usando profilePhotoUrl
            if (walker.profilePhotoUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(walker.profilePhotoUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .circleCrop()
                    .into(binding.imagenPaseador)
            } else {
                binding.imagenPaseador.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            binding.buttonSolicitar.setOnClickListener {
                onWalkerClicked(walker)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkerViewHolder {
        val binding = ItemWalkerBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return WalkerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WalkerViewHolder, position: Int) {
        holder.bind(walkers[position])
    }

    override fun getItemCount() = walkers.size
}
