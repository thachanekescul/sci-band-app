package com.example.appv1.admin

sealed class UsuarioItem {
    data class CuidadorItem(val id: String, val nombre: String) : UsuarioItem()
    data class PacienteItem(val idCuidador: String, val idPaciente: String, val nombre: String) : UsuarioItem()
}