package com.okledger.app.ui.partytransaction

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.okledger.app.base.BaseActivity
import com.okledger.app.data.model.Transaction
import com.okledger.app.databinding.ActivityPartyTransactionBinding
import com.okledger.app.ui.addtransaction.AddTransactionActivity
import com.okledger.app.ui.editparty.EditPartyActivity
import com.okledger.app.ui.edittransaction.EditTransactionActivity
import com.okledger.app.ui.viewmodel.PartyTransactionViewModel
import com.okledger.app.utils.DateUtils
import com.okledger.app.utils.LedgerType
import com.okledger.app.utils.PdfStatementGenerator
import com.okledger.app.utils.PdfStatementGeneratorOld
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            viewModel.getTransactionsWithBalance(partyId, selectedLedgerType)
                .observe(this) { pair ->

                    if (pair == null) return@observe

                    val (displayList, _) = pair
                    if (displayList.isEmpty()) return@observe

                    PdfStatementGenerator.createStatementPdf(this, partyName,displayList)
                }
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


            binding.toolbar.tvAmount.visibility = if (displayList.isNotEmpty()) View.VISIBLE else View.GONE
            binding.toolbar.imgDownloadStatement.visibility = if (displayList.isNotEmpty()) View.VISIBLE else View.GONE

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

    private fun generateLedgerPdf(partyName: String, txns: List<Transaction>) {
        val pdf = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()

        val pageInfo = PdfDocument.PageInfo.Builder(1200, 1800, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        // Header
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.textSize = 40f

        canvas.drawText(partyName, 600f, 80f, titlePaint)
        titlePaint.textSize = 32f
        canvas.drawText("Account Ledger", 600f, 130f, titlePaint)

        titlePaint.textSize = 28f
        canvas.drawText("Account : $partyName", 600f, 180f, titlePaint)

        // Table Header
        paint.textSize = 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        var y = 260f

        canvas.drawText("Date", 40f, y, paint)
        canvas.drawText("Note", 220f, y, paint)
        canvas.drawText("Received", 600f, y, paint)
        canvas.drawText("Given", 800f, y, paint)
        canvas.drawText("Balance", 1000f, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        y += 40f

        // Running Balance
        var runningBalance = 0.0

        txns.forEach { tx ->
            val dateStr = DateUtils.formatDateOrTime(tx.date)
            val note = tx.note

            val received = if (tx.type.equals("Received", true)) tx.amount else 0.0
            val given = if (tx.type.equals("Given", true)) tx.amount else 0.0

            runningBalance += (received - given)

            canvas.drawText(dateStr, 40f, y, paint)
            canvas.drawText(note, 220f, y, paint)
            canvas.drawText("%.2f".format(received), 600f, y, paint)
            canvas.drawText("%.2f".format(given), 800f, y, paint)
            canvas.drawText("%.2f".format(runningBalance), 1000f, y, paint)

            y += 35f
        }

        // Footer totals
        y += 40f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val totalReceived = txns.filter { it.type == "Received" }.sumOf { it.amount }
        val totalGiven = txns.filter { it.type == "Given" }.sumOf { it.amount }

        canvas.drawText("Total Received: ₹${"%.2f".format(totalReceived)}", 40f, y, paint)
        canvas.drawText("Total Given: ₹${"%.2f".format(totalGiven)}", 500f, y, paint)
        canvas.drawText("Balance: ₹${"%.2f".format(runningBalance)}", 900f, y, paint)

        pdf.finishPage(page)

        // Save Locally
        val filePath = File(getExternalFilesDir(null), "${partyName}_ledger.pdf")
        pdf.writeTo(FileOutputStream(filePath))
        pdf.close()

        Toast.makeText(this, "PDF saved: ${filePath.path}", Toast.LENGTH_LONG).show()
    }

}
