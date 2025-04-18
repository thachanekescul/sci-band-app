package com.example.appv1.paciente

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class PacienteQR : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var ivQRCode: ImageView
    private lateinit var tvCodigo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paciente_qr)

        db = FirebaseFirestore.getInstance()
        ivQRCode = findViewById(R.id.ivQRCode)
        tvCodigo = findViewById(R.id.tvCodigo)

        // 1. Generar código aleatorio
        val codigo = (100000..999999).random().toString()

        // 2. Guardar en Firestore (colección temporal)
        val qrData = hashMapOf(
            "generado_por" to "paciente_sin_registro", // puedes cambiarlo por deviceId o similar
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("codigos_espera").document(codigo)
            .set(qrData)
            .addOnSuccessListener {


                val bitmap = generarQRBitmap(codigo)
                ivQRCode.setImageBitmap(bitmap)
            }
            .addOnFailureListener {
                tvCodigo.text = "Error al generar código"
            }
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
}