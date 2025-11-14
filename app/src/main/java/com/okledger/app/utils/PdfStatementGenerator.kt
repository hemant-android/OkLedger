package com.okledger.app.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.okledger.app.data.model.Transaction

object PdfStatementGenerator {

    @RequiresApi(Build.VERSION_CODES.Q)
    fun createStatementPdf(context: Context, partyName: String, txns: List<Transaction>) {
        if (txns.isEmpty()) {
            Toast.makeText(context, "No transactions available!", Toast.LENGTH_SHORT).show()
            return
        }

        val pdf = PdfDocument()
        val pageWidth = 1200
        val pageHeight = 1800

        val lineHeight = 40f
        var y = 260f
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            color = Color.BLACK
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 26f
        }

        // ---------- COLUMN POSITIONS ----------
        val colDate = 40f
        val colNote = 220f
        val colReceived = 600f
        val colGiven = 800f
        val colBalance = 1000f

        // ----------------- HEADER FUNCTION --------------------
        fun drawHeader() {
            canvas.apply {
                // Title
                titlePaint.textSize = 40f
                drawText(partyName, (pageWidth / 2).toFloat(), 80f, titlePaint)

                titlePaint.textSize = 32f
                drawText("Ok Ledger", (pageWidth / 2).toFloat(), 130f, titlePaint)

                titlePaint.textSize = 28f
                drawText("Generated Report", (pageWidth / 2).toFloat(), 180f, titlePaint)

                // Table Header
                var headerY = 260f

                drawText("Date", colDate, headerY, headerPaint)
                drawText("Note", colNote, headerY, headerPaint)
                drawText("Received", colReceived, headerY, headerPaint)
                drawText("Given", colGiven, headerY, headerPaint)
                drawText("Balance", colBalance, headerY, headerPaint)
            }

            y = 300f
        }

        drawHeader()

        // RUNNING BALANCE
        var runningBalance = 0.0

        txns.forEach { tx ->

            // PAGE BREAK
            if (y > pageHeight - 150) {
                pdf.finishPage(page)
                pageNumber++

                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
            }

            val dateStr = DateUtils.formatDateOrTime(tx.date)
            val note = tx.note ?: ""

            val received = if (tx.type.equals("Received", true)) tx.amount else 0.0
            val given = if (tx.type.equals("Given", true)) tx.amount else 0.0

            runningBalance += (received - given)

            canvas.drawText(dateStr, colDate, y, valuePaint)
            canvas.drawText(note, colNote, y, valuePaint)
            canvas.drawText("₹%.2f".format(received), colReceived, y, valuePaint)
            canvas.drawText("₹%.2f".format(given), colGiven, y, valuePaint)
            canvas.drawText("₹%.2f".format(runningBalance), colBalance, y, valuePaint)

            y += lineHeight
        }

        // ------------ FOOTER TOTALS ------------
        y += 30f
        val footerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 28f
            color = Color.BLACK
        }

        val totalReceived = txns.filter { it.type.equals("Received", true) }.sumOf { it.amount }
        val totalGiven = txns.filter { it.type.equals("Given", true) }.sumOf { it.amount }

        canvas.drawText("Total Received: ₹${"%.2f".format(totalReceived)}", 40f, y, footerPaint)
        canvas.drawText("Total Given: ₹${"%.2f".format(totalGiven)}", 500f, y, footerPaint)
        canvas.drawText("Balance: ₹${"%.2f".format(runningBalance)}", 900f, y, footerPaint)

        pdf.finishPage(page)

        saveAndOpenPdf(context, pdf)
    }

    // ---------------- SAVE IN DOWNLOADS ----------------
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveAndOpenPdf(context: Context, pdfDocument: PdfDocument) {
        val fileName = "LedgerStatement_${System.currentTimeMillis()}.pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream -> pdfDocument.writeTo(outputStream) }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            Toast.makeText(context, "PDF saved in Downloads!", Toast.LENGTH_LONG).show()
            openPdf(context, uri)

        } ?: Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()

        pdfDocument.close()
    }

    private fun openPdf(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Install a PDF Viewer App", Toast.LENGTH_SHORT).show()
        }
    }
}
