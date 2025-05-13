package com.example.appv1.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.appv1.R
import com.example.appv1.admin.UsuarioItem

class UsuariosAdapter(
    private val items: List<UsuarioItem>,
    private val onEditarCuidadorClick: (String) -> Unit,
    private val onEditarPacienteClick: (String, String) -> Unit,
    private val onMedirPacienteClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TIPO_CUIDADOR = 0
        private const val TIPO_PACIENTE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is UsuarioItem.CuidadorItem -> TIPO_CUIDADOR
            is UsuarioItem.PacienteItem -> TIPO_PACIENTE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIPO_CUIDADOR) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cuidador, parent, false)
            CuidadorViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_paciente, parent, false)
            PacienteViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is UsuarioItem.CuidadorItem -> (holder as CuidadorViewHolder).bind(item)
            is UsuarioItem.PacienteItem -> (holder as PacienteViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class CuidadorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nombre: TextView = itemView.findViewById(R.id.txtNombreCuidador)
        private val editarBtn: Button = itemView.findViewById(R.id.btnEditarCuidador)

        fun bind(cuidador: UsuarioItem.CuidadorItem) {
            nombre.text = cuidador.nombre
            editarBtn.setOnClickListener {
                onEditarCuidadorClick(cuidador.id)
            }
        }
    }

    inner class PacienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nombre: TextView = itemView.findViewById(R.id.txtNombrePaciente)
        private val medirBtn: Button = itemView.findViewById(R.id.btnMedirPaciente)
        private val editarBtn: Button = itemView.findViewById(R.id.btnEditarPaciente)

        fun bind(paciente: UsuarioItem.PacienteItem) {
            nombre.text = paciente.nombre
            medirBtn.setOnClickListener {
                onMedirPacienteClick(paciente.idPaciente)
            }
            editarBtn.setOnClickListener {
                onEditarPacienteClick(paciente.idCuidador, paciente.idPaciente)
            }
        }
    }
}
