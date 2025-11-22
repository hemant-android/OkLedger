package com.okledger.app.ui.editparty

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.okledger.app.base.BaseActivity
import com.okledger.app.databinding.ActivityEditPartyBinding
import com.okledger.app.ui.dashboard.DashboardActivity
import com.okledger.app.ui.viewmodel.EditPartyViewModel
import com.okledger.app.utils.DateUtils
import com.okledger.app.utils.LedgerType
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class EditPartyActivity : BaseActivity<ActivityEditPartyBinding>() {

    private val viewModel: EditPartyViewModel by viewModels()

    private var partyId: Int = 0
    private var partyName: String = ""
    private var selectedDate: Long = System.currentTimeMillis()
    var ledgerType: LedgerType = LedgerType.PURCHASES


    override fun getViewBinding() = ActivityEditPartyBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        partyId = intent.getIntExtra("partyId", 0)
        partyName = intent.getStringExtra("partyName") ?: ""
        intent.getStringExtra("ledgerType")?.let {
            ledgerType = LedgerType.valueOf(it)
        }

        binding.toolbar.tvTitle.text = partyName

        viewModel.loadParty(partyId,ledgerType)

        setupObservers()


        binding.toolbar.imgBack.setOnClickListener {
            finish()
        }

        binding.llDatePicker.setOnClickListener {
            showDatePicker()
        }

        binding.btnDelete.setOnClickListener {
            // Show confirmation dialog
            AlertDialog.Builder(this)
                .setTitle("Delete Party")
                .setMessage("Are you sure you want to delete this party?")
                .setPositiveButton("Yes") { _, _ ->
                    viewModel.deleteParty(partyId,ledgerType)
                }
                .setNegativeButton("No", null)
                .show()
        }



        binding.btnConfirm.setOnClickListener {
            val name = binding.etPartyName.text.toString().trim()
            val mobile = binding.etPartyMobile.text.toString().trim()
            val address = binding.etPartyAddress.text.toString().trim()
            val openingBalance = binding.etOpeningBalance.text.toString().toDoubleOrNull() ?: 0.0
            val openingType = if (binding.rbGiven.isChecked) "Given" else "Received"

            if (name.isEmpty()) {
                binding.etPartyName.requestFocus()
                Toast.makeText(this, "Enter party name", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.updateParty(
                    partyId,
                    name,
                    mobile,
                    selectedDate,
                    openingBalance,
                    openingType,
                    address,
                    ledgerType

                )

                Toast.makeText(this, "Party added", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupObservers() {
        viewModel.party.observe(this) { party ->
            if (party != null) {
                binding.etPartyName.setText(party.name)
                binding.etPartyMobile.setText(party.mobile)
                if (party.openingBalance >0) {
                    binding.etOpeningBalance.setText(party.openingBalance.toString())
                }
                binding.tvSelectedDate.text = DateUtils.formatDateOrTime(party.createdDate)
                when (party.openingType?.lowercase()) {
                    "received" -> binding.rbReceived.isChecked = true
                    "given" -> binding.rbGiven.isChecked = true
                    else -> binding.rgOpeningType.clearCheck() // if null or unknown
                }

            }
        }

        viewModel.updateStatus.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Party updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        viewModel.partyDeleted.observe(this) { deleted ->
            if (deleted) {
                Toast.makeText(this, "Party deleted", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
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
