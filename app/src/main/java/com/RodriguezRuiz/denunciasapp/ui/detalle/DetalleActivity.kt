package com.RodriguezRuiz.denunciasapp.ui.detalle

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.RodriguezRuiz.denunciasapp.R
import com.RodriguezRuiz.denunciasapp.databinding.ActivityDetalleBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DetalleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "EXTRA_ID"
    }

    private lateinit var binding: ActivityDetalleBinding
    private val viewModel: DetalleViewModel by viewModels()
    private var jobAvisoDemora: Job? = null
    private var idDenuncia: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.titulo_detalle)

        idDenuncia = intent.getIntExtra(EXTRA_ID, -1)
        if (idDenuncia <= 0) {
            Toast.makeText(this, "ID de denuncia no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
        observeViewModel()

        viewModel.cargarDenuncia(idDenuncia)
    }

    private fun setupListeners() {
        binding.btnAtender.setOnClickListener {
            viewModel.atenderDenuncia()
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

    private fun renderUiState(state: DetalleUiState) {
        when (state) {
            is DetalleUiState.Cargando -> {
                binding.layoutCargando.visibility = View.VISIBLE
                binding.layoutContenido.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.tvAvisoDemora.visibility = View.GONE

                jobAvisoDemora?.cancel()
                jobAvisoDemora = lifecycleScope.launch {
                    delay(5000L)
                    binding.tvAvisoDemora.visibility = View.VISIBLE
                }
            }
            is DetalleUiState.Exito -> {
                jobAvisoDemora?.cancel()
                binding.layoutCargando.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutContenido.visibility = View.VISIBLE

                if (state.huboCambios) {
                    setResult(Activity.RESULT_OK)
                }

                val d = state.denuncia
                binding.tvIdDetalle.text = getString(R.string.label_id, d.id)
                binding.tvDniDetalle.text = d.dni
                binding.tvTipoDetalle.text = d.tipo
                binding.tvFechaDetalle.text = d.fecha
                binding.tvDireccionDetalle.text = d.direccion
                binding.tvDescripcionDetalle.text = d.descripcion

                if (d.isPendiente) {
                    binding.tvEstadoDetalle.text = getString(R.string.estado_pendiente)
                    binding.tvEstadoDetalle.setTextColor(
                        ContextCompat.getColor(this, R.color.estado_pendiente)
                    )
                    binding.tvEstadoDetalle.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.estado_pendiente_bg)
                    )
                    binding.btnAtender.visibility = View.VISIBLE
                    binding.btnAtender.isEnabled = !state.atendiendo
                    binding.pbAtendiendo.visibility = if (state.atendiendo) View.VISIBLE else View.GONE
                } else {
                    binding.tvEstadoDetalle.text = getString(R.string.estado_atendida)
                    binding.tvEstadoDetalle.setTextColor(
                        ContextCompat.getColor(this, R.color.estado_atendida)
                    )
                    binding.tvEstadoDetalle.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.estado_atendida_bg)
                    )
                    binding.btnAtender.visibility = View.GONE
                    binding.pbAtendiendo.visibility = View.GONE
                }
            }
            is DetalleUiState.Error -> {
                jobAvisoDemora?.cancel()
                binding.layoutCargando.visibility = View.GONE
                binding.layoutContenido.visibility = View.GONE
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
