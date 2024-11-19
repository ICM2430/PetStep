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
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class PetsAdapter(
    private val context: Context,
    private val petsList: List<MyPet>
) : BaseAdapter() {

    private val storage = FirebaseStorage.getInstance()

    override fun getCount(): Int = petsList.size

    override fun getItem(position: Int): Any = petsList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_pet, parent, false)
        val pet = petsList[position]

        val petPhotoImageView = view.findViewById<ImageView>(R.id.petPhoto)
        val petNameTextView = view.findViewById<TextView>(R.id.petName)
        val petBreedTextView = view.findViewById<TextView>(R.id.petBreed)

        // Load the pet photo from Firebase Storage
        val photoRef = storage.getReferenceFromUrl(pet.photoUrl)
        photoRef.downloadUrl.addOnSuccessListener { uri ->
            Picasso.get().load(uri).placeholder(R.drawable.perrito).into(petPhotoImageView)
        }.addOnFailureListener {
            petPhotoImageView.setImageResource(R.drawable.perrito)
        }

        petNameTextView.text = pet.nombre
        petBreedTextView.text = pet.raza

        return view
    }
}