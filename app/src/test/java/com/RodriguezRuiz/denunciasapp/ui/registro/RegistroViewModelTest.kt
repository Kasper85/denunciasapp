package com.RodriguezRuiz.denunciasapp.ui.registro

import com.RodriguezRuiz.denunciasapp.data.model.Denuncia
import com.RodriguezRuiz.denunciasapp.data.model.DenunciaRequest
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
class RegistroViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeDenunciaRepository : DenunciaRepository(apiService = nullFakeApiService()) {
        var registrarLlamado = false
        var ultimoRequest: DenunciaRequest? = null
        var resultadoRegistrar: Result<Denuncia> = Result.success(
            Denuncia(1, "12345678", "2026-10-28T09:00:00", "ROBO", "Dir", "Desc", 1)
        )

        override suspend fun registrarDenuncia(request: DenunciaRequest): Result<Denuncia> {
            registrarLlamado = true
            ultimoRequest = request
            return resultadoRegistrar
        }
    }

    private class nullFakeApiService : com.RodriguezRuiz.denunciasapp.data.remote.ApiService {
        override suspend fun registrarDenuncia(request: DenunciaRequest) = throw NotImplementedError()
        override suspend fun listarDenuncias(offset: Int, limit: Int) = throw NotImplementedError()
        override suspend fun obtenerDenunciaPorId(id: Int) = throw NotImplementedError()
        override suspend fun listarPorCiudadano(dni: String) = throw NotImplementedError()
        override suspend fun atenderDenuncia(id: Int) = throw NotImplementedError()
    }

    private lateinit var fakeRepository: FakeDenunciaRepository
    private lateinit var viewModel: RegistroViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeDenunciaRepository()
        viewModel = RegistroViewModel(repository = fakeRepository)
    }

    @Test
    fun registrar_conDniInvalido_noLlamaRepositorioYEmiteErrorValidacion() = runTest {
        viewModel.registrar(
            dni = "12345",
            tipo = "ROBO",
            direccion = "Calle 1",
            descripcion = "Descripcion valida"
        )

        assertFalse(fakeRepository.registrarLlamado)
        val state = viewModel.uiState.value
        assertTrue(state is RegistroUiState.ErrorValidacion)
        val error = state as RegistroUiState.ErrorValidacion
        assertEquals("El DNI debe tener exactamente 8 dígitos", error.mensaje)
    }

    @Test
    fun registrar_conDireccionVacia_noLlamaRepositorioYEmiteErrorValidacion() = runTest {
        viewModel.registrar(
            dni = "12345678",
            tipo = "ROBO",
            direccion = "   ",
            descripcion = "Descripcion valida"
        )

        assertFalse(fakeRepository.registrarLlamado)
        val state = viewModel.uiState.value
        assertTrue(state is RegistroUiState.ErrorValidacion)
        val error = state as RegistroUiState.ErrorValidacion
        assertEquals("La dirección es obligatoria (máx. 200 caracteres)", error.mensaje)
    }

    @Test
    fun registrar_conDatosValidos_exitoRetornaId() = runTest {
        val denunciaCreada = Denuncia(
            id = 42,
            dni = "87654321",
            fecha = "2026-10-28T09:10:00",
            tipo = "ASALTO",
            direccion = "Av. Bolognesi 100",
            descripcion = "Hecho delictivo",
            estado = 1
        )
        fakeRepository.resultadoRegistrar = Result.success(denunciaCreada)

        viewModel.registrar(
            dni = "87654321",
            tipo = "ASALTO",
            direccion = "Av. Bolognesi 100",
            descripcion = "Hecho delictivo"
        )

        assertTrue(fakeRepository.registrarLlamado)
        val state = viewModel.uiState.value
        assertTrue(state is RegistroUiState.Exito)
        val exito = state as RegistroUiState.Exito
        assertEquals(42, exito.idDenuncia)
    }

    @Test
    fun registrar_conDatosValidos_fallaServidorRetornaErrorServidor() = runTest {
        fakeRepository.resultadoRegistrar = Result.failure(Exception("Error 400: Datos inválidos en servidor"))

        viewModel.registrar(
            dni = "87654321",
            tipo = "ASALTO",
            direccion = "Av. Bolognesi 100",
            descripcion = "Hecho delictivo"
        )

        assertTrue(fakeRepository.registrarLlamado)
        val state = viewModel.uiState.value
        assertTrue(state is RegistroUiState.ErrorServidor)
        val error = state as RegistroUiState.ErrorServidor
        assertEquals("Error 400: Datos inválidos en servidor", error.mensaje)
    }
}
