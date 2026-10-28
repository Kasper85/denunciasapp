package com.RodriguezRuiz.denunciasapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.RodriguezRuiz.denunciasapp.databinding.ActivityMainBinding
import com.RodriguezRuiz.denunciasapp.ui.busqueda.BusquedaActivity
import com.RodriguezRuiz.denunciasapp.ui.detalle.DetalleActivity
import com.RodriguezRuiz.denunciasapp.ui.lista.DenunciaAdapter
import com.RodriguezRuiz.denunciasapp.ui.lista.ListaUiState
import com.RodriguezRuiz.denunciasapp.ui.lista.ListaViewModel
import com.RodriguezRuiz.denunciasapp.ui.registro.RegistroActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ListaViewModel by viewModels()
    private lateinit var adapter: DenunciaAdapter
    private var jobAvisoDemora: Job? = null

    private val registroLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.cargarInicial()
        }
    }

    private val detalleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.cargarInicial()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = DenunciaAdapter { denuncia ->
            val intent = Intent(this, DetalleActivity::class.java).apply {
                putExtra(DetalleActivity.EXTRA_ID, denuncia.id)
            }
            detalleLauncher.launch(intent)
        }
        binding.rvDenuncias.layoutManager = LinearLayoutManager(this)
        binding.rvDenuncias.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnNuevaDenuncia.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            registroLauncher.launch(intent)
        }

        binding.btnBuscarDni.setOnClickListener {
            val intent = Intent(this, BusquedaActivity::class.java)
            startActivity(intent)
        }

        binding.btnCargarMas.setOnClickListener {
            viewModel.cargarSiguientes()
        }

        binding.btnReintentar.setOnClickListener {
            viewModel.reintentar()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: ListaUiState) {
        when (state) {
            is ListaUiState.Cargando -> {
                binding.layoutCargando.visibility = View.VISIBLE
                binding.rvDenuncias.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutPaginacion.visibility = View.GONE
                binding.tvAvisoDemora.visibility = View.GONE

                jobAvisoDemora?.cancel()
                jobAvisoDemora = lifecycleScope.launch {
                    delay(5000L)
                    binding.tvAvisoDemora.visibility = View.VISIBLE
                }
            }
            is ListaUiState.Exito -> {
                jobAvisoDemora?.cancel()
                binding.layoutCargando.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.rvDenuncias.visibility = View.VISIBLE
                binding.layoutPaginacion.visibility = View.VISIBLE

                adapter.submitList(state.denuncias)

                if (state.hayMas) {
                    binding.btnCargarMas.visibility = View.VISIBLE
                    binding.btnCargarMas.isEnabled = !state.cargandoMas
                    binding.tvNoHayMas.visibility = View.GONE
                } else {
                    binding.btnCargarMas.visibility = View.GONE
                    binding.tvNoHayMas.visibility = View.VISIBLE
                }

                binding.progressBarPaginacion.visibility = if (state.cargandoMas) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
            is ListaUiState.Error -> {
                jobAvisoDemora?.cancel()
                binding.layoutCargando.visibility = View.GONE
                if (state.esInicial) {
                    binding.rvDenuncias.visibility = View.GONE
                    binding.layoutPaginacion.visibility = View.GONE
                    binding.layoutError.visibility = View.VISIBLE
                    binding.tvErrorMensaje.text = state.mensaje
                } else {
                    Toast.makeText(this, state.mensaje, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jobAvisoDemora?.cancel()
    }
}
