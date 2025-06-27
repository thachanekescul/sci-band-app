package com.example.appv1.recuperar

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appv1.R

class RecuperarContrasena : AppCompatActivity() {

    private lateinit var edtCorreo: EditText
    private lateinit var spTipoUsuario: Spinner
    private lateinit var btnEnviarCodigo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_contrasena)

        edtCorreo = findViewById(R.id.edtCorreo)
        spTipoUsuario = findViewById(R.id.spTipoUsuario)
        btnEnviarCodigo = findViewById(R.id.btnEnviarCodigo)

        val userTypes = arrayOf("Administradores", "Cuidadores")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipoUsuario.adapter = adapter

        btnEnviarCodigo.setOnClickListener {
            val correo = edtCorreo.text.toString().trim()
            val tipoUsuario = spTipoUsuario.selectedItem?.toString()?.trim() ?: ""

            if (correo.isEmpty() || tipoUsuario.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese correo y tipo de usuario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, CambiarContrasena::class.java)
            intent.putExtra("correo", correo)
            intent.putExtra("tipoUsuario", tipoUsuario)
            startActivity(intent)
        }
    }
}