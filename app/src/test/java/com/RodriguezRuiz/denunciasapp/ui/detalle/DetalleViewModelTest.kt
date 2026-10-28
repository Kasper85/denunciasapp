package com.RodriguezRuiz.denunciasapp.ui.detalle

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
class DetalleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeDenunciaRepository : DenunciaRepository(apiService = nullFakeApiService()) {
        var denunciaResultado: Result<Denuncia> = Result.failure(Exception("Not set"))
        var atenderResultado: Result<Denuncia> = Result.failure(Exception("Not set"))
        var atenderLlamado = false
        var ultimoIdAtendido: Int? = null

        override suspend fun obtenerDenunciaPorId(id: Int): Result<Denuncia> {
            return denunciaResultado
        }

        override suspend fun atenderDenuncia(id: Int): Result<Denuncia> {
            atenderLlamado = true
            ultimoIdAtendido = id
            return atenderResultado
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
    private lateinit var viewModel: DetalleViewModel

    private fun crearDenuncia(id: Int, estado: Int = 1) = Denuncia(
        id = id,
        dni = "87654321",
        fecha = "2026-10-28T09:30:00",
        tipo = "VANDALISMO",
        direccion = "Plaza de Armas",
        descripcion = "Grafiti en monumento",
        estado = estado
    )

    @Before
    fun setUp() {
        fakeRepository = FakeDenunciaRepository()
        viewModel = DetalleViewModel(repository = fakeRepository)
    }

    @Test
    fun cargarDenuncia_exito_muestraDetalle() = runTest {
        val denuncia = crearDenuncia(10, estado = 1)
        fakeRepository.denunciaResultado = Result.success(denuncia)

        viewModel.cargarDenuncia(10)

        val state = viewModel.uiState.value
        assertTrue(state is DetalleUiState.Exito)
        val exito = state as DetalleUiState.Exito
        assertEquals(10, exito.denuncia.id)
        assertEquals(1, exito.denuncia.estado)
        assertTrue(exito.denuncia.isPendiente)
        assertFalse(exito.huboCambios)
    }

    @Test
    fun cargarDenuncia_error404_muestraError() = runTest {
        fakeRepository.denunciaResultado = Result.failure(Exception("No se encontró la denuncia con ID 99"))

        viewModel.cargarDenuncia(99)

        val state = viewModel.uiState.value
        assertTrue(state is DetalleUiState.Error)
        val error = state as DetalleUiState.Error
        assertEquals("No se encontró la denuncia con ID 99", error.mensaje)
    }

    @Test
    fun atenderDenuncia_exito_cambiaEstadoACeroYMarcaHuboCambios() = runTest {
        val denunciaPendiente = crearDenuncia(15, estado = 1)
        val denunciaAtendida = denunciaPendiente.copy(estado = 0)
        fakeRepository.denunciaResultado = Result.success(denunciaPendiente)
        fakeRepository.atenderResultado = Result.success(denunciaAtendida)

        viewModel.cargarDenuncia(15)
        viewModel.atenderDenuncia()

        assertTrue(fakeRepository.atenderLlamado)
        assertEquals(15, fakeRepository.ultimoIdAtendido)

        val state = viewModel.uiState.value
        assertTrue(state is DetalleUiState.Exito)
        val exito = state as DetalleUiState.Exito
        assertEquals(0, exito.denuncia.estado)
        assertTrue(exito.denuncia.isAtendida)
        assertTrue(exito.huboCambios)
    }

    @Test
    fun atenderDenuncia_siYaEstaAtendida_noLlamaRepositorio() = runTest {
        val denunciaYaAtendida = crearDenuncia(20, estado = 0)
        fakeRepository.denunciaResultado = Result.success(denunciaYaAtendida)

        viewModel.cargarDenuncia(20)
        viewModel.atenderDenuncia()

        assertFalse(fakeRepository.atenderLlamado)
    }
}
