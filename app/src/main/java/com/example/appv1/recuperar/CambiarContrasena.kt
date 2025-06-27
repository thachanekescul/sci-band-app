package com.example.appv1.recuperar

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.google.firebase.firestore.FirebaseFirestore

class CambiarContrasena : AppCompatActivity() {

    private lateinit var edtNombre: EditText
    private lateinit var edtCelular: EditText
    private lateinit var edtNuevaContrasena: EditText
    private lateinit var edtConfirmarNuevaContrasena: EditText
    private lateinit var btnCambiar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var correo: String
    private lateinit var tipoUsuario: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cambiar_contrasena)

        edtNombre = findViewById(R.id.edtNombreCambiar)
        edtCelular = findViewById(R.id.edtCelularCambiar)
        edtNuevaContrasena = findViewById(R.id.edtNuevaContrasena)
        edtConfirmarNuevaContrasena = findViewById(R.id.edtConfirmarNuevaContrasena)
        btnCambiar = findViewById(R.id.btnCambiar)
        progressBar = findViewById(R.id.progressBar)

        correo = intent.getStringExtra("correo") ?: ""
        tipoUsuario = intent.getStringExtra("tipoUsuario") ?: ""

        btnCambiar.setOnClickListener {
            val nombre = edtNombre.text.toString().trim()
            val celular = edtCelular.text.toString().trim()
            val nuevaContrasena = edtNuevaContrasena.text.toString().trim()
            val confirmarContrasena = edtConfirmarNuevaContrasena.text.toString().trim()

            if (nombre.isEmpty() || celular.isEmpty() || nuevaContrasena.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nuevaContrasena != confirmarContrasena) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            buscarUsuarioYActualizar(nombre, celular, nuevaContrasena)
        }
    }

    private fun buscarUsuarioYActualizar(nombre: String, celular: String, nuevaContrasena: String) {
        val db = FirebaseFirestore.getInstance()
        val organizacionRef = db.collection("organizacion")

        organizacionRef.get().addOnSuccessListener { orgSnap ->
            for (orgDoc in orgSnap.documents) {
                val orgId = orgDoc.id
                val ruta = if (tipoUsuario == "Administradores") "administradores" else "cuidadores"

                db.collection("organizacion").document(orgId)
                    .collection(ruta)
                    .whereEqualTo("email", correo)
                    .get()
                    .addOnSuccessListener { userSnap ->
                        if (!userSnap.isEmpty) {
                            val userDoc = userSnap.documents[0]
                            val userId = userDoc.id
                            val campoCel = if (tipoUsuario == "Cuidadores") "cel" else "celular"

                            val nombreDB = userDoc.getString("nombre") ?: ""
                            val celDB = userDoc.getString(campoCel) ?: ""

                            if (nombreDB == nombre && celDB == celular) {
                                db.collection("organizacion").document(orgId)
                                    .collection(ruta).document(userId)
                                    .update("password", nuevaContrasena)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                                        progressBar.visibility = View.GONE
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Error al actualizar contraseña", Toast.LENGTH_SHORT).show()
                                        progressBar.visibility = View.GONE
                                    }
                            } else {
                                Toast.makeText(this, "Datos incorrectos", Toast.LENGTH_SHORT).show()
                                progressBar.visibility = View.GONE
                            }
                         } else {
                            progressBar.visibility = View.GONE
                        }
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al buscar usuario", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
        }
    }
}