package com.okledger.app.ui.edittransaction

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.okledger.app.base.BaseActivity
import com.okledger.app.databinding.ActivityAddTransactionBinding
import com.okledger.app.databinding.ActivityEditTransactionBinding
import com.okledger.app.ui.viewmodel.AddTransactionViewModel
import com.okledger.app.ui.viewmodel.EditTransactionViewModel
import com.okledger.app.utils.DateUtils
import com.okledger.app.utils.LedgerType
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EditTransactionActivity : BaseActivity<ActivityEditTransactionBinding>() {

    private val viewModel: EditTransactionViewModel by viewModels()
    private var txnType: String = ""
    private var transactionId: Int = 0
    private var partyId: Int = 0
    private var partyName: String = ""

    private var selectedDate: Long = System.currentTimeMillis()
    private var selectedLedgerType: LedgerType = LedgerType.PURCHASES


    override fun getViewBinding() = ActivityEditTransactionBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transactionId = intent.getIntExtra("transactionId", 0)
        partyId = intent.getIntExtra("partyId", 0)
        partyName = intent.getStringExtra("partyName") ?: ""
        txnType = intent.getStringExtra("txnType") ?: ""
        intent.getStringExtra("ledgerType")?.let {
            selectedLedgerType = LedgerType.valueOf(it)
        }


        binding.toolbar.tvTitle.text = partyName

        viewModel.getTransactionById(transactionId).observe(this) { txn ->
            txn?.let {
                binding.etAmount.setText(it.amount.toString())
                binding.etNote.setText(it.note)
                binding.tvSelectedDate.text = DateUtils.formatDateOrTime(it.date)
                selectedDate = it.date
            }
        }


        binding.toolbar.imgBack.setOnClickListener {
            finish()
        }

        binding.llDatePicker.setOnClickListener {
            showDatePicker()
        }
        binding.btnDelete.setOnClickListener {

            AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.deleteTransaction(transactionId,selectedLedgerType)
                    Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("No", null)
                .show()

        }

        binding.btnSubmit.setOnClickListener {
            val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val note = binding.etNote.text.toString()
            if (amount > 0) {
                viewModel.updateTransaction(transactionId, amount, note,selectedDate,selectedLedgerType)
                Toast.makeText(this, "$txnType transaction updated", Toast.LENGTH_SHORT).show()
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
