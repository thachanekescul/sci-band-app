package com.example.appv1.cuidador

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.appv1.R
import com.example.appv1.charts.ChartAsistencias
import com.example.appv1.cuidador.service.BleCuidadorService
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

class HomeCuidadorFragment : Fragment() {

    private lateinit var chartContainer: LinearLayout
    private lateinit var organizacionId: String
    private lateinit var estadoConexionView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_cuidador, container, false)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartContainer = view.findViewById(R.id.linearLayoutGraficas)
        estadoConexionView = view.findViewById(R.id.txtEstadoConexion)

        // Por defecto: Desconectado
        estadoConexionView.text = "Desconectado"
        estadoConexionView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))

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

        Thread {
            cargarResumenDelDia(view, organizacionId, cuidadorId)
        }.start()

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val dispositivos = bluetoothAdapter?.bondedDevices

        val pulsera = dispositivos?.find { it.name == "SCI-BAND cuidador" }

        if (pulsera != null) {
            val intent = Intent(requireContext(), BleCuidadorService::class.java)
            intent.putExtra("device", pulsera)

            if (requireContext().isInForeground()) {
                ContextCompat.startForegroundService(requireContext(), intent)
                Log.d("BLE", "Servicio iniciado automáticamente")


                estadoConexionView.text = "Conectado"
                estadoConexionView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
            } else {
                Log.e("BLE", "No se puede iniciar servicio en segundo plano")


                estadoConexionView.text = "Desconectado"
                estadoConexionView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            }
        }
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
                            .addOnSuccessListener { _ ->
                                requireActivity().runOnUiThread {
                                    estadoLlamadosView.text = "Llamados hoy: $llamadosTotales"
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

    fun Context.isInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = packageName
        for (appProcess in appProcesses) {
            if (appProcess.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }
}
