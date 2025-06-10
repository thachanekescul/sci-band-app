package com.example.appv1.cuidador

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.appv1.R
import com.example.appv1.charts.ChartAsistencias
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

class HomeCuidadorFragment : Fragment() {

    private lateinit var chartContainer: LinearLayout
    private lateinit var organizacionId: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_cuidador, container, false) // <-- cambia al layout real
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartContainer = view.findViewById(R.id.linearLayoutGraficas)

        val prefs = requireContext().getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        organizacionId = prefs.getString("id_organizacion", null) ?: return
        val cuidadorId = prefs.getString("id_usuario", null) ?: return

        val chartManager = ChartAsistencias(
            context = requireContext(),
            container = chartContainer,
            organizacionId = organizacionId,
            cuidadorId = cuidadorId
        )
        chartManager.loadChart()

        // Ejecutar carga de resumen fuera del hilo principal
        Thread {
            cargarResumenDelDia(view, organizacionId, cuidadorId)
        }.start()
    }

    private fun cargarResumenDelDia(view: View, orgId: String, cuidadorId: String) {
        val today = LocalDate.now()
        val diaHoy = String.format("%02d", today.dayOfMonth)
        val db = FirebaseFirestore.getInstance()
        val estadoTextView = view.findViewById<TextView>(R.id.txtRareza)
        val estadoLlamadosView = view.findViewById<TextView>(R.id.LlamadosHoy)


        db.collection("organizacion").document(orgId)
            .collection("cuidadores").document(cuidadorId)
            .collection("pacientes")
            .get()
            .addOnSuccessListener { pacientesSnapshot ->
                val pacientes = pacientesSnapshot.documents
                if (pacientes.isEmpty()) {
                    requireActivity().runOnUiThread {
                        estadoTextView.text = "Sin pacientes registrados"
                    }
                    return@addOnSuccessListener
                }

                val llamadosTasks = pacientes.map { pacienteDoc ->
                    val pacienteId = pacienteDoc.id
                    db.collection("organizacion").document(orgId)
                        .collection("cuidadores").document(cuidadorId)
                        .collection("pacientes").document(pacienteId)
                        .collection("llamados").document(diaHoy)
                        .get()
                }

                Tasks.whenAllSuccess<DocumentSnapshot>(llamadosTasks)
                    .addOnSuccessListener { results ->
                        val llamadosTotales = results.sumOf { it.getLong("llamado")?.toInt() ?: 0 }

                        db.collection("organizacion").document(orgId)
                            .collection("cuidadores").document(cuidadorId)
                            .collection("asistencias").document(diaHoy)
                            .get()
                            .addOnSuccessListener { asistenciaDoc ->


                                requireActivity().runOnUiThread {
                                    estadoLlamadosView.text =
                                        "Llamados hoy: $llamadosTotales"
                                }
                            }
                            .addOnFailureListener {
                                requireActivity().runOnUiThread {
                                    estadoLlamadosView.text = "Error al obtener asistencias"
                                }
                            }
                    }
                    .addOnFailureListener {
                        requireActivity().runOnUiThread {
                            estadoLlamadosView.text = "Error al obtener llamados"
                        }
                    }
            }
            .addOnFailureListener {
                requireActivity().runOnUiThread {
                    estadoTextView.text = "Error al obtener pacientes"
                }
            }
    }
}
