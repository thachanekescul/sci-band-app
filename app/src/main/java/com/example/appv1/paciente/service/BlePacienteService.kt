package com.example.appv1.paciente.service





import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar
import java.util.UUID

class BlePacienteService : Service() {

    private var gatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())

    private var pulso = 0
    private var oxigeno = 0
    private var temperatura = 0.0

    private var llamado = false
    private var alerta = false
    private var desconectado = false
    private var botonEmergencia = false

    private val pulsoUUID = UUID.fromString("12345678-1234-1234-1234-123456789abd")
    private val oxigenoUUID = UUID.fromString("12345678-1234-1234-1234-123456789abe")
    private val tempUUID = UUID.fromString("12345678-1234-1234-1234-123456789abf")
    private val serviceUUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        val device = intent?.getParcelableExtra<BluetoothDevice>("device")
        device?.connectGatt(this, false, gattCallback)
        handler.post(updateRunnable)
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "ble_channel"
        val channelName = "BLE Servicio"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pulsera conectada")
            .setContentText("Recolectando datos")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        startForeground(1, notification)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
                desconectado = false
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                desconectado = true
                Log.e("BLE", "Pulsera desconectada")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(serviceUUID)
            listOf(pulsoUUID, oxigenoUUID, tempUUID).forEach { uuid ->
                val ch = service.getCharacteristic(uuid)
                gatt.setCharacteristicNotification(ch, true)
                val descriptor = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            when (ch.uuid) {
                pulsoUUID -> {
                    val valor = ch.getStringValue(0)
                    when (valor) {
                        "llamado" -> llamado = true
                        "emergencia" -> botonEmergencia = true
                        else -> pulso = valor.toIntOrNull() ?: 0
                    }
                }
                oxigenoUUID -> oxigeno = ch.getStringValue(0).toIntOrNull() ?: 0
                tempUUID -> {
                    val raw = ByteBuffer.wrap(ch.value).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                    temperatura = raw / 100.0
                }
            }

            // Opcional: broadcast a UI
            val intent = Intent("DATOS_PULSERA")
            intent.putExtra("pulso", pulso)
            intent.putExtra("oxigeno", oxigeno)
            intent.putExtra("temperatura", temperatura)
            sendBroadcast(intent)
        }
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            subirUltimosDatos()
            subirCada30Min()
            handler.postDelayed(this, 5 * 60 * 1000)
        }
    }

    private fun subirUltimosDatos() {
        val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
        val idPaciente = prefs.getString("id_paciente", null) ?: return

        alerta = desconectado || botonEmergencia || datosCriticos()

        val data = hashMapOf(
            "p" to pulso,
            "o" to oxigeno,
            "t" to temperatura,
            "llamado" to llamado,
            "alerta" to alerta
        )

        FirebaseFirestore.getInstance()
            .collection("ultimos-datos")
            .document(idPaciente)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { Log.d("BLE", "Ultimos-datos subido") }
            .addOnFailureListener { Log.e("BLE", "Error al subir ultimos-datos", it) }

        llamado = false
        alerta = false
        botonEmergencia = false
    }

    private fun subirCada30Min() {
        val cal = Calendar.getInstance()
        val min = cal.get(Calendar.MINUTE)
        if (min != 0 && min != 30) return

        val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
        val idOrg = prefs.getString("id_organizacion", null) ?: return
        val idPaciente = prefs.getString("id_usuario", null) ?: return
        val hora = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))

        val db = FirebaseFirestore.getInstance()
        db.collection("organizacion").document(idOrg)
            .collection("cuidadores")
            .get()
            .addOnSuccessListener { cuidadores ->
                for (cuidador in cuidadores) {
                    val idCuidador = cuidador.id
                    db.collection("organizacion").document(idOrg)
                        .collection("cuidadores").document(idCuidador)
                        .collection("pacientes").document(idPaciente)
                        .get()
                        .addOnSuccessListener { paciente ->
                            if (paciente.exists()) {
                                val ruta = "organizacion/$idOrg/cuidadores/$idCuidador/pacientes/$idPaciente/bio-datosprm/$hora"
                                val datos = hashMapOf(
                                    "p" to pulso,
                                    "o" to oxigeno,
                                    "t" to temperatura
                                )
                                db.document(ruta)
                                    .set(datos, SetOptions.merge())
                                    .addOnSuccessListener { Log.d("BLE", "Bio-datos subido") }
                                    .addOnFailureListener { Log.e("BLE", "Error al subir bio-datos", it) }
                            }
                        }
                }
            }
            .addOnFailureListener {
                Log.e("BLE", "Error al buscar cuidadores", it)
            }
    }


    private fun datosCriticos(): Boolean {
        return pulso < 40 || pulso > 150 || oxigeno < 85 || temperatura < 35.0 || temperatura > 39.5
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        gatt?.close()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
