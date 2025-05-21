package com.example.appv1.registro

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.appv1.R

import android.widget.*

import com.google.firebase.firestore.FirebaseFirestore


class ConfirmarDatosAdmin : AppCompatActivity() {

    private lateinit var tvDatosCompletos: TextView
    private lateinit var btnRegistro: Button

    // Firestore
    private val db = FirebaseFirestore.getInstance()
    // Generar código único para la organización

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmar_datos_admin)

        tvDatosCompletos = findViewById(R.id.etDatosCompletos)
        btnRegistro = findViewById(R.id.btnRegistro)
        val codigoOrg = generarCodigoOrganizacion()
        // Obtener los datos
        val nombreOrg = intent.getStringExtra("nombre_org") ?: ""
        val direccion = intent.getStringExtra("direccion") ?: ""
        val celularOrg = intent.getStringExtra("celular") ?: ""
        val fechaFundacion = intent.getStringExtra("fecha_fundacion") ?: ""

        val adminEmail = intent.getStringExtra("admin_email") ?: ""
        val adminNombre = intent.getStringExtra("admin_nombre") ?: ""
        val adminApellido = intent.getStringExtra("admin_apellido") ?: ""
        val adminCelular = intent.getStringExtra("admin_celular") ?: ""


        val password = intent.getStringExtra("admin_password") ?: ""


        // Mostrar todos los datos en un solo TextView
        val datosFinales = """
            DATOS DE LA ORGANIZACIÓN
            
            Nombre: $nombreOrg
            Dirección: $direccion
            Celular: $celularOrg
            Fundación: $fechaFundacion

            ADMINISTRADOR RESPONSABLE
            Email: $adminEmail
            Nombre: $adminNombre $adminApellido
            Celular: $adminCelular

            Código de la organización: $codigoOrg
            Guarde este codigo que es importrante
                    """.trimIndent()

        tvDatosCompletos.text = datosFinales

        btnRegistro.setOnClickListener {
            val orgRef = db.collection("organizacion").document(codigoOrg)

            val datosOrganizacion = hashMapOf(
                "nombre" to nombreOrg,
                "direccion" to direccion,
                "celular" to celularOrg,
                "fecha_fundacion" to fechaFundacion
            )

            val datosAdmin = hashMapOf(
                "email" to adminEmail,
                "nombre" to adminNombre,
                "apellido" to adminApellido,
                "celular" to adminCelular,
                "password" to password

            )

            orgRef.set(datosOrganizacion)
            orgRef.collection("administradores").add(datosAdmin)

                .addOnSuccessListener {
                    // Crear subcolección cuidadores (vacía con documento inicial)
                    orgRef.collection("cuidadores")
                        .document("placeholder")
                        .set(hashMapOf("init" to true))
                        .addOnSuccessListener {
                            Toast.makeText(this, "Organización registrada correctamente 🎉", Toast.LENGTH_LONG).show()
                            finish() // o redirige a otra pantalla
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al crear cuidadores", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar administrador", Toast.LENGTH_LONG).show()
                }
        }



    }


    private fun generarCodigoOrganizacion(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
