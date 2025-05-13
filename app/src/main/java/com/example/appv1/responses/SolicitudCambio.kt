package com.example.appv1.responses

data class SolicitudCambio(
    val correo: String,
    val codigo: String,
    val nuevaContra: String,
    val soloEnviar: Boolean = false
)
