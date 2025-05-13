package com.example.appv1.responses

data class SolicitudEnvio(
    val correo: String,
    val codigo: String,
    val soloEnviar: Boolean = true
)

