package com.example.appv1.logins

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.example.appv1.admin.HomeAdmin
import com.example.appv1.admin.MainAdministrador
import com.example.appv1.logins.CuidadorLogin
import com.example.appv1.registro.RegistroDeLaOrg
import com.google.firebase.firestore.FirebaseFirestore

class AdminLogin : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPass = findViewById<EditText>(R.id.etPass)
        val tvRegistrar = findViewById<TextView>(R.id.tvRegistrar)
        val btnAccesocuidador = findViewById<Button>(R.id.btncuidadoracceso)

        db = FirebaseFirestore.getInstance()

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPass.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Por favor no deje campos vacíos", Toast.LENGTH_SHORT).show()
            }
        }

        tvRegistrar.setOnClickListener {
            startActivity(Intent(this, RegistroDeLaOrg::class.java))
        }

        btnAccesocuidador.setOnClickListener {
            startActivity(Intent(this, CuidadorLogin::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        val organizacionesRef = db.collection("organizacion")

        organizacionesRef.get()
            .addOnSuccessListener { orgSnapshots ->
                var encontrado = false
                var consultasPendientes = orgSnapshots.size()

                if (consultasPendientes == 0) {
                    Toast.makeText(this, "No hay organizaciones registradas", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (orgDoc in orgSnapshots) {
                    val adminRef = orgDoc.reference.collection("administradores")

                    adminRef.whereEqualTo("email", email)
                        .whereEqualTo("password", password)
                        .get()
                        .addOnSuccessListener { adminSnapshots ->
                            if (!adminSnapshots.isEmpty && !encontrado) {
                                encontrado = true

                                val adminDoc = adminSnapshots.documents[0]
                                val idAdmin = adminDoc.id
                                val idOrganizacion = orgDoc.id

                                // Guardar sesión
                                val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
                                prefs.edit()
                                    .putString("tipo_usuario", "admin")
                                    .putString("id_usuario", idAdmin)
                                    .putString("id_organizacion", idOrganizacion)
                                    .apply()

                                Toast.makeText(this, "Bienvenido, $email", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainAdministrador::class.java))
                                finish()
                            }

                            consultasPendientes--

                            if (consultasPendientes == 0 && !encontrado) {
                                Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            consultasPendientes--
                            if (consultasPendientes == 0 && !encontrado) {
                                Toast.makeText(this, "Error al verificar los datos", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener las organizaciones", Toast.LENGTH_SHORT).show()
            }
    }
}
