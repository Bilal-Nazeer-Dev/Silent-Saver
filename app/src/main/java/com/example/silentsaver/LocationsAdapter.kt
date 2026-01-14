package com.example.silentsaver

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.silentsaver.databinding.ItemLocationBinding
import com.example.silentsaver.db.LocationEntity

class LocationAdapter(
    private var list: List<LocationEntity>,
    private val onDeleteClick: (LocationEntity) -> Unit
) : RecyclerView.Adapter<LocationAdapter.Holder>() {

    class Holder(val binding: ItemLocationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemLocationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = list[position]

        holder.binding.tvLocationName.text = item.name
        holder.binding.tvLocationDetails.text = "${item.radius.toInt()}m Radius"

        // Active/Inactive Switch Logic
        holder.binding.switchActive.isChecked = item.isActive

        holder.binding.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = list.size

    // Helper to update data efficiently
    fun updateData(newList: List<LocationEntity>) {
        list = newList
        notifyDataSetChanged()
    }
}