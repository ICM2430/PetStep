// PetsAdapter.kt
package com.example.petstep

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.petstep.model.MyPet
import com.squareup.picasso.Picasso

class PetsAdapter(
    private val context: Context,
    private var petsList: List<MyPet>
) : BaseAdapter() {

    override fun getCount(): Int = petsList.size

    override fun getItem(position: Int): Any = petsList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        if (petsList.isEmpty()) {
            return View(context)
        }

        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_pet, parent, false)
        val pet = petsList[position]

        val petPhotoImageView = view.findViewById<ImageView>(R.id.petPhoto)
        val petNameTextView = view.findViewById<TextView>(R.id.petName)
        val petBreedTextView = view.findViewById<TextView>(R.id.petBreed)

        // Load the pet photo from Firebase Storage
        if (pet.photoUrl.isNotEmpty()) {
            Picasso.get().load(pet.photoUrl).into(petPhotoImageView)
        } else {
            petPhotoImageView.setImageResource(0) // No image
        }

        petNameTextView.text = pet.nombre
        petBreedTextView.text = pet.raza

        return view
    }

    fun updatePetsList(newPetsList: List<MyPet>) {
        petsList = newPetsList
        notifyDataSetChanged()
    }
}