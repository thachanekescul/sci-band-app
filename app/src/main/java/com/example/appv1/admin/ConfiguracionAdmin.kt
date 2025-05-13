package com.example.appv1.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.MainActivity
import com.example.appv1.R

class ConfiguracionAdmin : AppCompatActivity() {

    private lateinit var btnCerrarSesion: Button
    private lateinit var btnEditarInfo: Button
    private lateinit var btnEliminarCuenta: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion_admin)

        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        btnEditarInfo = findViewById(R.id.btnEditarInfo)
        btnEliminarCuenta = findViewById(R.id.btnEliminarCuenta)

        // Función para cerrar sesión
        btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }

        // Acción para ir a la vista de editar información
        btnEditarInfo.setOnClickListener {
            mostrarDialogoEdicion()
        }

        // Acción para eliminar cuenta
        btnEliminarCuenta.setOnClickListener {
            Toast.makeText(this, "Luego :)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cerrarSesion() {
        val prefs = getSharedPreferences("usuario_sesion", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.clear()
        editor.apply()

        Toast.makeText(this, "Sesión cerrada con éxito", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun mostrarDialogoEdicion() {
        val opciones = arrayOf("Información Personal", "Información de la Empresa")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("¿Qué desea editar?")
        builder.setItems(opciones) { _, which ->
            when (which) {
                0 -> {

                    val intent = Intent(this, EditarInfoAdmin::class.java)
                    startActivity(intent)
                }
                1 -> {

                    val intent = Intent(this, EditarEmpresaAdmin::class.java)
                    startActivity(intent)
                }
            }
        }

        builder.show()
    }
}
