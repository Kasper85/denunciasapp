package com.RodriguezRuiz.denunciasapp.ui.lista

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
class ListaViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeDenunciaRepository : DenunciaRepository(apiService = nullFakeApiService()) {
        var respuestas = mutableMapOf<Pair<Int, Int>, Result<List<Denuncia>>>()
        var errorGeneral: Exception? = null

        override suspend fun listarDenuncias(offset: Int, limit: Int): Result<List<Denuncia>> {
            errorGeneral?.let { return Result.failure(it) }
            return respuestas[Pair(offset, limit)] ?: Result.success(emptyList())
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
    private lateinit var viewModel: ListaViewModel

    private fun crearDenuncia(id: Int) = Denuncia(
        id = id,
        dni = "12345678",
        fecha = "2026-10-28T09:00:00",
        tipo = "ROBO",
        direccion = "Calle $id",
        descripcion = "Descripcion $id",
        estado = 1
    )

    @Before
    fun setUp() {
        fakeRepository = FakeDenunciaRepository()
        viewModel = ListaViewModel(repository = fakeRepository)
    }

    @Test
    fun primeraCarga_5Elementos_exitoYHayMasTrue() = runTest {
        val lista5 = (1..5).map { crearDenuncia(it) }
        fakeRepository.respuestas[Pair(0, 5)] = Result.success(lista5)

        viewModel.cargarInicial()

        val state = viewModel.uiState.value
        assertTrue(state is ListaUiState.Exito)
        val exito = state as ListaUiState.Exito
        assertEquals(5, exito.denuncias.size)
        assertTrue(exito.hayMas)
    }

    @Test
    fun cargarSiguientes_acumulaYDetectaFin() = runTest {
        val lista1a5 = (1..5).map { crearDenuncia(it) }
        val lista6a7 = (6..7).map { crearDenuncia(it) }
        fakeRepository.respuestas[Pair(0, 5)] = Result.success(lista1a5)
        fakeRepository.respuestas[Pair(5, 5)] = Result.success(lista6a7)

        viewModel.cargarInicial()
        viewModel.cargarSiguientes()

        val state = viewModel.uiState.value
        assertTrue(state is ListaUiState.Exito)
        val exito = state as ListaUiState.Exito
        assertEquals(7, exito.denuncias.size)
        assertFalse(exito.hayMas) // Llegaron < 5, por lo tanto hayMas = false
    }

    @Test
    fun primeraCarga_menosDe5Elementos_hayMasFalse() = runTest {
        val lista3 = (1..3).map { crearDenuncia(it) }
        fakeRepository.respuestas[Pair(0, 5)] = Result.success(lista3)

        viewModel.cargarInicial()

        val state = viewModel.uiState.value
        assertTrue(state is ListaUiState.Exito)
        val exito = state as ListaUiState.Exito
        assertEquals(3, exito.denuncias.size)
        assertFalse(exito.hayMas)
    }

    @Test
    fun primeraCarga_falla_estadoError() = runTest {
        fakeRepository.errorGeneral = Exception("Fallo de red en el servidor")

        viewModel.cargarInicial()

        val state = viewModel.uiState.value
        assertTrue(state is ListaUiState.Error)
        val error = state as ListaUiState.Error
        assertEquals("Fallo de red en el servidor", error.mensaje)
    }
}
