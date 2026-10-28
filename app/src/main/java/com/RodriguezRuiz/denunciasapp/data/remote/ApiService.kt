package com.RodriguezRuiz.denunciasapp.data.remote

import com.RodriguezRuiz.denunciasapp.data.model.Denuncia
import com.RodriguezRuiz.denunciasapp.data.model.DenunciaRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // E1: Registrar denuncia
    @POST("denuncias/")
    suspend fun registrarDenuncia(
        @Body request: DenunciaRequest
    ): Response<Denuncia>

    // E2: Obtener denuncias paginadas
    @GET("denuncias/")
    suspend fun listarDenuncias(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 5
    ): Response<List<Denuncia>>

    // E3: Obtener denuncia por id
    @GET("denuncias/{id}")
    suspend fun obtenerDenunciaPorId(
        @Path("id") id: Int
    ): Response<Denuncia>

    // E4: Denuncias de un ciudadano
    @GET("denuncias/ciudadano/{dni}")
    suspend fun listarPorCiudadano(
        @Path("dni") dni: String
    ): Response<List<Denuncia>>

    // E5: Atender denuncia
    @PUT("denuncias/{id}")
    suspend fun atenderDenuncia(
        @Path("id") id: Int
    ): Response<Denuncia>
}
