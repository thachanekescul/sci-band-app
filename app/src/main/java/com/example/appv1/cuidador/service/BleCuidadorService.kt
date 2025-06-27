package com.example.appv1.cuidador.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID

class BleCuidadorService : Service() {

    private var gatt: BluetoothGatt? = null
    private var bleCharacteristic: BluetoothGattCharacteristic? = null
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val firestore = FirebaseFirestore.getInstance()

    private val pacientesAsignados = mutableListOf<String>()
    private val firestoreListeners = mutableListOf<ListenerRegistration>()

    private val serviceUUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
    private val pulsoUUID = UUID.fromString("12345678-1234-1234-1234-123456789abd")

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        conectarBLE(intent?.getParcelableExtra("device"))
        cargarPacientesYMonitorear()
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "cuidador_ble"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Pulsera cuidador", NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Pulsera conectada")
            .setContentText("Monitoreando pacientes asignados...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        startForeground(2, notification)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun conectarBLE(device: BluetoothDevice?) {
        if (device == null) {
            Log.e("BLE", "Dispositivo BLE nulo")
            stopSelf()
            return
        }

        gatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("BLE", "Conectado a pulsera cuidador")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e("BLE", "Desconectado")
                    stopSelf()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val service = gatt.getService(serviceUUID)
                bleCharacteristic = service?.getCharacteristic(pulsoUUID)
                Log.d("BLE", "Servicio y característica BLE listos")
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun cargarPacientesYMonitorear() {
        val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
        val idOrg = prefs.getString("id_organizacion", null) ?: return
        val idCuidador = prefs.getString("id_usuario", null) ?: return

        firestore.collection("organizacion").document(idOrg)
            .collection("cuidadores").document(idCuidador)
            .collection("pacientes")
            .get()
            .addOnSuccessListener { snapshot ->
                pacientesAsignados.clear()
                firestoreListeners.forEach { it.remove() }
                firestoreListeners.clear()

                snapshot.forEach { doc ->
                    val pacienteId = doc.id
                    pacientesAsignados.add(pacienteId)

                    val reg = firestore.collection("ultimos-datos").document(pacienteId)
                        .addSnapshotListener { snap, error ->
                            if (error != null || snap == null || !snap.exists()) return@addSnapshotListener

                            val llamado = snap.getBoolean("llamado") ?: false
                            val alerta = snap.getBoolean("alerta") ?: false

                            if (alerta) enviarVibracion(3)
                            else if (llamado) enviarVibracion(1)
                        }

                    firestoreListeners.add(reg)
                }

                Log.d("BLE_CUIDADOR", "Pacientes suscritos: ${pacientesAsignados.size}")
            }
            .addOnFailureListener {
                Log.e("BLE_CUIDADOR", "Error cargando pacientes", it)
            }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enviarVibracion(repeticiones: Int) {
        val mensaje = when (repeticiones) {
            1 -> "llamado"
            3 -> "emergencia"
            else -> return
        }

        val ch = bleCharacteristic ?: return
        ch.setValue(mensaje)
        val success = gatt?.writeCharacteristic(ch) ?: false
        Log.d("BLE_CUIDADOR", "Enviado a pulsera: $mensaje (success=$success)")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        gatt?.close()
        firestoreListeners.forEach { it.remove() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
