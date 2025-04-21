package com.example.appv1.cuidador

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.appv1.R;
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.example.appv1.Adapters.PacienteAdapter.PacienteAdapter;
import com.example.appv1.Adapters.PacienteAdapter.Paciente;
import com.google.firebase.firestore.DocumentReference

class PacientesCUIDFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PacienteAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var txtNombreCuidador: TextView
    private lateinit var txtNombreOrganizacion: TextView

    private val listaPacientes = mutableListOf<Paciente>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pacientes_c_u_i_d, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        recyclerView = view.findViewById(R.id.recyclerPacientes)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        txtNombreCuidador = view.findViewById(R.id.txtNombreCuidador)
        txtNombreOrganizacion = view.findViewById(R.id.txtNombreOrganizacion)

        adapter = PacienteAdapter(listaPacientes,
            onEditarClick = { paciente -> editarPaciente(paciente) },
            onMedirClick = { paciente -> medirPaciente(paciente) }
        )
        recyclerView.adapter = adapter

        cargarDatosDelCuidador()
    }

    private fun cargarDatosDelCuidador() {
        val prefs = requireActivity().getSharedPreferences("cuidador_sesion", Context.MODE_PRIVATE)
        val idCuidador = prefs.getString("id_cuidador", null)
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

        // Cargar nombre del cuidador
        val cuidadorRef = db.collection("organizacion")
            .document(idOrganizacion)
            .collection("cuidadores")
            .document(idCuidador)

        cuidadorRef.get().addOnSuccessListener { cuidadorSnapshot ->
            val nombre = cuidadorSnapshot.getString("nombre") ?: ""
            val ape = cuidadorSnapshot.getString("ape") ?: ""
            txtNombreCuidador.text = "Cuidador: $nombre $ape"

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
        // Aquí iría tu lógica para abrir modal o activity
    }

    private fun medirPaciente(paciente: Paciente) {
        Toast.makeText(requireContext(), "Medir: ${paciente.nombre}", Toast.LENGTH_SHORT).show()
        // Aquí iría la lógica para graficar o leer datos
    }
}
