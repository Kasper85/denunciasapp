package com.RodriguezRuiz.denunciasapp.ui.detalle

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.RodriguezRuiz.denunciasapp.R
import com.RodriguezRuiz.denunciasapp.databinding.ActivityDetalleBinding
import com.RodriguezRuiz.denunciasapp.util.FormatoFecha
import com.RodriguezRuiz.denunciasapp.util.TipoDenuncia
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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
    private var yaMostroSnackbarExito = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idDenuncia = intent.getIntExtra(EXTRA_ID, -1)
        if (idDenuncia <= 0) {
            Toast.makeText(this, "ID de denuncia no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupInsets()
        setupToolbar()
        setupListeners()
        observeViewModel()

        viewModel.cargarDenuncia(idDenuncia)
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorLayout) { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.appBarLayout.setPadding(0, statusBar.top, 0, 0)

            val baseBottomPadding = (12 * resources.displayMetrics.density).toInt()
            binding.layoutBottomBar.setPadding(
                binding.layoutBottomBar.paddingLeft,
                binding.layoutBottomBar.paddingTop,
                binding.layoutBottomBar.paddingRight,
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

    private fun setupListeners() {
        binding.btnAtender.setOnClickListener {
            mostrarDialogoConfirmacion()
        }

        binding.btnReintentar.setOnClickListener {
            viewModel.reintentar()
        }
    }

    private fun mostrarDialogoConfirmacion() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_atender_titulo)
            .setMessage(getString(R.string.dialog_atender_mensaje, idDenuncia))
            .setNegativeButton(R.string.dialog_cancelar, null)
            .setPositiveButton(R.string.dialog_confirmar) { _, _ ->
                viewModel.atenderDenuncia()
            }
            .show()
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
                binding.cardAvisoDemora.visibility = View.GONE
                binding.layoutContenido.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutBottomBar.visibility = View.GONE
                binding.progressBarIndeterminate.visibility = View.GONE

                jobAvisoDemora?.cancel()
                jobAvisoDemora = lifecycleScope.launch {
                    delay(5000L)
                    binding.cardAvisoDemora.visibility = View.VISIBLE
                }
            }
            is DetalleUiState.Exito -> {
                jobAvisoDemora?.cancel()
                binding.cardAvisoDemora.visibility = View.GONE
                binding.layoutCargando.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutContenido.visibility = View.VISIBLE

                if (state.huboCambios) {
                    setResult(Activity.RESULT_OK)
                    if (!yaMostroSnackbarExito) {
                        yaMostroSnackbarExito = true
                        Snackbar.make(
                            binding.coordinatorLayout,
                            R.string.atendida_exito,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                val d = state.denuncia
                binding.toolbar.title = getString(R.string.label_id, d.id)

                val tipoInfo = TipoDenuncia.from(d.tipo)
                binding.containerIconoTipo.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, tipoInfo.colorBgRes)
                )
                binding.ivTipoIcono.setImageResource(tipoInfo.iconoRes)
                binding.ivTipoIcono.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, tipoInfo.colorRes)
                )
                binding.tvIdDetalle.text = getString(R.string.label_id, d.id)
                binding.tvTipoDetalle.text = tipoInfo.etiqueta

                binding.tvDniDetalle.text = d.dni
                binding.tvFechaAmigable.text = FormatoFecha.formatearAmigable(d.fecha)
                binding.tvFechaDetalle.text = d.fecha
                binding.tvDireccionDetalle.text = d.direccion
                binding.tvDescripcionDetalle.text = d.descripcion

                if (d.isPendiente) {
                    binding.layoutBannerEstado.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.estado_pendiente_bg)
                    )
                    binding.ivBannerEstado.setImageResource(R.drawable.ic_alerta)
                    binding.ivBannerEstado.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.estado_pendiente)
                    )
                    binding.tvBannerEstado.text = getString(R.string.banner_pendiente)
                    binding.tvBannerEstado.setTextColor(
                        ContextCompat.getColor(this, R.color.estado_pendiente)
                    )

                    binding.layoutBottomBar.visibility = View.VISIBLE
                    binding.btnAtender.isEnabled = !state.atendiendo
                    binding.progressBarIndeterminate.visibility = if (state.atendiendo) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                } else {
                    binding.layoutBannerEstado.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.estado_atendida_bg)
                    )
                    binding.ivBannerEstado.setImageResource(R.drawable.ic_check)
                    binding.ivBannerEstado.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.estado_atendida)
                    )
                    binding.tvBannerEstado.text = getString(R.string.banner_atendida)
                    binding.tvBannerEstado.setTextColor(
                        ContextCompat.getColor(this, R.color.estado_atendida)
                    )

                    binding.layoutBottomBar.visibility = View.GONE
                    binding.progressBarIndeterminate.visibility = View.GONE
                }
            }
            is DetalleUiState.Error -> {
                jobAvisoDemora?.cancel()
                binding.cardAvisoDemora.visibility = View.GONE
                binding.layoutCargando.visibility = View.GONE
                binding.layoutContenido.visibility = View.GONE
                binding.layoutBottomBar.visibility = View.GONE
                binding.progressBarIndeterminate.visibility = View.GONE
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
