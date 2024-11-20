package com.example.petstep.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petstep.databinding.ItemWalkerBinding
import com.example.petstep.com.example.petstep.model.Walker

class WalkersAdapter(
    private val walkers: List<Walker>,
    private val onWalkerSelected: (Walker) -> Unit
) : RecyclerView.Adapter<WalkersAdapter.WalkerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkerViewHolder {
        val binding = ItemWalkerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalkerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WalkerViewHolder, position: Int) {
        val walker = walkers[position]
        holder.bind(walker)
    }

    override fun getItemCount(): Int = walkers.size

    inner class WalkerViewHolder(private val binding: ItemWalkerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(walker: Walker) {
            binding.walkerName.text = walker.nombre
            binding.walkerPrice.text = "Precio: ${walker.precioPorHora} €/hora"
            binding.walkerDistance.text = "Distancia: ${walker.distancia} km"

            Glide.with(binding.root.context)
                .load(walker.fotoPerfil)
                .placeholder(com.example.petstep.R.drawable.avatar)
                .into(binding.walkerImage)

            binding.root.setOnClickListener {
                onWalkerSelected(walker)
            }
        }
    }
}
