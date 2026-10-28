package com.RodriguezRuiz.denunciasapp.ui.busqueda

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        enableEdgeToEdge()
        binding = ActivityBusquedaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorLayout) { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.appBarLayout.setPadding(0, statusBar.top, 0, 0)

            val baseBottomPadding = (24 * resources.displayMetrics.density).toInt()
            binding.rvResultados.setPadding(
                binding.rvResultados.paddingLeft,
                binding.rvResultados.paddingTop,
                binding.rvResultados.paddingRight,
                baseBottomPadding + navBar.bottom
            )
            insets
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
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
        hideKeyboard()
        binding.tilDniBusqueda.error = null
        val dni = binding.etDniBusqueda.text?.toString().orEmpty()
        viewModel.buscar(dni)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocusView = currentFocus ?: binding.root
        imm?.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
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
                binding.progressBarIndeterminate.visibility = View.GONE
                binding.layoutEstadoInicial.visibility = View.VISIBLE
                binding.layoutResultados.visibility = View.GONE
                binding.layoutVacio.visibility = View.GONE
                binding.layoutCargando.visibility = View.GONE
                binding.cardAvisoDemora.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
            }
            is BusquedaUiState.Cargando -> {
                binding.progressBarIndeterminate.visibility = View.VISIBLE
                binding.layoutEstadoInicial.visibility = View.GONE
                binding.layoutResultados.visibility = View.GONE
                binding.layoutVacio.visibility = View.GONE
                binding.layoutCargando.visibility = View.VISIBLE
                binding.cardAvisoDemora.visibility = View.GONE
                binding.layoutError.visibility = View.GONE

                jobAvisoDemora?.cancel()
                jobAvisoDemora = lifecycleScope.launch {
                    delay(5000L)
                    binding.cardAvisoDemora.visibility = View.VISIBLE
                }
            }
            is BusquedaUiState.Exito -> {
                jobAvisoDemora?.cancel()
                binding.progressBarIndeterminate.visibility = View.GONE
                binding.cardAvisoDemora.visibility = View.GONE
                binding.layoutEstadoInicial.visibility = View.GONE
                binding.layoutVacio.visibility = View.GONE
                binding.layoutCargando.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutResultados.visibility = View.VISIBLE

                val dniBuscado = binding.etDniBusqueda.text?.toString().orEmpty()
                binding.tvConteoResultados.text = getString(
                    R.string.resultados_conteo_formato,
                    state.denuncias.size,
                    dniBuscado
                )
                adapter.submitList(state.denuncias)
            }
            is BusquedaUiState.Vacio -> {
                jobAvisoDemora?.cancel()
                binding.progressBarIndeterminate.visibility = View.GONE
                binding.cardAvisoDemora.visibility = View.GONE
                binding.layoutEstadoInicial.visibility = View.GONE
                binding.layoutResultados.visibility = View.GONE
                binding.layoutCargando.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutVacio.visibility = View.VISIBLE
            }
            is BusquedaUiState.ErrorValidacion -> {
                jobAvisoDemora?.cancel()
                binding.progressBarIndeterminate.visibility = View.GONE
                binding.cardAvisoDemora.visibility = View.GONE
                binding.tilDniBusqueda.error = state.mensaje
                binding.etDniBusqueda.requestFocus()
            }
            is BusquedaUiState.ErrorServidor -> {
                jobAvisoDemora?.cancel()
                binding.progressBarIndeterminate.visibility = View.GONE
                binding.cardAvisoDemora.visibility = View.GONE
                binding.layoutEstadoInicial.visibility = View.GONE
                binding.layoutResultados.visibility = View.GONE
                binding.layoutVacio.visibility = View.GONE
                binding.layoutCargando.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMensaje.text = state.mensaje
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jobAvisoDemora?.cancel()
    }
}
