package com.example.appv1


import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

class FirestoreInitializer(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val PREFS_NAME = "usuario_sesion"
    private val KEY_ORG_ID = "id_organizacion"

    suspend fun initializeDatabase() {
        try {
            // Obtener id_organizacion desde SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val orgid = prefs.getString(KEY_ORG_ID, null) ?: throw IllegalStateException("No orgid found")

            // Fecha actual para inicializar datos de ejemplo
            val today = LocalDate.now()
            val dayId = today.dayOfMonth.toString()

            // Crear cuidadores de ejemplo
            val cuidadores = listOf(
                mapOf("nombre" to "Juan Pérez", "cuidadorId" to "cuidador1"),
                mapOf("nombre" to "Ana Gómez", "cuidadorId" to "cuidador2")
            )

            for (cuidador in cuidadores) {
                val cuidadorId = cuidador["cuidadorId"] as String
                // Crear documento del cuidador
                db.collection("organizacion").document(orgid)
                    .collection("cuidadores").document(cuidadorId)
                    .set(mapOf("nombre" to cuidador["nombre"]))
                    .await()

                // Crear subcolección asistencias con datos de ejemplo
                db.collection("organizacion").document(orgid)
                    .collection("cuidadores").document(cuidadorId)
                    .collection("asistencias").document(dayId)
                    .set(mapOf("asistencias" to (1..5).random())) // Ejemplo: 1-5 asistencias
                    .await()

                // Crear pacientes de ejemplo para este cuidador
                val pacientes = listOf(
                    mapOf("nombre" to "María López", "pacienteId" to "paciente1"),
                    mapOf("nombre" to "Carlos Ramírez", "pacienteId" to "paciente2")
                )

                for (paciente in pacientes) {
                    val pacienteId = paciente["pacienteId"] as String
                    // Crear documento del paciente
                    db.collection("organizacion").document(orgid)
                        .collection("cuidadores").document(cuidadorId)
                        .collection("pacientes").document(pacienteId)
                        .set(
                            mapOf(
                                "nombre" to paciente["nombre"],
                                "llamadosT" to 0L,
                                "llamadosD" to 0L,
                                "llamadosS" to 0L,
                                "llamadosM" to 0L
                            )
                        )
                        .await()

                    // Crear subcolección llamados con datos de ejemplo
                    db.collection("organizacion").document(orgid)
                        .collection("cuidadores").document(cuidadorId)
                        .collection("pacientes").document(pacienteId)
                        .collection("llamados").document(dayId)
                        .set(mapOf("llamados" to (1..3).random())) // Ejemplo: 1-3 llamados
                        .await()
                }
            }

            // Crear colección resumen_diario (inicialmente vacía, LlamadosWorker la llenará)
            db.collection("organizacion").document(orgid)
                .collection("resumen_diario").document(dayId)
                .set(
                    mapOf(
                        "llamadosTotalesHoy" to 0L,
                        "asistenciasTotalesHoy" to 0L,
                        "cuidadorMenosLlamados" to "N/A",
                        "cuidadorMenosAsistencias" to "N/A"
                    )
                )
                .await()

            println("Base de datos inicializada correctamente para orgid: $orgid")
        } catch (e: Exception) {
            println("Error al inicializar la base de datos: ${e.message}")
        }
    }
}