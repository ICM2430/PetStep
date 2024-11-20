package com.example.petstep.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.petstep.databinding.ItemPetBinding
import com.example.petstep.adapters.com.example.petstep.model.Pet

class PetAdapter(
    private val pets: List<Pet>,
    private val onDelete: (Pet) -> Unit
) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = pets[position]
        holder.bind(pet)
    }

    override fun getItemCount(): Int = pets.size

    inner class PetViewHolder(private val binding: ItemPetBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pet: Pet) {
            binding.petName.text = pet.nombre
            binding.petType.text = pet.tipo
            binding.petAge.text = "Edad: ${pet.edad} años"
            binding.deletePetButton.setOnClickListener { onDelete(pet) }
        }
    }
}