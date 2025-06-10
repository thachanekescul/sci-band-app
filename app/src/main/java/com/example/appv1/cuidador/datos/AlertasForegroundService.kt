package com.example.appv1.cuidador.datos


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.appv1.MainActivity
import com.example.appv1.R
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AlertasForegroundService : Service() {

    private val CHANNEL_ID = "alertas_foreground_channel"
    private val NOTIF_ID = 4321

    private lateinit var database: DatabaseReference
    private lateinit var alertasRef: DatabaseReference
    private lateinit var prefs: android.content.SharedPreferences
    private var cuidadorId: String? = null
    private lateinit var alertListener: ChildEventListener
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, createNotification())

        prefs = getSharedPreferences("usuario_sesion", Context.MODE_PRIVATE)
        cuidadorId = prefs.getString("cuidador_id", null)

        database = FirebaseDatabase.getInstance().reference
        alertasRef = database.child("alertas")

        startListeningAlertas()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SCI-BAND: Escuchando alertas")
            .setContentText("El servicio está activo para recibir alertas")
            .setSmallIcon(R.drawable.ic_corazon) // Cambia por tu ícono
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas en primer plano",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startListeningAlertas() {
        alertasRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                manejarAlerta(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                manejarAlerta(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun manejarAlerta(snapshot: DataSnapshot) {
        val pacienteId = snapshot.key ?: return
        val alerta = snapshot.getValue(Boolean::class.java) ?: false
        if (alerta) {
            mostrarNotificacionAlerta(pacienteId)
            // Eliminar alerta para que no se repita
            alertasRef.child(pacienteId).removeValue()
        }
    }

    private fun mostrarNotificacionAlerta(pacienteId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("paciente_id", pacienteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            pacienteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_displash) // Cambia por tu ícono
            .setContentTitle("Alerta de paciente")
            .setContentText("El paciente $pacienteId necesita ayuda")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(pacienteId.hashCode(), notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        // Eliminar correctamente el listener con la instancia guardada
        if (this::alertListener.isInitialized) {
            alertasRef.removeEventListener(alertListener)
        }
}}
