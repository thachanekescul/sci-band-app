package com.example.appv1.paciente.manejodatos

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import java.text.SimpleDateFormat
import java.util.*

class BlePacienteManager(context: Context, private val idPaciente: String) : BleManager(context) {

    companion object {
        val SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val CHAR_PULSO_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abd")
        val CHAR_OXIGENO_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abe")
        val CHAR_TEMP_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abf")
    }

    private var pulsoChar: BluetoothGattCharacteristic? = null
    private var oxigenoChar: BluetoothGattCharacteristic? = null
    private var tempChar: BluetoothGattCharacteristic? = null

    private val _sensorDataFlow = MutableStateFlow<SensorData?>(null)
    val sensorDataFlow = _sensorDataFlow.asStateFlow()

    data class SensorData(val pulso: Int, val oxigeno: Int, val temperatura: Double)

    private var ultimoPulso: Int? = null
    private var ultimoOxigeno: Int? = null
    private var ultimaTemp: Double? = null
    private val firestore = FirebaseFirestore.getInstance()
    private var ultimoDia = getHoy()
    private var contador = 0

    override fun getGattCallback() = object : BleManagerGattCallback() {
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(SERVICE_UUID) ?: return false
            pulsoChar = service.getCharacteristic(CHAR_PULSO_UUID)
            oxigenoChar = service.getCharacteristic(CHAR_OXIGENO_UUID)
            tempChar = service.getCharacteristic(CHAR_TEMP_UUID)
            return pulsoChar != null && oxigenoChar != null && tempChar != null
        }

        override fun initialize() {
            setNotificationCallback(pulsoChar).with { _, data -> parsePulso(data) }
            enableNotifications(pulsoChar).enqueue()

            setNotificationCallback(oxigenoChar).with { _, data -> parseOxigeno(data) }
            enableNotifications(oxigenoChar).enqueue()

            setNotificationCallback(tempChar).with { _, data -> parseTemp(data) }
            enableNotifications(tempChar).enqueue()
        }

        override fun onDeviceDisconnected() {
            pulsoChar = null
            oxigenoChar = null
            tempChar = null
        }

        override fun onServicesInvalidated() {
            pulsoChar = null
            oxigenoChar = null
            tempChar = null
        }
    }

    private fun parsePulso(data: Data) {
        val str = data.value?.toString(Charsets.UTF_8) ?: return
        str.toIntOrNull()?.takeIf { it in 40..180 }?.let {
            ultimoPulso = it
            emitirYGuardar()
        }
    }

    private fun parseOxigeno(data: Data) {
        val str = data.value?.toString(Charsets.UTF_8) ?: return
        str.toIntOrNull()?.takeIf { it in 70..100 }?.let {
            ultimoOxigeno = it
            emitirYGuardar()
        }
    }

    private fun parseTemp(data: Data) {
        val value = data.value ?: return
        if (value.size >= 2) {
            val tempRaw = (value[0].toInt() and 0xFF) or ((value[1].toInt() and 0xFF) shl 8)
            val temp = tempRaw / 100.0
            if (temp in 34.0..42.0) {
                ultimaTemp = temp
                emitirYGuardar()
            }
        }
    }

    private fun emitirYGuardar() {
        if (ultimoPulso != null && ultimoOxigeno != null && ultimaTemp != null) {
            val datos = SensorData(ultimoPulso!!, ultimoOxigeno!!, ultimaTemp!!)
            _sensorDataFlow.value = datos
            guardarEnFirestore(datos)
            // Reinicio para siguiente lectura
            ultimoPulso = null
            ultimoOxigeno = null
            ultimaTemp = null
        }
    }

    public fun guardarEnFirestore(data: SensorData) {
        val hoy = getHoy()
        if (hoy != ultimoDia) {
            moverPromedioADiario()
            contador = 0
            ultimoDia = hoy
        }

        val idLectura = contador.toString().padStart(2, '0')
        val docRef = firestore.collection("pacientes")
            .document(idPaciente)
            .collection("bio-datos")
            .document(idLectura)

        val datosConFecha = mapOf(
            "pulso" to data.pulso,
            "oxigeno" to data.oxigeno,
            "temperatura" to data.temperatura,
            "timestamp" to System.currentTimeMillis()
        )

        docRef.set(datosConFecha)
            .addOnSuccessListener { Log.d("Firestore", "Lectura subida: $idLectura") }
            .addOnFailureListener { Log.e("Firestore", "Error al subir datos", it) }

        contador++
    }

    private fun moverPromedioADiario() {
        val col = firestore.collection("pacientes").document(idPaciente).collection("bio-datos")
        col.get().addOnSuccessListener { snapshot ->
            val datos = snapshot.documents.mapNotNull { it.data }
            if (datos.size < 48) return@addOnSuccessListener

            val promedio = mapOf(
                "prom_pulso" to datos.mapNotNull { it["pulso"] as? Int }.average(),
                "prom_oxigeno" to datos.mapNotNull { it["oxigeno"] as? Int }.average(),
                "prom_temp" to datos.mapNotNull { it["temperatura"] as? Number }
                    .map { it.toDouble() }.average(),
                "fecha" to getHoy()
            )

            firestore.collection("pacientes").document(idPaciente)
                .collection("promPpac")
                .document(ultimoDia)
                .set(promedio)

            snapshot.documents.forEach { it.reference.delete() }
        }
    }

    private fun getHoy(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun solicitarMedicion() {
        pulsoChar?.let { characteristic ->
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            val data = "medir".toByteArray()
            writeCharacteristic(characteristic, data).enqueue()
        }
    }
}
