package com.RodriguezRuiz.denunciasapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.RodriguezRuiz.denunciasapp.databinding.ActivityMainBinding
import com.RodriguezRuiz.denunciasapp.ui.busqueda.BusquedaActivity
import com.RodriguezRuiz.denunciasapp.ui.detalle.DetalleActivity
import com.RodriguezRuiz.denunciasapp.ui.lista.DenunciaAdapter
import com.RodriguezRuiz.denunciasapp.ui.lista.FiltroEstado
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
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorLayout) { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.appBarLayout.setPadding(0, statusBar.top, 0, 0)

            val baseBottomPadding = (10 * resources.displayMetrics.density).toInt()
            binding.layoutPaginacion.setPadding(
                binding.layoutPaginacion.paddingLeft,
                binding.layoutPaginacion.paddingTop,
                binding.layoutPaginacion.paddingRight,
                baseBottomPadding + navBar.bottom
            )

            val density = resources.displayMetrics.density
            val fabMarginBottom = (72 * density).toInt() + navBar.bottom
            val fabMarginEnd = (16 * density).toInt()
            (binding.fabNuevaDenuncia.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
                bottomMargin = fabMarginBottom
                marginEnd = fabMarginEnd
            }

            insets
        }
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

        binding.rvDenuncias.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && binding.fabNuevaDenuncia.isExtended) {
                    binding.fabNuevaDenuncia.shrink()
                } else if (dy < 0 && !binding.fabNuevaDenuncia.isExtended) {
                    binding.fabNuevaDenuncia.extend()
                }
            }
        })
    }

    private fun setupListeners() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.cargarInicial()
        }

        binding.chipGroupFiltros.setOnCheckedStateChangeListener { _, checkedIds ->
            val filtro = when {
                checkedIds.contains(R.id.chipPendientes) -> FiltroEstado.PENDIENTES
                checkedIds.contains(R.id.chipAtendidas) -> FiltroEstado.ATENDIDAS
                else -> {
                    if (checkedIds.isEmpty()) {
                        binding.chipTodas.isChecked = true
                    }
                    FiltroEstado.TODAS
                }
            }
            viewModel.aplicarFiltro(filtro)
        }

        binding.fabNuevaDenuncia.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            registroLauncher.launch(intent)
        }

        binding.btnRegistrarPrimera.setOnClickListener {
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
                binding.cardAvisoDemora.visibility = View.GONE
                binding.layoutLista.visibility = View.GONE
                binding.layoutVacio.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.progressBarIndeterminate.visibility = View.GONE

                jobAvisoDemora?.cancel()
                jobAvisoDemora = lifecycleScope.launch {
                    delay(5000L)
                    binding.cardAvisoDemora.visibility = View.VISIBLE
                }
            }
            is ListaUiState.Exito -> {
                jobAvisoDemora?.cancel()
                binding.cardAvisoDemora.visibility = View.GONE
                binding.layoutCargando.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                binding.progressBarIndeterminate.visibility = if (state.cargandoMas) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                if (state.totalCargadas == 0) {
                    binding.layoutVacio.visibility = View.VISIBLE
                    binding.layoutLista.visibility = View.GONE
                    binding.tvConteo.text = getString(R.string.resumen_conteo_formato, 0, 0)
                } else {
                    binding.layoutVacio.visibility = View.GONE
                    binding.layoutLista.visibility = View.VISIBLE
                    adapter.submitList(state.denunciasFiltradas)
                    binding.tvConteo.text = getString(
                        R.string.resumen_conteo_formato,
                        state.totalFiltradas,
                        state.totalCargadas
                    )

                    if (state.hayMas) {
                        binding.btnCargarMas.visibility = View.VISIBLE
                        binding.btnCargarMas.isEnabled = !state.cargandoMas
                        binding.tvNoHayMas.visibility = View.GONE
                    } else {
                        binding.btnCargarMas.visibility = View.GONE
                        binding.tvNoHayMas.visibility = View.VISIBLE
                    }
                }
            }
            is ListaUiState.Error -> {
                jobAvisoDemora?.cancel()
                binding.cardAvisoDemora.visibility = View.GONE
                binding.layoutCargando.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                binding.progressBarIndeterminate.visibility = View.GONE

                if (state.esInicial) {
                    binding.layoutLista.visibility = View.GONE
                    binding.layoutVacio.visibility = View.GONE
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
