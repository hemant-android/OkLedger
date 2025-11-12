package com.okledger.app.ui.addtransaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.okledger.app.base.BaseActivity
import com.okledger.app.databinding.ActivityAddTransactionBinding
import com.okledger.app.ui.viewmodel.AddTransactionViewModel
import com.okledger.app.utils.LedgerType
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddTransactionActivity : BaseActivity<ActivityAddTransactionBinding>() {

    private val viewModel: AddTransactionViewModel by viewModels()
    private var txnType: String = ""
    private var partyId: Int = 0
    private var partyName: String = ""

    private var selectedDate: Long = System.currentTimeMillis()
    private var selectedLedgerType: LedgerType = LedgerType.PURCHASES

    override fun getViewBinding() = ActivityAddTransactionBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        partyId = intent.getIntExtra("partyId", 0)
        partyName = intent.getStringExtra("partyName") ?: ""
        txnType = intent.getStringExtra("txnType") ?: ""

        intent.getStringExtra("ledgerType")?.let {
            selectedLedgerType = LedgerType.valueOf(it)
        }

        binding.toolbar.tvTitle.text = partyName

        binding.toolbar.imgBack.setOnClickListener {
            finish()
        }

        binding.llDatePicker.setOnClickListener {
            showDatePicker()
        }

        binding.btnSubmit.setOnClickListener {
            val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val note = binding.etNote.text.toString()
            if (amount > 0) {
                viewModel.addTransaction(partyId, amount, note, txnType,selectedDate,selectedLedgerType)
                Toast.makeText(this, "$txnType transaction added", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                binding.etAmount.requestFocus()
                Toast.makeText(this, "Enter valid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedDate
        DatePickerDialog(
            this,
            { _, year, month, day ->
                cal.set(year, month, day)
                selectedDate = cal.timeInMillis
                binding.tvSelectedDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(Date(selectedDate))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
