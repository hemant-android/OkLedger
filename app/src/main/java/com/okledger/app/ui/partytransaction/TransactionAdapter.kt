package com.okledger.app.ui.partytransaction

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.okledger.app.data.model.Transaction
import com.okledger.app.databinding.ItemTransactionBinding
import com.okledger.app.utils.DateUtils

class TransactionAdapter(private val onEditClick: (Transaction) -> Unit
) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private val list = mutableListOf<Transaction>()

    fun submitList(newList: List<Transaction>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(t: Transaction) {
            val isClickable = t.id != -1

            if (t.type.equals("Given", ignoreCase = true)) {
                binding.llGivenSection.visibility = View.VISIBLE
                binding.llReceivedSection.visibility = View.GONE

                binding.tvAmountGiven.text = "₹ ${t.amount}"
                if (t.note.isNotBlank()) {
                    binding.tvNoteGiven.text = t.note
                    binding.tvNoteGiven.visibility = View.VISIBLE
                } else {
                    binding.tvNoteGiven.visibility = View.GONE
                }

                binding.tvDateGiven.text = DateUtils.formatDateOrTime(t.date)

                binding.llGivenSection.setOnClickListener {
                    if (isClickable) onEditClick(t)
                }


            }else if (t.type.equals("Received", ignoreCase = true)) {
                binding.llReceivedSection.visibility = View.VISIBLE
                binding.llGivenSection.visibility = View.GONE

                binding.tvAmountReceived.text = "₹ ${t.amount}"
                if (t.note.isNotBlank()) {
                    binding.tvNoteReceived.text = t.note
                    binding.tvNoteReceived.visibility = View.VISIBLE
                } else {
                    binding.tvNoteReceived.visibility = View.GONE
                }
                binding.tvDateReceived.text = DateUtils.formatDateOrTime(t.date)

                binding.llReceivedSection.setOnClickListener {
                    if (isClickable) onEditClick(t)
                }


            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(list[position])

    override fun getItemCount() = list.size
}
