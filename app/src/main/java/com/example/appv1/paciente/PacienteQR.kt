package com.example.appv1.paciente

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class PacienteQR : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var ivQRCode: ImageView
    private lateinit var tvCodigo: TextView
    private lateinit var etCodigoPaciente: EditText
    private lateinit var btnIngresarPaciente: Button
    private lateinit var progressBar: ProgressBar
    private var listener: ListenerRegistration? = null
    private var codigoGenerado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paciente_qr)

        db = FirebaseFirestore.getInstance()
        ivQRCode = findViewById(R.id.ivQRCode)
        tvCodigo = findViewById(R.id.tvCodigo)
        etCodigoPaciente = findViewById(R.id.etCodigoPaciente)
        btnIngresarPaciente = findViewById(R.id.btnIngresarPaciente)
        progressBar = findViewById(R.id.progressBar)

        // Generar código aleatorio de 6 dígitos
        codigoGenerado = (100000..999999).random().toString()
        tvCodigo.text = "Código: $codigoGenerado"

        val qrData = hashMapOf(
            "generado_por" to "paciente_sin_registro",
            "timestamp" to System.currentTimeMillis(),
            "escaneado" to false
        )

        // Guardar QR en Firestore
        db.collection("codigos_espera").document(codigoGenerado!!)
            .set(qrData)
            .addOnSuccessListener {
                val bitmap = generarQRBitmap(codigoGenerado!!)
                ivQRCode.setImageBitmap(bitmap)

                // Escuchar si fue escaneado
                listener = db.collection("codigos_espera").document(codigoGenerado!!)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        val escaneado = snapshot.getBoolean("escaneado") ?: false
                        if (escaneado) {
                            listener?.remove()
                            irAPermisoBluetooth()
                        }
                    }
            }
            .addOnFailureListener {
                tvCodigo.text = "Error al generar código"
            }

        // Lógica del botón de "Ingresar"
        btnIngresarPaciente.setOnClickListener {
            val codigoIngresado = etCodigoPaciente.text.toString().trim()
            if (codigoIngresado.isEmpty()) {
                etCodigoPaciente.error = "Ingresa tu código"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE

            db.collection("organizacion").get()
                .addOnSuccessListener { organizaciones ->
                    var encontrado = false

                    for (org in organizaciones) {
                        val idOrg = org.id
                        db.collection("organizacion").document(idOrg)
                            .collection("cuidadores").get()
                            .addOnSuccessListener { cuidadores ->
                                for (cuidador in cuidadores) {
                                    val idCuidador = cuidador.id
                                    db.collection("organizacion").document(idOrg)
                                        .collection("cuidadores").document(idCuidador)
                                        .collection("pacientes").document(codigoIngresado)
                                        .get()
                                        .addOnSuccessListener { docPaciente ->
                                            if (docPaciente.exists()) {

                                                val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
                                                prefs.edit()
                                                    .putString("tipo_usuario", "paciente")
                                                    .putString("id_usuario", codigoIngresado)
                                                    .putString("id_organizacion", idOrg)
                                                    .apply()


                                                codigoGenerado?.let {
                                                    db.collection("codigos_espera").document(it).delete()
                                                }

                                                progressBar.visibility = View.GONE
                                                irAPermisoBluetooth()
                                                encontrado = true
                                            }
                                        }
                                }
                            }
                    }

                    if (encontrado == false) {
                        progressBar.visibility = View.GONE
                        etCodigoPaciente.error = "No se encontró el paciente"
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    etCodigoPaciente.error = "Error al buscar paciente"
                }
        }
    }

    private fun irAPermisoBluetooth() {
        val intent = Intent(this, HomePaciente::class.java)
        startActivity(intent)
        finish()
    }

    private fun generarQRBitmap(text: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bitmap
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
        codigoGenerado?.let {
            db.collection("codigos_espera").document(it).delete()
        }
    }
}
