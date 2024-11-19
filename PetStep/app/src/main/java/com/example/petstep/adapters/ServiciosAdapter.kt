package com.example.petstep

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

data class Servicio(val fecha: String, val hora: String, val valor: String, val zona: String)

class ServicioAdapter(context: Context, private val servicios: List<Servicio>) : ArrayAdapter<Servicio>(context, 0, servicios) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.serviciosrow, parent, false)
        val servicio = servicios[position]

        val fechaTextView = view.findViewById<TextView>(R.id.fecha)
        val ubicacionTextView = view.findViewById<TextView>(R.id.ubicacion)
        val valorTextView = view.findViewById<TextView>(R.id.valor)

        fechaTextView.text = servicio.fecha
        ubicacionTextView.text = servicio.zona
        valorTextView.text = servicio.valor

        return view
    }
}