package com.example.appv1.paciente

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.appv1.R
import com.example.appv1.paciente.manejodatos.BlePacienteManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class HomePaciente : AppCompatActivity() {


    private lateinit var bleManager: BlePacienteManager

    private lateinit var txtEstadoConexion: TextView
    private lateinit var txtTemperatura: TextView
    private lateinit var txtPulso: TextView
    private lateinit var txtOxigeno: TextView
    private lateinit var btnMedir: Button

    private lateinit var idPaciente: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_paciente)

        txtEstadoConexion = findViewById(R.id.txtEstadoConexion)
        txtTemperatura = findViewById(R.id.temperatura)
        txtPulso = findViewById(R.id.hrtpulso)
        txtOxigeno = findViewById(R.id.oxigeno)
        btnMedir = findViewById(R.id.button2)

        val prefs = getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        idPaciente = prefs.getString("id_usuario", "sin_id") ?: "sin_id"

        bleManager = BlePacienteManager(this, idPaciente)

        observarDatos()
        btnMedir.setOnClickListener {
            bleManager.solicitarMedicion()
            Toast.makeText(this, "Solicitando medición...", Toast.LENGTH_SHORT).show()
        }

        conectarDispositivo()
    }

    private fun observarDatos() {
        lifecycleScope.launch {
            bleManager.sensorDataFlow.collect { data ->
                data?.let {
                    txtTemperatura.text = "%.1f °C".format(it.temperatura)
                    txtPulso.text = it.pulso.toString()
                    txtOxigeno.text = it.oxigeno.toString()

                    subirDatoBioDiario(idPaciente, it.pulso, it.oxigeno, it.temperatura)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun conectarDispositivo() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth no está activo", Toast.LENGTH_SHORT).show()
            return
        }

        val dispositivos = bluetoothAdapter.bondedDevices
        val dispositivo = dispositivos.find { it.name == "SCI-BAND paciente" } // <- nombre BLE desde Arduino

        if (dispositivo == null) {
            Toast.makeText(this, "No se encontró la pulsera vinculada", Toast.LENGTH_SHORT).show()
            return
        }

        txtEstadoConexion.text = "Conectando a ${dispositivo.name}..."
        bleManager.connect(dispositivo)
            .retry(3, 100)
            .useAutoConnect(true)
            .enqueue()
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.disconnect()
    }

    private suspend fun subirDatoBioDiario(
        idPaciente: String,
        pulso: Int,
        oxigeno: Int,
        temperatura: Double
    ): Boolean {
        val db = FirebaseFirestore.getInstance()
        val bioDatosRef = db.collection("pacientes")
            .document(idPaciente)
            .collection("bio-datos")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hoy = dateFormat.format(Date())

        return try {
            val ultimoDoc = bioDatosRef
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val ultimoId: Int
            val fechaUltimo: String

            if (ultimoDoc.isEmpty) {
                ultimoId = 0
                fechaUltimo = ""
            } else {
                val doc = ultimoDoc.documents[0]
                val ts = doc.getLong("timestamp") ?: 0L
                fechaUltimo = dateFormat.format(Date(ts))
                ultimoId = doc.getLong("id")?.toInt() ?: 0
            }

            val nuevoId = if (fechaUltimo != hoy) 1 else ultimoId + 1

            val dato = hashMapOf(
                "pulso" to pulso,
                "oxigeno" to oxigeno,
                "temperatura" to temperatura,
                "timestamp" to System.currentTimeMillis(),
                "id" to nuevoId
            )

            bioDatosRef.document(nuevoId.toString()).set(dato).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}