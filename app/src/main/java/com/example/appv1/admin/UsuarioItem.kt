package com.example.appv1.admin


sealed class UsuarioItem {

    data class CuidadorItem(
        val id: String,
        val nombre: String,
        val profilePictureUrl: String // URL de la foto del cuidador
    ) : UsuarioItem()

    data class PacienteItem(
        val idCuidador: String,
        val idPaciente: String,
        val nombre: String,
        val profilePictureUrl: String // URL de la foto del paciente
    ) : UsuarioItem()
}
