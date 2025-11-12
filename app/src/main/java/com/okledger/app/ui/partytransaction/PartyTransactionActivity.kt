package com.okledger.app.ui.partytransaction

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.okledger.app.base.BaseActivity
import com.okledger.app.databinding.ActivityPartyTransactionBinding
import com.okledger.app.ui.addtransaction.AddTransactionActivity
import com.okledger.app.ui.editparty.EditPartyActivity
import com.okledger.app.ui.edittransaction.EditTransactionActivity
import com.okledger.app.ui.viewmodel.PartyTransactionViewModel
import com.okledger.app.utils.LedgerType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PartyTransactionActivity : BaseActivity<ActivityPartyTransactionBinding>() {
    private val viewModel: PartyTransactionViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter
    private var partyId: Int = 0
    private var partyName: String = ""
    private var selectedLedgerType: LedgerType = LedgerType.PURCHASES


    override fun getViewBinding() = ActivityPartyTransactionBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        partyId = intent.getIntExtra("partyId", 0)
        partyName = intent.getStringExtra("partyName") ?: ""

        intent.getStringExtra("ledgerType")?.let {
            selectedLedgerType = LedgerType.valueOf(it)
        }

        binding.toolbar.tvTitle.text = partyName

        binding.toolbar.imgEdit.visibility = View.VISIBLE

        binding.toolbar.imgBack.setOnClickListener {
            finish()
        }

        adapter = TransactionAdapter { transaction ->
            val intent = Intent(this, EditTransactionActivity::class.java)
            intent.putExtra("transactionId", transaction.id)
            intent.putExtra("partyId", transaction.partyId)
            intent.putExtra("partyName", partyName)
            intent.putExtra("ledgerType", selectedLedgerType.name)
            startActivity(intent)
        }

        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter

        binding.toolbar.tvTitle.setOnClickListener {
            val intent = Intent(this, EditPartyActivity::class.java)
            intent.putExtra("partyId", partyId)
            intent.putExtra("partyName", partyName)
            intent.putExtra("ledgerType", selectedLedgerType.name)
            startActivity(intent)
        }
        binding.toolbar.imgEdit.setOnClickListener {
            val intent = Intent(this, EditPartyActivity::class.java)
            intent.putExtra("partyId", partyId)
            intent.putExtra("partyName", partyName)
            intent.putExtra("ledgerType", selectedLedgerType.name)
            startActivity(intent)
        }
        binding.btnReceived.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            intent.putExtra("partyId", partyId)
            intent.putExtra("partyName", partyName)
            intent.putExtra("txnType", "Received")
            intent.putExtra("ledgerType", selectedLedgerType.name)
            startActivity(intent)
        }
        binding.btnGiven.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            intent.putExtra("partyId", partyId)
            intent.putExtra("partyName", partyName)
            intent.putExtra("txnType", "Given")
            intent.putExtra("ledgerType", selectedLedgerType.name)
            startActivity(intent)
        }

        viewModel.getTransactionsWithBalance(partyId,selectedLedgerType).observe(this) { pair ->
            if (pair == null) {
                // Party deleted → navigate back to Dashboard
//                Toast.makeText(this, "This party has been deleted", Toast.LENGTH_SHORT).show()
//                finish()
                return@observe
            }

            val (displayList, netBalance) = pair
            adapter.submitList(displayList)

            if (displayList.isEmpty()) {
                binding.rvTransactions.visibility = View.GONE
                binding.textNoTransaction.visibility = View.VISIBLE
            } else {
                binding.rvTransactions.visibility = View.VISIBLE
                binding.textNoTransaction.visibility = View.GONE
            }


            binding.toolbar.tvAmount.visibility =
                if (displayList.isNotEmpty()) View.VISIBLE else View.GONE

            val balanceText = when {
                netBalance > 0 -> "Advance ₹${"%.2f".format(netBalance)}"
                netBalance < 0 -> "Due ₹${"%.2f".format(-netBalance)}"
                else -> "₹ 0.0"
            }

            binding.toolbar.tvAmount.apply {
                text = balanceText
                setTextColor(
                    when {
                        netBalance > 0 -> Color.GREEN
                        netBalance < 0 -> Color.RED
                        else -> Color.GREEN
                    }
                )
            }
        }
    }
}
