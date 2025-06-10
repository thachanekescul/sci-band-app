package com.example.appv1.admin.manejodatos

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate

class LlamadosWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        val orgId = prefs.getString("id_organizacion", null) ?: return@withContext Result.failure()

        val today = LocalDate.now()
        val dayId = String.format("%02d", today.dayOfMonth)

        val resumen = mutableMapOf<String, Any>()
        var totalLlamadosHoy = 0
        var totalAsistenciasHoy = 0

        val llamadosPorCuidador = mutableMapOf<String, Int>()
        val asistenciasPorCuidador = mutableMapOf<String, Int>()
        val nombresCuidadores = mutableMapOf<String, String>()
        val pacientesConRareza = mutableListOf<String>()

        try {
            val cuidadoresSnapshot = db.collection("organizacion").document(orgId)
                .collection("cuidadores").get().await()

            for (cuidadorDoc in cuidadoresSnapshot) {
                val cuidadorId = cuidadorDoc.id
                val cuidadorNombre = cuidadorDoc.getString("nombre") ?: "Cuidador $cuidadorId"
                nombresCuidadores[cuidadorId] = cuidadorNombre
                llamadosPorCuidador[cuidadorId] = 0
                asistenciasPorCuidador[cuidadorId] = 0

                val pacientesSnapshot = db.collection("organizacion").document(orgId)
                    .collection("cuidadores").document(cuidadorId)
                    .collection("pacientes").get().await()

                for (pacienteDoc in pacientesSnapshot) {
                    val pacienteId = pacienteDoc.id
                    val pacienteNombre = pacienteDoc.getString("nombre") ?: "Paciente $pacienteId"

                    val llamadoDoc = db.collection("organizacion").document(orgId)
                        .collection("cuidadores").document(cuidadorId)
                        .collection("pacientes").document(pacienteId)
                        .collection("llamados").document(dayId).get().await()

                    val llamados = llamadoDoc.getLong("llamado")?.toInt() ?: 0
                    totalLlamadosHoy += llamados
                    llamadosPorCuidador[cuidadorId] = llamadosPorCuidador[cuidadorId]!! + llamados

                    val pacienteRef = db.collection("organizacion").document(orgId)
                        .collection("cuidadores").document(cuidadorId)
                        .collection("pacientes").document(pacienteId)

                    pacienteRef.update(mapOf(
                        "llamadosD" to llamados
                    )).await()

                    // Check bio-datosprd
                    val bioDatos = db.collection("organizacion").document(orgId)
                        .collection("cuidadores").document(cuidadorId)
                        .collection("pacientes").document(pacienteId)
                        .collection("bio-datosprd").document(dayId).get().await()

                    val o = bioDatos.getDouble("o") ?: 100.0
                    val p = bioDatos.getDouble("p") ?: 80.0
                    val t = bioDatos.getDouble("t") ?: 36.5

                    if (o < 90 || p < 40 || t < 35.0 || t > 38.5) {
                        pacientesConRareza.add(pacienteNombre)
                    }
                }

                val asistenciaDoc = db.collection("organizacion").document(orgId)
                    .collection("cuidadores").document(cuidadorId)
                    .collection("asistencias").document(dayId).get().await()

                val asistencias = asistenciaDoc.getLong("asistencia")?.toInt() ?: 0
                totalAsistenciasHoy += asistencias
                asistenciasPorCuidador[cuidadorId] = asistencias

                val cuidadorRef = db.collection("organizacion").document(orgId)
                    .collection("cuidadores").document(cuidadorId)

                cuidadorRef.update(mapOf(
                    "asistenciasD" to asistencias
                )).await()
            }

            val cuidadorMenosLlamados = llamadosPorCuidador.minByOrNull { it.value }?.key
            val cuidadorMenosAsistencias = asistenciasPorCuidador.minByOrNull { it.value }?.key

            resumen["llamadosTotalesHoy"] = totalLlamadosHoy
            resumen["asistenciasTotalesHoy"] = totalAsistenciasHoy
            resumen["cuidadorMenosLlamados"] = nombresCuidadores[cuidadorMenosLlamados] ?: "N/A"
            resumen["cuidadorMenosAsistencias"] = nombresCuidadores[cuidadorMenosAsistencias] ?: "N/A"

            // Rareza por exceso de llamados
            if (totalLlamadosHoy > 30) {
                resumen["rareza"] = "Rareza detectada: $totalLlamadosHoy llamados en un solo día"
            }

            // Rareza por condiciones fisiológicas
            if (pacientesConRareza.isNotEmpty()) {
                resumen["rareza"] = "Rareza con paceinte en: ${pacientesConRareza.joinToString(", ")}"
            }

            db.collection("organizacion").document(orgId)
                .collection("resumen_diario").document(dayId)
                .set(resumen).await()

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
