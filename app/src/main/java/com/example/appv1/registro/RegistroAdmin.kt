package com.example.appv1.registro

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R
import java.util.*
import com.example.appv1.registro.ConfirmarDatosAdmin
import com.google.firebase.firestore.FirebaseFirestore

class RegistroAdmin : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var etEmail: EditText
    private lateinit var etPass: EditText
    private lateinit var etConfirmar: EditText
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etCelular: EditText
    private lateinit var btnRegistro: Button

    // Datos de la organización recibidos
    private lateinit var nombreOrg: String
    private lateinit var direccionOrg: String
    private lateinit var celularOrg: String
    private lateinit var fechaFundacion: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_admin)
        db = FirebaseFirestore.getInstance()
        // Obtener datos de la organización
        nombreOrg = intent.getStringExtra("nombre_org") ?: ""
        direccionOrg = intent.getStringExtra("direccion") ?: ""
        celularOrg = intent.getStringExtra("celular") ?: ""
        fechaFundacion = intent.getStringExtra("fecha_fundacion") ?: ""

        // Referencias UI
        etEmail = findViewById(R.id.etEmail)
        etPass = findViewById(R.id.etPass)
        etConfirmar = findViewById(R.id.etConfirmar)
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCelular = findViewById(R.id.etCelular)
        btnRegistro = findViewById(R.id.btnRegistro)

        btnRegistro.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPass.text.toString().trim()
            val confirmar = etConfirmar.text.toString().trim()
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val celular = etCelular.text.toString().trim()

            // Validaciones
            if (email.isEmpty() || pass.isEmpty() || confirmar.isEmpty() || nombre.isEmpty() || apellido.isEmpty() || celular.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Correo electrónico inválido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmar) {
                Toast.makeText(this, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val organizacionesRef = db.collection("organizacion")

            organizacionesRef.get()
                .addOnSuccessListener { orgSnapshots ->
                    var encontrado = false
                    var consultasPendientes = orgSnapshots.size()


                    for (orgDoc in orgSnapshots) {
                        val adminRef = orgDoc.reference.collection("administradores")

                        adminRef.whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener { adminSnapshots ->
                                if (!adminSnapshots.isEmpty && !encontrado) {
                                    Toast.makeText(this, "El correo ya existe", Toast.LENGTH_SHORT)
                                        .show()

                                } else {
                                    val intent = Intent(
                                        this@RegistroAdmin,
                                        ConfirmarDatosAdmin::class.java
                                    ).apply {
                                        putExtra("nombre_org", nombreOrg)
                                        putExtra("direccion", direccionOrg)
                                        putExtra("celular", celularOrg)
                                        putExtra("fecha_fundacion", fechaFundacion)

                                        putExtra("admin_email", email)
                                        putExtra("admin_nombre", nombre)
                                        putExtra("admin_apellido", apellido)
                                        putExtra("admin_celular", celular)
                                        putExtra("admin_password", pass)


                                    }

                                    startActivity(intent)
                                }
                            }
                    }


                }
        }
    }
}