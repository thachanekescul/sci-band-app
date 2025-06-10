package com.example.appv1.admin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appv1.Adapters.UsuariosAdapter
import com.example.appv1.R
import com.example.appv1.medicion.MedicionTiempoReal
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore

class GruposAdminFragment : Fragment() {

    private lateinit var chipGroup: ChipGroup
    private lateinit var recyclerView: RecyclerView
    private lateinit var usuariosAdapter: UsuariosAdapter
    private val db = FirebaseFirestore.getInstance()
    private val listaUsuarios = mutableListOf<UsuarioItem>()
    private lateinit var idOrganizacion: String
    private lateinit var txtNombreOrganizacion: TextView
    private lateinit var txtNombreAdmin: TextView
    private lateinit var txtCodigoOrganizacion: TextView
    private lateinit var imgConfigAdmin: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_grupos_admin, container, false)

        chipGroup = view.findViewById(R.id.chipGroup)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        txtNombreOrganizacion = view.findViewById(R.id.txtNombreOrganizacion)
        txtNombreAdmin = view.findViewById(R.id.txtNombreAdmin)
        txtCodigoOrganizacion = view.findViewById(R.id.txtCodigoOrganizacion)
        imgConfigAdmin = view.findViewById(R.id.imgConfigAdmin)

        usuariosAdapter = UsuariosAdapter(
            listaUsuarios,
            onEditarCuidadorClick = { cuidadorId ->
                val intent = Intent(requireContext(), EditarCuidadorAdmin::class.java)
                intent.putExtra("idCuidador", cuidadorId)
                startActivity(intent)
            },
            onEditarPacienteClick = { cuidadorId, pacienteId ->
                val intent = Intent(requireContext(), EditarPacienteAdmin::class.java)
                intent.putExtra("idCuidador", cuidadorId)
                intent.putExtra("idPaciente", pacienteId)
                startActivity(intent)
            },
            onMedirPacienteClick = { pacienteId ->
                val intent = Intent(requireContext(), MedicionTiempoReal::class.java)
                intent.putExtra("idPaciente", pacienteId)
                startActivity(intent)
            }
        )

        imgConfigAdmin.setOnClickListener {
            val intent = Intent(requireContext(), ConfiguracionAdmin::class.java)
            startActivity(intent)
        }

        recyclerView.adapter = usuariosAdapter

        val prefs = requireActivity().getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        idOrganizacion = prefs.getString("id_organizacion", "")!!

        cargarOrganizacion()
        cargarCuidadores()

        return view
    }

    private fun cargarOrganizacion() {
        val prefs = requireActivity().getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        val idAdmin = prefs.getString("id_usuario", "") // Obtener el ID del admin desde SharedPreferences
        val idOrganizacion = prefs.getString("id_organizacion", "") // Obtener el ID de la organización

        if (idAdmin.isNullOrEmpty() || idOrganizacion.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Error: datos de sesión incompletos", Toast.LENGTH_SHORT).show()
            return
        }

        // Primero, carga la organización
        db.collection("organizacion")
            .document(idOrganizacion)  // Aquí accedemos al documento de la organización
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nombreOrg = document.getString("nombre") ?: "Sin nombre"
                    txtNombreOrganizacion.text = nombreOrg
                    val codigoOrg = document.id
                    txtCodigoOrganizacion.text = "Código de su Org: $codigoOrg"

                    // Ahora carga el admin con el ID que obtuvimos
                    cargarAdmin(idAdmin)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar la organización", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarAdmin(idAdmin: String) {
        db.collection("organizacion")
            .document(idOrganizacion) // Usar idOrganizacion que ya tenemos
            .collection("administradores")  // Subcolección donde está el admin
            .document(idAdmin)  // Usamos el ID del admin que obtuvimos de SharedPreferences
            .get()
            .addOnSuccessListener { adminDoc ->
                if (adminDoc.exists()) {
                    val nombreAdmin = adminDoc.getString("nombre") ?: "Sin nombre del admin"
                    txtNombreAdmin.text = nombreAdmin
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar el admin", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarCuidadores() {
        db.collection("organizacion")
            .document(idOrganizacion)
            .collection("cuidadores")
            .get()
            .addOnSuccessListener { result ->
                chipGroup.removeAllViews()
                for (document in result) {
                    val cuidadorId = document.id
                    val nombre = document.getString("nombre") ?: "Sin nombre"
                    val profilePictureUrl = document.getString("profile_picture_url") ?: "" // Cargar la URL de la foto

                    val chip = Chip(requireContext()).apply {
                        text = nombre
                        isCheckable = true
                        isClickable = true
                        setTextColor(resources.getColor(R.color.black, null))
                        chipBackgroundColor = resources.getColorStateList(R.color.chip_background_selector, null)
                        chipStrokeColor = resources.getColorStateList(R.color.chip_stroke_selector, null)
                        chipStrokeWidth = 2f
                        chipCornerRadius = 50f
                        textSize = 16f
                    }
                    chipGroup.addView(chip)

                    chip.setOnClickListener {
                        cargarGrupo(cuidadorId, nombre, profilePictureUrl)
                    }
                }
            }
    }

    private fun cargarGrupo(cuidadorId: String, nombreCuidador: String, profilePictureUrl: String) {
        listaUsuarios.clear()

        listaUsuarios.add(UsuarioItem.CuidadorItem(cuidadorId, nombreCuidador, profilePictureUrl))

        db.collection("organizacion")
            .document(idOrganizacion)
            .collection("cuidadores")
            .document(cuidadorId)
            .collection("pacientes")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val pacienteId = document.id
                    val nombrePaciente = document.getString("nombre") ?: "Paciente sin nombre"
                    val pacienteProfilePictureUrl = document.getString("profile_picture_url") ?: ""

                    listaUsuarios.add(UsuarioItem.PacienteItem(cuidadorId, pacienteId, nombrePaciente, pacienteProfilePictureUrl))
                }
                usuariosAdapter.notifyDataSetChanged()
            }
    }
}
