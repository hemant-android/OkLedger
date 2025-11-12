package com.okledger.app.ui.dashboard

import android.R.attr.onClick
import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.okledger.app.data.model.Party
import com.okledger.app.data.model.PartyWithTotals
import com.okledger.app.databinding.ItemPartyBinding
import com.okledger.app.databinding.ItemPartyDashboardBinding

class PartyAdapter(private val onClick: (PartyWithTotals) -> Unit) :
    RecyclerView.Adapter<PartyAdapter.ViewHolder>() {

    private val list = mutableListOf<PartyWithTotals>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<PartyWithTotals>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemPartyBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("UseKtx", "SetTextI18n", "DefaultLocale")
        fun bind(item: PartyWithTotals) {
            val p = item.party

            // --- Name ---
            if (p.name.isNotBlank()) {
                binding.tvName.text = p.name
                binding.tvName.visibility = View.VISIBLE
            } else {
                binding.tvName.visibility = View.GONE
            }

            // --- Mobile ---
            if (p.mobile.isNotBlank()) {
                binding.tvMobile.text = p.mobile
                binding.tvMobile.visibility = View.VISIBLE
            } else {
                binding.tvMobile.visibility = View.GONE
            }

            // --- Balance ---

            binding.tvBalance.text = "₹ %.2f".format(kotlin.math.abs(item.netBalance))

            binding.tvBalance.setTextColor(
                if (item.netBalance > 0) Color.RED else Color.parseColor("#4CAF50")
            )

            binding.root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemPartyBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(list[position])

    override fun getItemCount() = list.size
}
