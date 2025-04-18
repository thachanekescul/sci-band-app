package com.example.appv1.logins
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseUser

import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import com.example.appv1.admin.HomeAdmin
import com.example.appv1.cuidador.MainActivityCuidador
import com.example.appv1.registro.RegistroAdmin
import com.example.appv1.registro.RegistroCuidador
import com.example.appv1.registro.RegistroDeLaOrg


class AdminLogin : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPass = findViewById<EditText>(R.id.etPass)
        val tvRegistrar = findViewById<TextView>(R.id.tvRegistrar)
        val btnAccesocuidador=findViewById<Button>(R.id.btncuidadoracceso)
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
            val intent = Intent(this@AdminLogin, RegistroDeLaOrg::class.java)
            startActivity(intent)
        }



        btnAccesocuidador.setOnClickListener {
            val intent = Intent(this@AdminLogin, CuidadorLogin::class.java)
            startActivity(intent)
        }

    }



    private fun loginUser(email: String, password: String) {
        val organizacionesRef = db.collection("organizacion") // Asegúrate del nombre exacto: "organizacion", no "organizaciones"

        organizacionesRef.get()
            .addOnSuccessListener { orgSnapshots ->
                var encontrado = false
                var consultasPendientes = orgSnapshots.size()

                if (consultasPendientes == 0) {
                    Toast.makeText(this@AdminLogin, "No hay organizaciones registradas", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (orgDoc in orgSnapshots) {
                    val Adminref = orgDoc.reference.collection("administradores")

                    Adminref.whereEqualTo("email", email)
                        .whereEqualTo("password", password)
                        .get()
                        .addOnSuccessListener { AdminSnapshots ->
                            if (!AdminSnapshots.isEmpty && !encontrado) {
                                encontrado = true
                                Toast.makeText(this@AdminLogin, "Bienvenido, $email", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivityCuidador::class.java))
                            }

                            consultasPendientes--

                            if (consultasPendientes == 0 && !encontrado) {
                                Toast.makeText(this@AdminLogin, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            consultasPendientes--
                            if (consultasPendientes == 0 && !encontrado) {
                                Toast.makeText(this@AdminLogin, "Error al verificar los datos", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this@AdminLogin, "Error al obtener las organizaciones", Toast.LENGTH_SHORT).show()
            }
    }
}

