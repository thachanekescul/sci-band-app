package com.example.appv1.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.appv1.R

class PacienteAdapter {
    data class Paciente(
        var nombre: String = "",
        var apellido: String = "",
        var telefono: String = "",
        var condicion_cronica: String = "",
        var id: String = "",
        var profilePictureUrl: String = ""  // Usamos 'var' en lugar de 'val'
    )


    class PacienteAdapter(
        private val pacientes: List<Paciente>,
        private val onEditarClick: (Paciente) -> Unit,
        private val onMedirClick: (Paciente) -> Unit
    ) : RecyclerView.Adapter<PacienteAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nombre: TextView = itemView.findViewById(R.id.txtNombrePaciente)
            val btnEditar: Button = itemView.findViewById(R.id.btnEditarPaciente)
            val btnMedir: Button = itemView.findViewById(R.id.btnMedirPaciente)
            val img: ImageView = itemView.findViewById(R.id.imgPaciente)  // ImageView para mostrar la foto
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_paciente, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = pacientes.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val paciente = pacientes[position]
            holder.nombre.text = "${paciente.nombre} ${paciente.apellido}"
            holder.btnEditar.setOnClickListener { onEditarClick(paciente) }
            holder.btnMedir.setOnClickListener { onMedirClick(paciente) }

            // Cargar la imagen desde la URL con Glide
            if (paciente.profilePictureUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(paciente.profilePictureUrl)
                    .into(holder.img)
            } else {
                holder.img.setImageResource(R.drawable.images)  // Si no hay foto, muestra un placeholder
            }
        }
    }
}
