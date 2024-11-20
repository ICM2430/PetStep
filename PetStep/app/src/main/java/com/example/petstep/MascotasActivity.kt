// MascotasActivity.kt
package com.example.petstep
//check de fetch 
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petstep.databinding.ActivityMascotasBinding
import com.example.petstep.model.MyPet
import com.google.firebase.database.*

class MascotasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMascotasBinding
    private lateinit var petsAdapter: PetsAdapter
    private lateinit var petsList: MutableList<MyPet>
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMascotasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonAgregarMascota.setOnClickListener {
            startActivity(Intent(baseContext, AddPetActivity::class.java))
        }

        petsList = mutableListOf()
        petsAdapter = PetsAdapter(this, petsList)
        binding.listViewMascotas.adapter = petsAdapter

        database = FirebaseDatabase.getInstance().getReference("pets")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newPetsList = mutableListOf<MyPet>()
                for (petSnapshot in snapshot.children) {
                    val pet = petSnapshot.getValue(MyPet::class.java)
                    if (pet != null) {
                        newPetsList.add(pet)
                    }
                }
                petsList.clear()
                petsList.addAll(newPetsList)
                petsAdapter.updatePetsList(petsList)
                updateEmptyView()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors.
                Toast.makeText(this@MascotasActivity, "Failed to load pets: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        binding.listViewMascotas.setOnItemClickListener { _, _, position, _ ->
            if (petsList.isNotEmpty()) {
                val selectedPet = petsList[position]
                val intent = Intent(this, PetDetailActivity::class.java)
                intent.putExtra("pet", selectedPet)
                startActivity(intent)
            }
        }
    }

    private fun updateEmptyView() {
        if (petsList.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.listViewMascotas.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.listViewMascotas.visibility = View.VISIBLE
        }
    }
}