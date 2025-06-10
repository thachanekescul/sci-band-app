package com.example.appv1.cuidador

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.example.appv1.R
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.example.appv1.Adapters.PacienteAdapter.PacienteAdapter
import com.example.appv1.Adapters.PacienteAdapter.Paciente
import com.example.appv1.medicion.MedicionTiempoReal
import com.google.firebase.firestore.DocumentReference

class PacientesCUIDFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PacienteAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var txtNombreCuidador: TextView
    private lateinit var txtNombreOrganizacion: TextView
    private lateinit var btnConfig: ImageView
    private val listaPacientes = mutableListOf<Paciente>()
    private lateinit var imgFotoPerfil: ImageView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pacientes_c_u_i_d, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        val fabAgregarPaciente = view.findViewById<View>(R.id.fabAgregarPaciente)
        recyclerView = view.findViewById(R.id.recyclerPacientes)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        imgFotoPerfil = view.findViewById(R.id.imgFotoPerfil)
        txtNombreCuidador = view.findViewById(R.id.txtNombreCuidador)
        txtNombreOrganizacion = view.findViewById(R.id.txtNombreOrganizacion)
        btnConfig = view.findViewById(R.id.imgConfig)
        adapter = PacienteAdapter(listaPacientes,
            onEditarClick = { paciente -> editarPaciente(paciente) },
            onMedirClick = { paciente -> medirPaciente(paciente) }
        )
        recyclerView.adapter = adapter

        cargarDatosDelCuidador()

        btnConfig.setOnClickListener {
            val intent = Intent(requireContext(), ConfiguracionCuidador::class.java)
            startActivity(intent)
        }

        fabAgregarPaciente.setOnClickListener {
            val intent = Intent(requireContext(), RegistroDePaciente::class.java)
            startActivity(intent)
        }
    }

    private fun cargarDatosDelCuidador() {
        val prefs = requireActivity().getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        val idCuidador = prefs.getString("id_usuario", null)
        val idOrganizacion = prefs.getString("id_organizacion", null)

        if (idCuidador == null || idOrganizacion == null) {
            Toast.makeText(requireContext(), "Error: datos de sesión incompletos", Toast.LENGTH_SHORT).show()
            return
        }

        // Cargar nombre de la organización
        db.collection("organizacion")
            .document(idOrganizacion)
            .get()
            .addOnSuccessListener { orgSnapshot ->
                val nombreOrg = orgSnapshot.getString("nombre") ?: "Desconocida"
                txtNombreOrganizacion.text = "Organización: $nombreOrg"
            }

        // Cargar datos del cuidador, incluyendo foto
        val cuidadorRef = db.collection("organizacion")
            .document(idOrganizacion)
            .collection("cuidadores")
            .document(idCuidador)

        cuidadorRef.get().addOnSuccessListener { cuidadorSnapshot ->
            val nombre = cuidadorSnapshot.getString("nombre") ?: ""
            val ape = cuidadorSnapshot.getString("ape") ?: ""
            txtNombreCuidador.text = "Cuidador: $nombre $ape"

            val fotoUrl = cuidadorSnapshot.getString("profile_picture_url")
            if (!fotoUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(fotoUrl)
                    .placeholder(R.drawable.images)
                    .into(imgFotoPerfil)
            } else {
                // Imagen por defecto si no hay foto
                imgFotoPerfil.setImageResource(R.drawable.images)
            }

            // Después de cargar el cuidador, cargar sus pacientes
            cargarPacientes(cuidadorRef)
        }
    }

    private fun cargarPacientes(cuidadorRef: DocumentReference) {
        cuidadorRef.collection("pacientes")
            .get()
            .addOnSuccessListener { result ->
                listaPacientes.clear()
                for (doc in result) {
                    val paciente = doc.toObject(Paciente::class.java).copy(id = doc.id)
                    val profilePictureUrl = doc.getString("profile_picture_url") // Obtener la URL de la foto

                    // Añadir la URL de la foto al paciente
                    paciente.profilePictureUrl = profilePictureUrl ?: ""  // Si no tiene foto, asignar cadena vacía

                    listaPacientes.add(paciente)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar pacientes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editarPaciente(paciente: Paciente) {
        Toast.makeText(requireContext(), "Editar: ${paciente.nombre}", Toast.LENGTH_SHORT).show()
        val intent = Intent(requireContext(), EditarPacienteCuid::class.java)
        intent.putExtra("idPaciente", paciente.id)
        startActivity(intent)
    }

    private fun medirPaciente(paciente: Paciente) {
        val intent = Intent(requireContext(), MedicionTiempoReal::class.java)
        intent.putExtra("idPaciente", paciente.id)
        startActivity(intent)
    }
}
