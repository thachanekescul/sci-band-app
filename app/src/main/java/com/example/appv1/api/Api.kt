package com.example.appv1.api

import com.example.appv1.responses.SolicitudCambio
import com.example.appv1.responses.SolicitudEnvio
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface Api {
    @Headers("Content-Type: application/json")
    @POST("enviar.php")
    fun enviarCodigo(@Body solicitud: SolicitudEnvio): Call<Map<String, Any>>

    @Headers("Content-Type: application/json")
    @POST("enviar.php")
    fun cambiarContrasena(@Body solicitud: SolicitudCambio): Call<Map<String, Any>>
}