package com.example.appv1.paciente

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appv1.R

class BlueToothPaciente : AppCompatActivity() {

    private lateinit var tvBluetoothInfo: TextView

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permiso concedido, ir a HomePaciente
                startActivity(Intent(this, HomePaciente::class.java))
                finish()
            } else {
                tvBluetoothInfo.text = "Permiso de Bluetooth es necesario para usar la pulsera."
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blue_tooth_paciente) // Usa tu layout con tvBluetoothInfo

        tvBluetoothInfo = findViewById(R.id.tvBluetoothInfo)

        // Mensaje simple
        tvBluetoothInfo.text = "Para usar esta aplicación necesitas tener Bluetooth activado. " +
                "Si no lo activas, no se podrá utilizar la pulsera del paciente."

        pedirPermisoBluetooth()
    }

    private fun pedirPermisoBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                // Ya tiene permiso, pasa a HomePaciente
                startActivity(Intent(this, HomePaciente::class.java))
                finish()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            // Versiones anteriores no necesitan permiso
            startActivity(Intent(this, HomePaciente::class.java))
            finish()
        }
    }
}
