package com.okledger.app.ui.statement

import android.content.ContentValues
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.okledger.app.R
import com.okledger.app.base.BaseActivity
import com.okledger.app.databinding.ActivityStatementBinding
import com.okledger.app.ui.viewmodel.StatementViewModel
import com.okledger.app.utils.DateUtils
import com.okledger.app.utils.PdfStatementGenerator
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class StatementActivity : BaseActivity<ActivityStatementBinding>() {

    private val viewModel: StatementViewModel by viewModels()
    private lateinit var adapter: StatementAdapter

    override fun getViewBinding() = ActivityStatementBinding.inflate(layoutInflater)

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.toolbar.tvTitle.text = getString(R.string.text_title_view_statement)

        setupRecyclerView()

        binding.toolbar.imgBack.setOnClickListener {
            finish()
        }
        binding.toolbar.btnDownloadStatement.setOnClickListener {
            val items = viewModel.statementItems.value ?: emptyList()
            PdfStatementGenerator.createStatementPdf(this, items)
        }

        // Observe transactions
        viewModel.statementItems.observe(this) { list ->
            if (!list.isNullOrEmpty()) {
                binding.rvStatement.visibility = View.VISIBLE
                binding.toolbar.btnDownloadStatement.visibility = View.VISIBLE
                binding.emptyView.root.visibility = View.GONE
                adapter.submitList(list)
            } else {
                binding.rvStatement.visibility = View.GONE
                binding.toolbar.btnDownloadStatement.visibility = View.GONE
                binding.emptyView.root.visibility = View.VISIBLE
            }
        }
    }

    private fun setupRecyclerView() {
        // Initialize adapter first
        adapter = StatementAdapter { transaction ->
        }

        // Set up RecyclerView
        binding.rvStatement.layoutManager = LinearLayoutManager(this)
        binding.rvStatement.adapter = adapter
    }
}