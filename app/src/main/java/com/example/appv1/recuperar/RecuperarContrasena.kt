package com.example.appv1.recuperar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.functions.FirebaseFunctions

class RecuperarContrasena : AppCompatActivity() {

    private lateinit var edtCorreo: EditText
    private lateinit var spTipoUsuario: Spinner
    private lateinit var btnEnviarCodigo: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_contrasena)

        edtCorreo = findViewById(R.id.edtCorreo)
        spTipoUsuario = findViewById(R.id.spTipoUsuario)
        btnEnviarCodigo = findViewById(R.id.btnEnviarCodigo)
        progressBar = findViewById(R.id.progressBar)

        // Llenar el Spinner con opciones de usuario
        val userTypes = arrayOf("Administrador", "Cuidador") // Usar valores más claros para el backend
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userTypes)
        spTipoUsuario.adapter = adapter

        btnEnviarCodigo.setOnClickListener {
            val correo = edtCorreo.text.toString().trim()
            val tipoUsuario = spTipoUsuario.selectedItem.toString().trim()

            // Depuración: Verifica el correo y el tipo de usuario
            Log.d("RecuperarContrasena", "Correo: $correo")
            Log.d("RecuperarContrasena", "Tipo de usuario: $tipoUsuario")

            // Verifica si el correo y el tipo de usuario son válidos
            if (correo.isNotEmpty() && tipoUsuario.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE
                sendVerificationCode(correo, tipoUsuario)
            } else {
                Toast.makeText(this, "Por favor, ingrese un correo válido y seleccione un tipo de usuario.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendVerificationCode(correo: String, tipoUsuario: String) {
        val functions = FirebaseFunctions.getInstance()

        val data = hashMapOf(
            "email" to correo,
            "userType" to tipoUsuario
        )

        Log.d("RecuperarContrasena", "Enviando datos a Firebase Functions: email = $correo, userType = $tipoUsuario")

        functions.getHttpsCallable("sendVerificationCode")
            .call(data)
            .addOnSuccessListener { result ->
                val success = result.data as Map<*, *>
                if (success["success"] == true) {
                    Toast.makeText(this, "Código enviado con éxito", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    // Ir a la segunda vista
                    val intent = Intent(this, CambiarContrasena::class.java)
                    intent.putExtra("correo", correo)
                    intent.putExtra("tipoUsuario", tipoUsuario)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Error al enviar el código: ${success["message"]}", Toast.LENGTH_SHORT).show()
                    Log.e("RecuperarContrasena", "Error al enviar el código: ${success["message"]}")
                    progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                // Imprime el error completo en los logs
                exception.printStackTrace()

                // Muestra el mensaje del error en el Toast
                Log.e("RecuperarContrasena", "Error al comunicarse con el servidor: ${exception.message}", exception)

                // Oculta el ProgressBar
                progressBar.visibility = View.GONE
            }
    }
}
