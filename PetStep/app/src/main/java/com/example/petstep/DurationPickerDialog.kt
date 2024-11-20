package com.example.petstep

import android.app.Dialog
import android.content.Context
import android.widget.ArrayAdapter
import com.example.petstep.databinding.ActivityDurationPickerBinding
import com.example.petstep.model.MyPet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DurationPickerDialog(context: Context) {
    private val dialog = Dialog(context)
    private val binding = ActivityDurationPickerBinding.inflate(dialog.layoutInflater)
    private var duration = 30
    private val petsList = mutableListOf<MyPet>()
    private lateinit var petsAdapter: ArrayAdapter<MyPet>

    var onConfirm: ((duration: Int, petId: String) -> Unit)? = null

    init {
        dialog.setContentView(binding.root)
        setupDurationPicker()
        loadUserPets()
    }

    private fun setupDurationPicker() {
        binding.buttonDecrease.setOnClickListener {
            if (duration > 30) {
                duration -= 15
                updateDurationText()
            }
        }

        binding.buttonIncrease.setOnClickListener {
            if (duration < 240) { // 4 hours maximum
                duration += 15
                updateDurationText()
            }
        }

        binding.buttonConfirm.setOnClickListener {
            val selectedPet = binding.spinnerPets.selectedItem as? MyPet
            selectedPet?.let {
                onConfirm?.invoke(duration, it.id) // it.id is now non-null because it's defined as non-null in MyPet
                dialog.dismiss()
            }
        }
    }

    private fun loadUserPets() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("pets")
        
        petsAdapter = ArrayAdapter(
            dialog.context,
            R.layout.spinner_item_pet, // Crearemos este layout
            petsList
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item_pet) // Y este también
        }
        
        binding.spinnerPets.adapter = petsAdapter

        database.orderByChild("userId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    petsList.clear()
                    for (petSnapshot in snapshot.children) {
                        petSnapshot.getValue(MyPet::class.java)?.let { pet ->
                            pet.id = petSnapshot.key ?: return@let // Skip if key is null
                            petsList.add(pet)
                        }
                    }
                    petsAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun updateDurationText() {
        binding.textDuration.text = "$duration min"
    }

    fun show() {
        dialog.show()
    }
}
