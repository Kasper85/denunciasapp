package com.RodriguezRuiz.denunciasapp.ui.registro

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.RodriguezRuiz.denunciasapp.R
import com.RodriguezRuiz.denunciasapp.databinding.ActivityRegistroBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val viewModel: RegistroViewModel by viewModels()
    private var jobAvisoDemora: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupToolbar()
        setupListeners()
        setupValidationListeners()
        observeViewModel()
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
        binding.btnRegistrar.setOnClickListener {
            hideKeyboard()
            limpiarErrores()

            val dni = binding.etDni.text?.toString().orEmpty()
            val tipo = obtenerTipoSeleccionado()
            val direccion = binding.etDireccion.text?.toString().orEmpty()
            val descripcion = binding.etDescripcion.text?.toString().orEmpty()

            viewModel.registrar(
                dni = dni,
                tipo = tipo,
                direccion = direccion,
                descripcion = descripcion
            )
        }
    }

    private fun setupValidationListeners() {
        binding.etDni.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val dni = binding.etDni.text?.toString().orEmpty()
                if (dni.isNotEmpty() && dni.length != 8) {
                    binding.tilDni.error = getString(R.string.error_dni_invalido)
                }
            }
        }
        binding.etDni.doAfterTextChanged {
            if (binding.tilDni.error != null && it?.length == 8) {
                binding.tilDni.error = null
            }
        }

        binding.etDireccion.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val dir = binding.etDireccion.text?.toString().orEmpty()
                if (dir.isBlank()) {
                    binding.tilDireccion.error = getString(R.string.error_campo_requerido)
                }
            }
        }
        binding.etDireccion.doAfterTextChanged {
            if (binding.tilDireccion.error != null && !it.isNullOrBlank()) {
                binding.tilDireccion.error = null
            }
        }

        binding.etDescripcion.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val desc = binding.etDescripcion.text?.toString().orEmpty()
                if (desc.isBlank()) {
                    binding.tilDescripcion.error = getString(R.string.error_campo_requerido)
                }
            }
        }
        binding.etDescripcion.doAfterTextChanged {
            if (binding.tilDescripcion.error != null && !it.isNullOrBlank()) {
                binding.tilDescripcion.error = null
            }
        }
    }

    private fun obtenerTipoSeleccionado(): String {
        return when (binding.chipGroupTipo.checkedChipId) {
            R.id.chipTipoAsalto -> "ASALTO"
            R.id.chipTipoVandalismo -> "VANDALISMO"
            R.id.chipTipoAcoso -> "ACOSO"
            R.id.chipTipoOtro -> "OTRO"
            else -> "ROBO"
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.etDni.isEnabled = enabled
        binding.chipGroupTipo.isEnabled = enabled
        binding.chipTipoRobo.isEnabled = enabled
        binding.chipTipoAsalto.isEnabled = enabled
        binding.chipTipoVandalismo.isEnabled = enabled
        binding.chipTipoAcoso.isEnabled = enabled
        binding.chipTipoOtro.isEnabled = enabled
        binding.etDireccion.isEnabled = enabled
        binding.etDescripcion.isEnabled = enabled
        binding.btnRegistrar.isEnabled = enabled
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocusView = currentFocus ?: binding.root
        imm?.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
    }

    private fun limpiarErrores() {
        binding.tilDni.error = null
        binding.tilDireccion.error = null
        binding.tilDescripcion.error = null
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

    private fun renderUiState(state: RegistroUiState) {
        when (state) {
            is RegistroUiState.Idle -> {
                jobAvisoDemora?.cancel()
                setInputsEnabled(true)
                binding.progressBarIndeterminate.visibility = View.GONE
                binding.cardAvisoDemora.visibility = View.GONE
            }
            is RegistroUiState.Cargando -> {
                setInputsEnabled(false)
                binding.progressBarIndeterminate.visibility = View.VISIBLE
                binding.cardAvisoDemora.visibility = View.GONE

                jobAvisoDemora?.cancel()
                jobAvisoDemora = lifecycleScope.launch {
                    delay(5000L)
                    binding.cardAvisoDemora.visibility = View.VISIBLE
                }
            }
            is RegistroUiState.ErrorValidacion -> {
                jobAvisoDemora?.cancel()
                setInputsEnabled(true)
                binding.progressBarIndeterminate.visibility = View.GONE
                binding.cardAvisoDemora.visibility = View.GONE

                val mensaje = state.mensaje
                when {
                    mensaje.contains("DNI", ignoreCase = true) -> {
                        binding.tilDni.error = mensaje
                        binding.etDni.requestFocus()
                    }
                    mensaje.contains("dirección", ignoreCase = true) || mensaje.contains("direccion", ignoreCase = true) -> {
                        binding.tilDireccion.error = mensaje
                        binding.etDireccion.requestFocus()
                    }
                    mensaje.contains("descripción", ignoreCase = true) || mensaje.contains("descripcion", ignoreCase = true) -> {
                        binding.tilDescripcion.error = mensaje
                        binding.etDescripcion.requestFocus()
                    }
                    else -> {
                        Snackbar.make(binding.coordinatorLayout, mensaje, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            is RegistroUiState.ErrorServidor -> {
                jobAvisoDemora?.cancel()
                setInputsEnabled(true)
                binding.progressBarIndeterminate.visibility = View.GONE
                binding.cardAvisoDemora.visibility = View.GONE

                Snackbar.make(binding.coordinatorLayout, state.mensaje, Snackbar.LENGTH_LONG)
                    .setAction(R.string.btn_reintentar) {
                        binding.btnRegistrar.performClick()
                    }
                    .show()
            }
            is RegistroUiState.Exito -> {
                jobAvisoDemora?.cancel()
                binding.progressBarIndeterminate.visibility = View.GONE
                binding.cardAvisoDemora.visibility = View.GONE
                setInputsEnabled(false)

                val mensaje = getString(R.string.registro_exito_formato, state.idDenuncia)
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jobAvisoDemora?.cancel()
    }
}
