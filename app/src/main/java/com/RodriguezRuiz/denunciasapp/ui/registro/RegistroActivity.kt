package com.RodriguezRuiz.denunciasapp.ui.registro

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.RodriguezRuiz.denunciasapp.R
import com.RodriguezRuiz.denunciasapp.databinding.ActivityRegistroBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val viewModel: RegistroViewModel by viewModels()
    private var jobAvisoDemora: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.titulo_registro)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnRegistrar.setOnClickListener {
            limpiarErrores()
            val dni = binding.etDni.text?.toString().orEmpty()
            val tipo = binding.spinnerTipo.selectedItem?.toString().orEmpty()
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

    private fun limpiarErrores() {
        binding.tilDni.error = null
        binding.tilDireccion.error = null
        binding.tilDescripcion.error = null
        binding.tvError.visibility = View.GONE
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
                binding.btnRegistrar.isEnabled = true
                binding.layoutCargando.visibility = View.GONE
            }
            is RegistroUiState.Cargando -> {
                binding.btnRegistrar.isEnabled = false
                binding.layoutCargando.visibility = View.VISIBLE
                binding.tvAvisoDemora.visibility = View.GONE

                jobAvisoDemora?.cancel()
                jobAvisoDemora = lifecycleScope.launch {
                    delay(5000L)
                    binding.tvAvisoDemora.visibility = View.VISIBLE
                }
            }
            is RegistroUiState.ErrorValidacion -> {
                jobAvisoDemora?.cancel()
                binding.btnRegistrar.isEnabled = true
                binding.layoutCargando.visibility = View.GONE

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
                        binding.tvError.text = mensaje
                        binding.tvError.visibility = View.VISIBLE
                    }
                }
            }
            is RegistroUiState.ErrorServidor -> {
                jobAvisoDemora?.cancel()
                binding.btnRegistrar.isEnabled = true
                binding.layoutCargando.visibility = View.GONE
                binding.tvError.text = state.mensaje
                binding.tvError.visibility = View.VISIBLE
            }
            is RegistroUiState.Exito -> {
                jobAvisoDemora?.cancel()
                binding.layoutCargando.visibility = View.GONE
                binding.btnRegistrar.isEnabled = false

                val mensaje = getString(R.string.registro_exito_formato, state.idDenuncia)
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

                setResult(Activity.RESULT_OK)
                finish()
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
