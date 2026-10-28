package com.RodriguezRuiz.denunciasapp.data.repository

import com.RodriguezRuiz.denunciasapp.data.model.Denuncia
import com.RodriguezRuiz.denunciasapp.data.model.DenunciaRequest
import com.RodriguezRuiz.denunciasapp.data.model.ErrorResponse
import com.RodriguezRuiz.denunciasapp.data.remote.ApiService
import com.RodriguezRuiz.denunciasapp.data.remote.RetrofitClient
import com.google.gson.Gson
import retrofit2.Response

class DenunciaRepository(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val gson: Gson = Gson()
) {

    private fun <T> parseError(response: Response<T>): Exception {
        val errorBodyStr = response.errorBody()?.string()
        if (!errorBodyStr.isNullOrBlank()) {
            try {
                val errorResponse = gson.fromJson(errorBodyStr, ErrorResponse::class.java)
                val mensaje = errorResponse?.mensaje
                if (!mensaje.isNullOrBlank()) {
                    return Exception(mensaje)
                }
            } catch (_: Exception) {
                // Si falla el parseo JSON, continuar con fallback
            }
        }
        return Exception("Error en la solicitud: ${response.code()} ${response.message()}".trim())
    }

    suspend fun listarDenuncias(offset: Int = 0, limit: Int = 5): Result<List<Denuncia>> {
        return try {
            val response = apiService.listarDenuncias(offset = offset, limit = limit)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(parseError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registrarDenuncia(request: DenunciaRequest): Result<Denuncia> {
        return try {
            val response = apiService.registrarDenuncia(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(parseError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerDenunciaPorId(id: Int): Result<Denuncia> {
        return try {
            val response = apiService.obtenerDenunciaPorId(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(parseError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listarPorCiudadano(dni: String): Result<List<Denuncia>> {
        return try {
            val response = apiService.listarPorCiudadano(dni)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(parseError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun atenderDenuncia(id: Int): Result<Denuncia> {
        return try {
            val response = apiService.atenderDenuncia(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(parseError(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
