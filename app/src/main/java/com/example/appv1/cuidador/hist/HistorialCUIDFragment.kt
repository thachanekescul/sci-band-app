package com.example.appv1.cuidador.hist



import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.appv1.R
import com.example.appv1.charts.ChartOxigeno
import com.example.appv1.charts.ChartPulso
import com.example.appv1.charts.ChartTemperatura
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
class HistorialCUIDFragment : Fragment() {

    private lateinit var chipGroupPacientes: ChipGroup
    private lateinit var chartsContainer: LinearLayout

    // IDs que obtenemos de SharedPreferences (sesión del cuidador)
    private lateinit var cuidId: String
    private lateinit var orgId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Asegúrate de inflar el layout que definimos arriba
        return inflater.inflate(R.layout.fragment_historial_c_u_i_d, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Inicializar las vistas usando los IDs del XML:
        chipGroupPacientes = view.findViewById(R.id.chipGroup)
        chartsContainer    = view.findViewById(R.id.chartsContainer)

        // 2) Leer cuidadorId y organizacionId desde SharedPreferences
        val prefs = requireContext()
            .getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        cuidId = prefs.getString("id_usuario", null) ?: ""
        orgId  = prefs.getString("id_organizacion", null) ?: ""

        if (cuidId.isEmpty() || orgId.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Sesión inválida: no se encontró cuidador u organización",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // 3) Llamamos a cargar los pacientes del cuidador
        loadPacientesDelCuidador()
    }

    private fun loadPacientesDelCuidador() {
        val db = FirebaseFirestore.getInstance()
        val pathPacientes = "organizacion/$orgId/cuidadores/$cuidId/pacientes"

        // Limpiar chips anteriores (por si se vuelve a llamar)
        chipGroupPacientes.removeAllViews()

        db.collection(pathPacientes)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "No hay pacientes asignados.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Por cada paciente, creamos un Chip dinámicamente
                for (doc in querySnapshot) {
                    val pacienteId = doc.id
                    val pacienteNombre = doc.getString("nombre") ?: pacienteId

                    val chip = Chip(requireContext()).apply {
                        text = pacienteNombre
                        isCheckable = true
                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                Log.d("HistorialCUID", "Paciente seleccionado: $pacienteNombre ($pacienteId)")
                                loadChartsParaPaciente(pacienteId)
                            }
                        }
                    }
                    chipGroupPacientes.addView(chip)
                }

                // Seleccionamos el primer chip automáticamente (para que se vean gráficos al inicio)
                if (chipGroupPacientes.childCount > 0) {
                    (chipGroupPacientes.getChildAt(0) as? Chip)?.isChecked = true
                }
            }
            .addOnFailureListener { e ->
                Log.e("HistorialCUID", "Error cargando pacientes: ${e.message}")
                Toast.makeText(requireContext(), "Error al cargar pacientes.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadChartsParaPaciente(pacienteId: String) {
        // Limpiamos el contenedor antes de inflar las tres gráficas
        chartsContainer.removeAllViews()

        // 1) Gráfica de Pulso
        ChartPulso(
            requireContext(),
            chartsContainer,
            orgId,
            cuidId,
            pacienteId
        ).loadChart()

        // 2) Gráfica de Oxigenación
        ChartOxigeno(
            requireContext(),
            chartsContainer,
            orgId,
            cuidId,
            pacienteId
        ).loadChart()

        // 3) Gráfica de Temperatura
        ChartTemperatura(
            requireContext(),
            chartsContainer,
            orgId,
            cuidId,
            pacienteId
        ).loadChart()
    }
}