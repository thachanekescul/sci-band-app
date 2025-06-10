package com.example.appv1.ui.admin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.appv1.R
import com.example.appv1.charts.ChartAsistencias
import com.example.appv1.charts.ChartCalls
import com.example.appv1.charts.ChartOxigeno
import com.example.appv1.charts.ChartPulso
import com.example.appv1.charts.ChartTemperatura
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore

class HistorialAdminFragment : Fragment() {

    private lateinit var chipGroupFiltro: ChipGroup
    private lateinit var chipGroupPacientes: ChipGroup
    private lateinit var chartsContainer: LinearLayout
    private var organizacionId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_historial_admin, container, false)

        chipGroupFiltro = view.findViewById(R.id.chipGroupFiltro)
        chipGroupPacientes = view.findViewById(R.id.chipGroupPacientes)
        chartsContainer = view.findViewById(R.id.chartsContainer)

        val prefs = requireContext().getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        organizacionId = prefs.getString("id_organizacion", null)

        if (organizacionId == null) {
            Toast.makeText(requireContext(), "ID de organización no encontrado", Toast.LENGTH_LONG).show()
            return view
        }

        chipGroupFiltro.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipPacientes -> {
                    chipGroupPacientes.visibility = View.VISIBLE
                    loadPacientes()
                }
                R.id.chipCuidadores -> {
                    loadCuidadores()
                }
            }
        }

        // Por defecto seleccionamos pacientes
        view.post {
            chipGroupFiltro.check(R.id.chipPacientes)
        }

        return view
    }

    private fun loadPacientes() {
        val orgId = organizacionId ?: return
        chipGroupPacientes.removeAllViews()
        chipGroupPacientes.visibility = View.VISIBLE

        FirebaseFirestore.getInstance()
            .collection("organizacion/$orgId/cuidadores")
            .get()
            .addOnSuccessListener { cuidadorDocs ->
                for (cuidadorDoc in cuidadorDocs) {
                    val cuidadorId = cuidadorDoc.id
                    FirebaseFirestore.getInstance()
                        .collection("organizacion/$orgId/cuidadores/$cuidadorId/pacientes")
                        .get()
                        .addOnSuccessListener { pacienteDocs ->
                            for (pacienteDoc in pacienteDocs) {
                                val pacienteId = pacienteDoc.id
                                val pacienteNombre = pacienteDoc.getString("nombre") ?: pacienteId

                                val chip = Chip(requireContext()).apply {
                                    text = pacienteNombre
                                    tag = cuidadorId // guardamos cuidadorId para usar en carga gráfica
                                    isCheckable = true
                                    setOnCheckedChangeListener { _, isChecked ->
                                        if (isChecked) {
                                            loadChartsForPaciente(cuidadorId, pacienteId)
                                        }
                                    }
                                }
                                chipGroupPacientes.addView(chip)
                            }

                            // Seleccionar el primer paciente automáticamente
                            if (chipGroupPacientes.childCount > 0 &&
                                chipGroupPacientes.checkedChipId == View.NO_ID
                            ) {
                                (chipGroupPacientes.getChildAt(0) as? Chip)?.performClick()
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar pacientes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCuidadores() {
        val orgId = organizacionId ?: return
        chipGroupPacientes.removeAllViews()
        chipGroupPacientes.visibility = View.VISIBLE
        chartsContainer.removeAllViews()

        FirebaseFirestore.getInstance()
            .collection("organizacion/$orgId/cuidadores")
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    val cuidadorId = doc.id
                    val nombreCuidador = doc.getString("nombre") ?: "Cuidador $cuidadorId"

                    val chip = Chip(requireContext()).apply {
                        text = nombreCuidador
                        isCheckable = true
                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                loadChartsForCuidador(cuidadorId)
                            }
                        }
                    }

                    chipGroupPacientes.addView(chip)
                }

                // Seleccionar el primer cuidador automáticamente
                if (chipGroupPacientes.childCount > 0 &&
                    chipGroupPacientes.checkedChipId == View.NO_ID
                ) {
                    (chipGroupPacientes.getChildAt(0) as? Chip)?.performClick()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar cuidadores", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadChartsForPaciente(cuidadorId: String, pacienteId: String) {
        val orgId = organizacionId ?: return
        chartsContainer.removeAllViews()

        ChartPulso(requireContext(), chartsContainer, orgId, cuidadorId, pacienteId).loadChart()
        ChartOxigeno(requireContext(), chartsContainer, orgId, cuidadorId, pacienteId).loadChart()
        ChartTemperatura(requireContext(), chartsContainer, orgId, cuidadorId, pacienteId).loadChart()
        ChartCalls(requireContext(), chartsContainer, orgId, cuidadorId, pacienteId).loadChart()
    }

    private fun loadChartsForCuidador(cuidadorId: String) {
        val orgId = organizacionId ?: return
        chartsContainer.removeAllViews()
        ChartAsistencias(requireContext(), chartsContainer, orgId, cuidadorId).loadChart()
    }
}
