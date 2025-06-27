package com.example.appv1.paciente

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.appv1.R
import com.example.appv1.paciente.service.BlePacienteService

class HomePaciente : AppCompatActivity() {

    private lateinit var pulsoView: TextView
    private lateinit var oxigenoView: TextView
    private lateinit var temperaturaView: TextView
    private lateinit var estadoConexionView: TextView
    private lateinit var btnMedir: AppCompatButton

    private val receptor = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pulso = intent?.getIntExtra("pulso", 0) ?: 0
            val oxigeno = intent?.getIntExtra("oxigeno", 0) ?: 0
            val temperatura = intent?.getDoubleExtra("temperatura", 0.0) ?: 0.0

            pulsoView.text = pulso.toString()
            oxigenoView.text = oxigeno.toString()
            temperaturaView.text = "$temperatura °C"
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()

        // Mostrar como desconectado por defecto
        estadoConexionView.text = "Desconectado"
        estadoConexionView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receptor, IntentFilter("DATOS_PULSERA"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receptor, IntentFilter("DATOS_PULSERA"))
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receptor)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_paciente)

        pulsoView = findViewById(R.id.hrtpulso)
        oxigenoView = findViewById(R.id.oxigeno)
        temperaturaView = findViewById(R.id.temperatura)
        estadoConexionView = findViewById(R.id.txtEstadoConexion)
        btnMedir = findViewById(R.id.button2)

        // Valor por defecto al iniciar
        estadoConexionView.text = "Desconectado"
        estadoConexionView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))

        btnMedir.setOnClickListener {
            pedirPermisos()
        }
    }

    private fun pedirPermisos() {
        val permisos = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permisos.add(Manifest.permission.BLUETOOTH_SCAN)
            permisos.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        permisosLauncher.launch(permisos.toTypedArray())
    }

    @SuppressLint("MissingPermission")
    private val permisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado ->
        val todosOtorgados = resultado.entries.all { it.value }

        if (!todosOtorgados) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    escanearYConectar()
                } else {
                    Toast.makeText(this, "Permiso BLUETOOTH_SCAN no otorgado", Toast.LENGTH_SHORT).show()
                }
            } else {
                escanearYConectar()
            }
        } else {
            Toast.makeText(this, "Debes aceptar todos los permisos para conectar la pulsera", Toast.LENGTH_LONG).show()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun escanearYConectar() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val scanner = bluetoothAdapter.bluetoothLeScanner

        val filtro = ScanFilter.Builder()
            .setDeviceName("SCI-BAND paciente")
            .build()

        val settings = ScanSettings.Builder().build()

        scanner.startScan(listOf(filtro), settings, object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                scanner.stopScan(this)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (!this@HomePaciente.isInForeground()) {
                        Toast.makeText(this@HomePaciente, "No se puede iniciar el servicio si la app no está activa", Toast.LENGTH_SHORT).show()
                        return
                    }
                }

                val intent = Intent(this@HomePaciente, BlePacienteService::class.java)
                intent.putExtra("device", result.device)

                ContextCompat.startForegroundService(this@HomePaciente, intent)
                Toast.makeText(this@HomePaciente, "Pulsera conectada", Toast.LENGTH_SHORT).show()


                estadoConexionView.text = "Conectado"
                estadoConexionView.setTextColor(ContextCompat.getColor(this@HomePaciente, android.R.color.holo_green_light))
            }

            override fun onScanFailed(errorCode: Int) {
                Toast.makeText(this@HomePaciente, "Fallo al escanear BLE ($errorCode)", Toast.LENGTH_SHORT).show()
                Log.e("BLE", "Scan falló: $errorCode")


                estadoConexionView.text = "Desconectado"
                estadoConexionView.setTextColor(ContextCompat.getColor(this@HomePaciente, android.R.color.holo_red_light))
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
        })
    }
}
