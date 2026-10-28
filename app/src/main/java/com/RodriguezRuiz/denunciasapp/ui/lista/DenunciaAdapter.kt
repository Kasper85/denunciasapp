package com.RodriguezRuiz.denunciasapp.ui.lista

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.RodriguezRuiz.denunciasapp.R
import com.RodriguezRuiz.denunciasapp.data.model.Denuncia
import com.RodriguezRuiz.denunciasapp.databinding.ItemDenunciaBinding

class DenunciaAdapter(
    private val onItemClick: (Denuncia) -> Unit
) : ListAdapter<Denuncia, DenunciaAdapter.DenunciaViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DenunciaViewHolder {
        val binding = ItemDenunciaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DenunciaViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: DenunciaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DenunciaViewHolder(
        private val binding: ItemDenunciaBinding,
        private val onItemClick: (Denuncia) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(denuncia: Denuncia) {
            val context = binding.root.context

            binding.tvId.text = "#${denuncia.id}"
            binding.tvTipo.text = denuncia.tipo
            binding.tvDireccion.text = denuncia.direccion
            binding.tvFecha.text = denuncia.fecha

            if (denuncia.isPendiente) {
                binding.tvEstado.text = context.getString(R.string.estado_pendiente)
                binding.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.estado_pendiente))
                binding.tvEstado.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.estado_pendiente_bg)
                )
            } else {
                binding.tvEstado.text = context.getString(R.string.estado_atendida)
                binding.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.estado_atendida))
                binding.tvEstado.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.estado_atendida_bg)
                )
            }

            binding.root.setOnClickListener {
                onItemClick(denuncia)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Denuncia>() {
        override fun areItemsTheSame(oldItem: Denuncia, newItem: Denuncia): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Denuncia, newItem: Denuncia): Boolean {
            return oldItem == newItem
        }
    }
}
