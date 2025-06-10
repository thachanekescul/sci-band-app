package com.example.appv1.recuperar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.functions.FirebaseFunctions
import java.io.StringWriter

class RecuperarContrasena : AppCompatActivity() {

    private lateinit var edtCorreo: EditText
    private lateinit var spTipoUsuario: Spinner
    private lateinit var btnEnviarCodigo: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_contrasena)

        // Initialize App Check
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        edtCorreo = findViewById(R.id.edtCorreo)
        spTipoUsuario = findViewById(R.id.spTipoUsuario)
        btnEnviarCodigo = findViewById(R.id.btnEnviarCodigo)
        progressBar = findViewById(R.id.progressBar)

        // Llenar el Spinner con opciones de usuario
        val userTypes = arrayOf("Administradores", "Cuidadores")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipoUsuario.adapter = adapter

        btnEnviarCodigo.setOnClickListener {
            val correo = edtCorreo.text.toString().trim()
            val tipoUsuario = spTipoUsuario.selectedItem?.toString()?.trim() ?: ""

            Log.d("RecuperarContrasena", "Correo: $correo")
            Log.d("RecuperarContrasena", "Tipo de usuario: $tipoUsuario")

            if (correo.isEmpty() || tipoUsuario.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese correo y tipo de usuario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendVerificationCode(correo, tipoUsuario)
        }
    }

    private fun sendVerificationCode(correo: String, tipoUsuario: String) {
        val functions = FirebaseFunctions.getInstance("us-central1") // Replace with your region
        val data = hashMapOf(
            "email" to correo,
            "userType" to tipoUsuario
        )
        val jsonData = try {
            android.util.JsonWriter(StringWriter()).use { writer ->
                writer.beginObject()
                data.forEach { (key, value) ->
                    writer.name(key).value(value.toString())
                }
                writer.endObject()
            }.toString()
        } catch (e: Exception) {
            Log.e("RecuperarContrasena", "Error serializing data: ${e.message}", e)
            "{}"
        }
        Log.d("RecuperarContrasena", "Serialized data: $jsonData")
        Log.d("RecuperarContrasena", "Enviando datos a Firebase Functions: email = $correo, userType = $tipoUsuario")

        progressBar.visibility = View.VISIBLE
        functions.getHttpsCallable("sendVerificationCode")
            .call(data)
            .addOnSuccessListener { result ->
                val success = result.data as Map<*, *>
                if (success["success"] == true) {
                    Toast.makeText(this, "Código enviado con éxito", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, CambiarContrasena::class.java)
                    intent.putExtra("correo", correo)
                    intent.putExtra("tipoUsuario", tipoUsuario)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Error: ${success["message"]}", Toast.LENGTH_SHORT).show()
                    Log.e("RecuperarContrasena", "Error: ${success["message"]}")
                }
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                Log.e("RecuperarContrasena", "Error al comunicarse con el servidor: ${exception.message}", exception)
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }
}