package com.zulfa.hijrah.ui.ibadah

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zulfa.hijrah.R
import com.zulfa.hijrah.data.IbadahOptions
import com.zulfa.hijrah.data.IbadahRecord
import com.zulfa.hijrah.databinding.ItemIbadahBinding

private object IbadahDiff : DiffUtil.ItemCallback<IbadahRecord>() {
    override fun areItemsTheSame(a: IbadahRecord, b: IbadahRecord) = a.id == b.id
    override fun areContentsTheSame(a: IbadahRecord, b: IbadahRecord) = a == b
}

class IbadahAdapter : ListAdapter<IbadahRecord, IbadahAdapter.ViewHolder>(IbadahDiff) {

    class ViewHolder(val binding: ItemIbadahBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIbadahBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        holder.binding.tvJenis.text = item.jenis
        holder.binding.tvTanggal.text = IbadahOptions.formatTanggal(item.tanggal.toDate().time)
        holder.binding.tvStatus.text = item.status

        val colorRes = when (item.status) {
            "Selesai" -> R.color.green_primary
            "Terlewat" -> R.color.error_red
            else -> R.color.gold
        }
        holder.binding.tvStatus.setTextColor(ContextCompat.getColor(context, colorRes))

        holder.binding.tvCatatan.visibility = if (item.catatan.isBlank()) View.GONE else View.VISIBLE
        holder.binding.tvCatatan.text = item.catatan
    }
}