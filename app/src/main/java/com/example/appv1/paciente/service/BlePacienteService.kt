package com.example.appv1.paciente.service



import android.app.*
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.appv1.paciente.manejodatos.BlePacienteManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class BlePacienteService : Service() {

    private lateinit var bleManager: BlePacienteManager
    private lateinit var pacienteId: String
    private var scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())

        val prefs = getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        pacienteId = prefs.getString("id_usuario", "sin_id") ?: "sin_id"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val device = intent?.getParcelableExtra<BluetoothDevice>("device")
        device?.let {
            bleManager = BlePacienteManager(this, pacienteId)
            bleManager.connect(it)
                .retry(3, 100)
                .useAutoConnect(true)
                .enqueue()

            // Observar datos recibidos y guardar en Firestore
            scope.launch {
                bleManager.sensorDataFlow.collectLatest { data ->
                    data?.let { bleManager.guardarEnFirestore(it) }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "ble_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Monitoreo BLE",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitoreo de salud activo")
            .setContentText("Recolectando datos del paciente en segundo plano")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()
    }
}
