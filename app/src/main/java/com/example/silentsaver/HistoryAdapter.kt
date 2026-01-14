package com.example.silentsaver

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.silentsaver.databinding.ItemHistoryEventBinding
import com.example.silentsaver.db.HistoryEntity
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(private var events: List<HistoryEntity>) :
    RecyclerView.Adapter<HistoryAdapter.Holder>() {

    class Holder(val binding: ItemHistoryEventBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemHistoryEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val event = events[position]
        holder.binding.tvTitle.text = event.title

        // Format Time
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        holder.binding.tvTimestamp.text = sdf.format(event.timestamp)
    }

    override fun getItemCount() = events.size

    fun updateData(newList: List<HistoryEntity>) {
        events = newList
        notifyDataSetChanged()
    }
}