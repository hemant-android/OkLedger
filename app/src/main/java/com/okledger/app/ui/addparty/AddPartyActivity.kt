package com.okledger.app.ui.addparty

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.okledger.app.R
import com.okledger.app.base.BaseActivity
import com.okledger.app.databinding.ActivityAddPartyBinding
import com.okledger.app.ui.viewmodel.AddPartyViewModel
import com.okledger.app.utils.LedgerType
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AddPartyActivity : BaseActivity<ActivityAddPartyBinding>() {

    private val viewModel: AddPartyViewModel by viewModels()
    private var selectedDate: Long = System.currentTimeMillis()
    private var selectedLedgerType: LedgerType = LedgerType.PURCHASES


    override fun getViewBinding() = ActivityAddPartyBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra("ledgerType")?.let {
            selectedLedgerType = LedgerType.valueOf(it)
        }


        binding.toolbar.tvTitle.text = getString(R.string.text_title_add_party)

        binding.toolbar.imgBack.setOnClickListener {
            finish()
        }


        binding.llDatePicker.setOnClickListener {
            showDatePicker()
        }

        binding.btnConfirm.setOnClickListener {
            val name = binding.etPartyName.text.toString().trim()
            val mobile = binding.etPartyMobile.text.toString().trim()
            val address = binding.etPartyAddress.text.toString().trim()
            if (name.isEmpty()) {
                binding.etPartyName.requestFocus()
                Toast.makeText(this, "Enter party name", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.addParty(name, mobile,selectedDate,address,selectedLedgerType)
                Toast.makeText(this, "Party added", Toast.LENGTH_SHORT).show()
                finish()
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
