package com.example.appv1.registro

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.example.appv1.cuidador.RegistroDePaciente
import com.google.firebase.firestore.FirebaseFirestore


class RegistroCuidador : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_cuidador)

        db = FirebaseFirestore.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPass = findViewById<EditText>(R.id.etPass)
        val etConfirmar = findViewById<EditText>(R.id.etConfirmar)
        val etOrganizacion = findViewById<EditText>(R.id.etOrganizacion)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellido = findViewById<EditText>(R.id.etApellido)
        val etCelular = findViewById<EditText>(R.id.etCelular)
        val btnRegistro = findViewById<Button>(R.id.btnRegistro)

        btnRegistro.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()
            val confirmar = etConfirmar.text.toString().trim()
            val orgCodigo = etOrganizacion.text.toString().trim()
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val celular = etCelular.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty() || confirmar.isEmpty() ||
                orgCodigo.isEmpty() || nombre.isEmpty() || apellido.isEmpty() || celular.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmar) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val orgRef = db.collection("organizacion").document(orgCodigo)

            orgRef.get().addOnSuccessListener { orgDoc ->
                if (!orgDoc.exists()) {
                    Toast.makeText(this, "La organización no existe", Toast.LENGTH_SHORT).show()
                } else {
                    val passwordEncriptada = encriptarPassword(pass)
                    val cuidadoresRef = orgRef.collection("cuidadores")

                    // Validar si el email ya está registrado
                    cuidadoresRef.whereEqualTo("email", email).get()
                        .addOnSuccessListener { snapshot ->
                            if (!snapshot.isEmpty) {
                                Toast.makeText(this, "El correo ya está registrado", Toast.LENGTH_SHORT).show()
                            } else {
                                val datos = hashMapOf(
                                    "email" to email,
                                    "password" to passwordEncriptada,
                                    "nombre" to nombre,
                                    "ape" to apellido,
                                    "cel" to celular
                                )




                                cuidadoresRef.add(datos)
                                    .addOnSuccessListener {
                                        // Eliminar el placeholder si existe
                                        cuidadoresRef.document("placeholder").get()
                                            .addOnSuccessListener { doc ->
                                                if (doc.exists()) {
                                                    cuidadoresRef.document("placeholder").delete()
                                                }
                                            }
                                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this@RegistroCuidador, RegistroDePaciente::class.java).apply{
                                            putExtra("codigo_org", orgCodigo)
                                        }

                                        startActivity(intent)
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Error al registrar", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al verificar el correo", Toast.LENGTH_SHORT).show()
                        }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error al verificar la organización", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun encriptarPassword(password: String): String {
        val bytes = password.toByteArray()
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
