package com.example.silentsaver

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.silentsaver.databinding.ItemWhitelistContactBinding
import com.example.silentsaver.db.ContactEntity

class WhitelistAdapter(
    private var contacts: MutableList<ContactEntity>,
    private val onDelete: (ContactEntity, Int) -> Unit
) : RecyclerView.Adapter<WhitelistAdapter.Holder>() {

    class Holder(val binding: ItemWhitelistContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemWhitelistContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val contact = contacts[position]
        holder.binding.tvContactName.text = contact.name
        holder.binding.tvPhoneNumber.text = contact.phoneNumber

        holder.binding.btnDelete.setOnClickListener {
            onDelete(contact, position)
        }
    }

    override fun getItemCount() = contacts.size

    fun updateData(newList: List<ContactEntity>) {
        contacts = newList.toMutableList()
        notifyDataSetChanged()
    }
}