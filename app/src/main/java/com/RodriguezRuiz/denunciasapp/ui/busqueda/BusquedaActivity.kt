package com.RodriguezRuiz.denunciasapp.ui.busqueda

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.RodriguezRuiz.denunciasapp.R
import com.RodriguezRuiz.denunciasapp.databinding.ActivityBusquedaBinding
import com.RodriguezRuiz.denunciasapp.ui.detalle.DetalleActivity
import com.RodriguezRuiz.denunciasapp.ui.lista.DenunciaAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BusquedaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBusquedaBinding
    private val viewModel: BusquedaViewModel by viewModels()
    private lateinit var adapter: DenunciaAdapter
    private var jobAvisoDemora: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBusquedaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.titulo_busqueda)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = DenunciaAdapter { denuncia ->
            val intent = Intent(this, DetalleActivity::class.java).apply {
                putExtra(DetalleActivity.EXTRA_ID, denuncia.id)
            }
            startActivity(intent)
        }
        binding.rvResultados.layoutManager = LinearLayoutManager(this)
        binding.rvResultados.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBuscar.setOnClickListener {
            ejecutarBusqueda()
        }

        binding.etDniBusqueda.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                ejecutarBusqueda()
                true
            } else {
                false
            }
        }

        binding.btnReintentar.setOnClickListener {
            viewModel.reintentar()
        }
    }

    private fun ejecutarBusqueda() {
        binding.tilDniBusqueda.error = null
        val dni = binding.etDniBusqueda.text?.toString().orEmpty()
        viewModel.buscar(dni)
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

    private fun renderUiState(state: BusquedaUiState) {
        when (state) {
            is BusquedaUiState.Idle -> {
                jobAvisoDemora?.cancel()
                binding.tvEstadoInicial.visibility = View.VISIBLE
                binding.rvResultados.visibility = View.GONE
                binding.layoutVacio.visibility = View.GONE
                binding.layoutCargando.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
            }
            is BusquedaUiState.Cargando -> {
                binding.tvEstadoInicial.visibility = View.GONE
                binding.rvResultados.visibility = View.GONE
                binding.layoutVacio.visibility = View.GONE
                binding.layoutCargando.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE
                binding.tvAvisoDemora.visibility = View.GONE

                jobAvisoDemora?.cancel()
                jobAvisoDemora = lifecycleScope.launch {
                    delay(5000L)
                    binding.tvAvisoDemora.visibility = View.VISIBLE
                }
            }
            is BusquedaUiState.Exito -> {
                jobAvisoDemora?.cancel()
                binding.tvEstadoInicial.visibility = View.GONE
                binding.layoutVacio.visibility = View.GONE
                binding.layoutCargando.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.rvResultados.visibility = View.VISIBLE

                adapter.submitList(state.denuncias)
            }
            is BusquedaUiState.Vacio -> {
                jobAvisoDemora?.cancel()
                binding.tvEstadoInicial.visibility = View.GONE
                binding.rvResultados.visibility = View.GONE
                binding.layoutCargando.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutVacio.visibility = View.VISIBLE
            }
            is BusquedaUiState.ErrorValidacion -> {
                jobAvisoDemora?.cancel()
                binding.tilDniBusqueda.error = state.mensaje
                binding.etDniBusqueda.requestFocus()
            }
            is BusquedaUiState.ErrorServidor -> {
                jobAvisoDemora?.cancel()
                binding.tvEstadoInicial.visibility = View.GONE
                binding.rvResultados.visibility = View.GONE
                binding.layoutVacio.visibility = View.GONE
                binding.layoutCargando.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMensaje.text = state.mensaje
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        jobAvisoDemora?.cancel()
    }
}
