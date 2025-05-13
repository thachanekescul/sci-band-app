package com.example.appv1.recuperar

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.functions.FirebaseFunctions

class CambiarContrasena : AppCompatActivity() {

    private lateinit var edtCodigo: EditText
    private lateinit var edtNuevaContrasena: EditText
    private lateinit var edtConfirmarNuevaContrasena: EditText
    private lateinit var btnCambiar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var correo: String
    private lateinit var tipoUsuario: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cambiar_contrasena)

        edtCodigo = findViewById(R.id.edtCodigo)
        edtNuevaContrasena = findViewById(R.id.edtNuevaContrasena)
        edtConfirmarNuevaContrasena = findViewById(R.id.edtConfirmarNuevaContrasena)
        btnCambiar = findViewById(R.id.btnCambiar)
        progressBar = findViewById(R.id.progressBar)

        // Recibir datos de la actividad anterior
        correo = intent.getStringExtra("correo").toString()
        tipoUsuario = intent.getStringExtra("tipoUsuario").toString()

        btnCambiar.setOnClickListener {
            val codigo = edtCodigo.text.toString().trim()
            val nuevaContrasena = edtNuevaContrasena.text.toString().trim()
            val confirmarContrasena = edtConfirmarNuevaContrasena.text.toString().trim()

            if (nuevaContrasena == confirmarContrasena) {
                progressBar.visibility = View.VISIBLE
                changePassword(codigo, nuevaContrasena)
            } else {
                Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changePassword(codigo: String, nuevaContrasena: String) {
        val functions = FirebaseFunctions.getInstance()

        val data = hashMapOf(
            "email" to correo,
            "code" to codigo,
            "newPassword" to nuevaContrasena,
            "userType" to tipoUsuario
        )

        functions.getHttpsCallable("changePassword")
            .call(data)
            .addOnSuccessListener { result ->
                val success = result.data as Map<*, *>
                if (success["success"] == true) {
                    Toast.makeText(this, "Contraseña cambiada con éxito", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    // Volver a la pantalla de login o al inicio
                    finish()
                } else {
                    Toast.makeText(this, "Código inválido o error al cambiar la contraseña", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al comunicarse con el servidor", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }
}
