package com.example.appv1.cuidador

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class RegistroDePaciente : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var btnEscanear: Button

    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val codigo = result.contents
            verificarCodigo(codigo)
        } else {
            Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_de_paciente)

        db = FirebaseFirestore.getInstance()
        btnEscanear = findViewById(R.id.btnEscanear)

        btnEscanear.setOnClickListener {

            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Escanea el código QR")
            options.setCameraId(0)
            options.setBeepEnabled(true)
            options.setBarcodeImageEnabled(true)

            qrLauncher.launch(options)
        }
    }

    private fun verificarCodigo(codigo: String) {
        val orgCodigo = intent.getStringExtra("codigo_org") ?: ""
        db.collection("codigos_espera").document(codigo).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // Código válido → iniciar registro del paciente
                    val intent = Intent(this@RegistroDePaciente , FormularioPaciente::class.java).apply{
                    putExtra("codigo_qr", codigo)
                    putExtra("codigo_org", orgCodigo)


                    }
                    startActivity(intent)


                } else {
                    Toast.makeText(this, "Código inválido o expirado", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar el código", Toast.LENGTH_SHORT).show()
            }
    }
}