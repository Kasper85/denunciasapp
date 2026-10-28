package com.RodriguezRuiz.denunciasapp.data.repository

import com.RodriguezRuiz.denunciasapp.data.model.DenunciaRequest
import com.RodriguezRuiz.denunciasapp.data.remote.RetrofitClient
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class DenunciaRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: DenunciaRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val apiService = RetrofitClient.create(
            baseUrl = mockWebServer.url("/").toString(),
            okHttpClient = okHttpClient
        )
        repository = DenunciaRepository(apiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun listarDenuncias_exito_verificaPathYQueries() = runBlocking {
        val json = """
            [
              {
                "id": 1,
                "dni": "12345678",
                "fecha": "2026-10-28T09:00:00",
                "tipo": "ROBO",
                "direccion": "Av. Principal 123",
                "descripcion": "Robo de mochila",
                "estado": 1
              }
            ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.listarDenuncias(offset = 0, limit = 5)

        assertTrue(result.isSuccess)
        val denuncias = result.getOrNull()!!
        assertEquals(1, denuncias.size)
        assertEquals(1, denuncias[0].id)
        assertEquals("ROBO", denuncias[0].tipo)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/denuncias/?offset=0&limit=5", recordedRequest.path)
        assertEquals("GET", recordedRequest.method)
    }

    @Test
    fun registrarDenuncia_exito_retorna201() = runBlocking {
        val responseJson = """
            {
              "id": 10,
              "dni": "87654321",
              "fecha": "2026-10-28T10:00:00",
              "tipo": "ASALTO",
              "direccion": "Calle Los Álamos 456",
              "descripcion": "Asalto a mano armada",
              "estado": 1
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(201).setBody(responseJson))

        val request = DenunciaRequest(
            dni = "87654321",
            tipo = "ASALTO",
            direccion = "Calle Los Álamos 456",
            descripcion = "Asalto a mano armada"
        )
        val result = repository.registrarDenuncia(request)

        assertTrue(result.isSuccess)
        val creada = result.getOrNull()!!
        assertEquals(10, creada.id)
        assertEquals(1, creada.estado)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/denuncias/", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
    }

    @Test
    fun obtenerDenunciaPorId_404_retornaFailureConMensaje() = runBlocking {
        val errorJson = """
            {
              "timestamp": "2026-10-28T10:05:00",
              "status": 404,
              "error": "Not Found",
              "mensaje": "No se encontró la denuncia con ID 999"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody(errorJson))

        val result = repository.obtenerDenunciaPorId(999)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.message?.contains("No se encontró la denuncia con ID 999") == true)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/denuncias/999", recordedRequest.path)
    }

    @Test
    fun listarPorCiudadano_vacio_retornaListaVacia() = runBlocking {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        val result = repository.listarPorCiudadano("11223344")

        assertTrue(result.isSuccess)
        val lista = result.getOrNull()!!
        assertTrue(lista.isEmpty())

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/denuncias/ciudadano/11223344", recordedRequest.path)
        assertEquals("GET", recordedRequest.method)
    }

    @Test
    fun atenderDenuncia_exito_retornaEstadoCero() = runBlocking {
        val responseJson = """
            {
              "id": 5,
              "dni": "55667788",
              "fecha": "2026-10-28T11:00:00",
              "tipo": "VANDALISMO",
              "direccion": "Parque Central",
              "descripcion": "Pintadas en estatua",
              "estado": 0
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody(responseJson))

        val result = repository.atenderDenuncia(5)

        assertTrue(result.isSuccess)
        val atendida = result.getOrNull()!!
        assertEquals(0, atendida.estado)
        assertTrue(atendida.isAtendida)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/denuncias/5", recordedRequest.path)
        assertEquals("PUT", recordedRequest.method)
    }
}
