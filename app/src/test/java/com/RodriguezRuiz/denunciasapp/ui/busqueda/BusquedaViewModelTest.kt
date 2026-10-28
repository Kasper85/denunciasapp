package com.RodriguezRuiz.denunciasapp.ui.busqueda

import com.RodriguezRuiz.denunciasapp.data.model.Denuncia
import com.RodriguezRuiz.denunciasapp.data.repository.DenunciaRepository
import com.RodriguezRuiz.denunciasapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BusquedaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeDenunciaRepository : DenunciaRepository(apiService = nullFakeApiService()) {
        var listarPorCiudadanoLlamado = false
        var ultimoDniConsultado: String? = null
        var resultadoCiudadano: Result<List<Denuncia>> = Result.success(emptyList())

        override suspend fun listarPorCiudadano(dni: String): Result<List<Denuncia>> {
            listarPorCiudadanoLlamado = true
            ultimoDniConsultado = dni
            return resultadoCiudadano
        }
    }

    private class nullFakeApiService : com.RodriguezRuiz.denunciasapp.data.remote.ApiService {
        override suspend fun registrarDenuncia(request: com.RodriguezRuiz.denunciasapp.data.model.DenunciaRequest) = throw NotImplementedError()
        override suspend fun listarDenuncias(offset: Int, limit: Int) = throw NotImplementedError()
        override suspend fun obtenerDenunciaPorId(id: Int) = throw NotImplementedError()
        override suspend fun listarPorCiudadano(dni: String) = throw NotImplementedError()
        override suspend fun atenderDenuncia(id: Int) = throw NotImplementedError()
    }

    private lateinit var fakeRepository: FakeDenunciaRepository
    private lateinit var viewModel: BusquedaViewModel

    private fun crearDenuncia(id: Int, dni: String) = Denuncia(
        id = id,
        dni = dni,
        fecha = "2026-10-28T09:40:00",
        tipo = "ACOSO",
        direccion = "Av. Grau 555",
        descripcion = "Reporte de acoso",
        estado = 1
    )

    @Before
    fun setUp() {
        fakeRepository = FakeDenunciaRepository()
        viewModel = BusquedaViewModel(repository = fakeRepository)
    }

    @Test
    fun buscar_conDniInvalido_noLlamaRepositorioYEmiteErrorValidacion() = runTest {
        viewModel.buscar("123")

        assertFalse(fakeRepository.listarPorCiudadanoLlamado)
        val state = viewModel.uiState.value
        assertTrue(state is BusquedaUiState.ErrorValidacion)
        val error = state as BusquedaUiState.ErrorValidacion
        assertEquals("El DNI debe tener exactamente 8 dígitos", error.mensaje)
    }

    @Test
    fun buscar_conDniValido_listaVacia_emiteVacio() = runTest {
        fakeRepository.resultadoCiudadano = Result.success(emptyList())

        viewModel.buscar("12345678")

        assertTrue(fakeRepository.listarPorCiudadanoLlamado)
        assertEquals("12345678", fakeRepository.ultimoDniConsultado)
        val state = viewModel.uiState.value
        assertTrue(state is BusquedaUiState.Vacio)
    }

    @Test
    fun buscar_conDniValido_conDenuncias_emiteExito() = runTest {
        val lista = listOf(crearDenuncia(1, "87654321"), crearDenuncia(2, "87654321"))
        fakeRepository.resultadoCiudadano = Result.success(lista)

        viewModel.buscar("87654321")

        assertTrue(fakeRepository.listarPorCiudadanoLlamado)
        val state = viewModel.uiState.value
        assertTrue(state is BusquedaUiState.Exito)
        val exito = state as BusquedaUiState.Exito
        assertEquals(2, exito.denuncias.size)
    }

    @Test
    fun buscar_conDniValido_errorServidor_emiteErrorServidor() = runTest {
        fakeRepository.resultadoCiudadano = Result.failure(Exception("Error de conexión con el servidor"))

        viewModel.buscar("87654321")

        assertTrue(fakeRepository.listarPorCiudadanoLlamado)
        val state = viewModel.uiState.value
        assertTrue(state is BusquedaUiState.ErrorServidor)
        val error = state as BusquedaUiState.ErrorServidor
        assertEquals("Error de conexión con el servidor", error.mensaje)
    }
}
