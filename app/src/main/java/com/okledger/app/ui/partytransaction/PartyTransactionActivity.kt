package com.okledger.app.ui.partytransaction

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.okledger.app.R
import com.okledger.app.base.BaseActivity
import com.okledger.app.databinding.ActivityPartyTransactionBinding
import com.okledger.app.ui.addtransaction.AddTransactionActivity
import com.okledger.app.ui.editparty.EditPartyActivity
import com.okledger.app.ui.edittransaction.EditTransactionActivity
import com.okledger.app.ui.viewmodel.PartyTransactionViewModel
import com.okledger.app.utils.LedgerType
import com.okledger.app.utils.PdfStatementGenerator
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class PartyTransactionActivity : BaseActivity<ActivityPartyTransactionBinding>() {
    private val viewModel: PartyTransactionViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter
    private var partyId: Int = 0
    private var partyName: String = ""
    private var selectedLedgerType: LedgerType = LedgerType.PURCHASES


    override fun getViewBinding() = ActivityPartyTransactionBinding.inflate(layoutInflater)

    @RequiresApi(Build.VERSION_CODES.Q)
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

        binding.toolbar.imgDownloadStatement.setOnClickListener {

            showDateRangeDialog(it)
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

        viewModel.getTransactionsWithBalance(partyId, selectedLedgerType).observe(this) { pair ->
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
            binding.toolbar.imgDownloadStatement.visibility =
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


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showDateRangeDialog(anchorView: View) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.date_filter_bottom_sheet, null)
        dialog.setContentView(view)

        val customRange = view.findViewById<TextView>(R.id.tvCustomRange)

        customRange.setOnClickListener {
            dialog.dismiss()
            openDateRangePicker()  // your existing function
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun openDateRangePicker() {
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now()) // Disable future dates
            .setStart(MaterialDatePicker.thisMonthInUtcMilliseconds()) // Start of current month
            .setEnd(today) // Up to today
            .build()

        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .setCalendarConstraints(constraints)
            .setTheme(R.style.CustomDatePickerTheme)
            .build()

        picker.show(supportFragmentManager, "DATE_RANGE_PICKER")

        picker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first ?: return@addOnPositiveButtonClickListener
            val endDate = selection.second ?: return@addOnPositiveButtonClickListener

            val endOfDay = Calendar.getInstance().apply {
                timeInMillis = endDate
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            viewModel.getTransactionsWithBalance(partyId, selectedLedgerType, startDate, endOfDay)
                .observe(this) { pair ->
                    val transactions = pair?.first ?: emptyList()
                    if (transactions.isEmpty()) {
//                        Toast.makeText(this, "No transactions", Toast.LENGTH_SHORT).show()
                    } else {
                        val farmName = prefs.getName()?:""
                        val farmAddress = prefs.getAddress()?:""
                        PdfStatementGenerator.createStatementPdf(this, partyName, transactions,startDate,endDate,farmName,farmAddress)
                    }
                }
        }
    }
}