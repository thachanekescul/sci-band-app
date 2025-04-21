package com.example.appv1.logins

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.example.appv1.cuidador.MainActivityCuidador
import com.example.appv1.registro.RegistroCuidador
import com.google.firebase.firestore.FirebaseFirestore



class CuidadorLogin : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnaccesoadmin: Button
    private lateinit var tvRegistrar: TextView

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cuidador_login)

        db = FirebaseFirestore.getInstance()

        tvRegistrar = findViewById(R.id.tvRegistrar)
        etEmail = findViewById(R.id.etEmail)
        etPass = findViewById(R.id.etPass)
        btnLogin = findViewById(R.id.btnLogin)
        btnaccesoadmin = findViewById(R.id.btnadminacceso)

        tvRegistrar.setOnClickListener {
            startActivity(Intent(this, RegistroCuidador::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPass.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese ambos campos", Toast.LENGTH_SHORT).show()
            } else {
                loginCuidador(email, password)
            }
        }

        btnaccesoadmin.setOnClickListener {
            startActivity(Intent(this, AdminLogin::class.java))
        }
    }

    private fun loginCuidador(email: String, password: String) {
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
                    val cuidadoresRef = orgDoc.reference.collection("cuidadores")

                    cuidadoresRef
                        .whereEqualTo("email", email)
                        .whereEqualTo("password", password)
                        .get()
                        .addOnSuccessListener { cuidadorSnapshots ->
                            if (!cuidadorSnapshots.isEmpty && !encontrado) {
                                encontrado = true
                                val cuidadorDoc = cuidadorSnapshots.documents[0]
                                val idCuidador = cuidadorDoc.id
                                val idOrganizacion = orgDoc.id

                                // ✅ GUARDAR SESIÓN UNIFICADA
                                val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
                                prefs.edit()
                                    .putString("tipo_usuario", "cuidador")
                                    .putString("id_usuario", idCuidador)
                                    .putString("id_organizacion", idOrganizacion)
                                    .apply()

                                Toast.makeText(this, "Bienvenido, $email", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivityCuidador::class.java))
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
