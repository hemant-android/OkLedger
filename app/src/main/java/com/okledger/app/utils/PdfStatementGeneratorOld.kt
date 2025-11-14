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
import com.okledger.app.data.model.StatementItem

object PdfStatementGeneratorOld {

    @RequiresApi(Build.VERSION_CODES.Q)
    fun createStatementPdf(context: Context, items: List<StatementItem>) {
        if (items.isEmpty()) {
            Toast.makeText(context, "No transactions available!", Toast.LENGTH_SHORT).show()
            return
        }

        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var y = 70f
        val lineHeight = 22f
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas

        val headerPaint = Paint().apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.RED
        }

        val titlePaint = Paint().apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }

        val valuePaint = Paint().apply {
            textSize = 14f
            color = Color.BLACK
        }

        // Fixed column positions
        val colParty = 30f
        val colDate = 120f
        val colType = 200f
        val colAmount = 280f
        val colNote = 360f

        fun drawHeader() {
            canvas.drawText("Statement", pageWidth / 2f - 50, 40f, titlePaint)
            y = 70f
            canvas.drawText("Party", colParty, y, headerPaint)
            canvas.drawText("Date", colDate, y, headerPaint)
            canvas.drawText("Type", colType, y, headerPaint)
            canvas.drawText("Amount", colAmount, y, headerPaint)
            canvas.drawText("Note", colNote, y, headerPaint)
            y += 25f
        }

        drawHeader()

        for (item in items) {

            // Check for page break
            if (y > pageHeight - 60) {
                pdf.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
            }

            canvas.drawText(item.partyName, colParty, y, valuePaint)
            canvas.drawText(DateUtils.formatDateOrTime(item.date), colDate, y, valuePaint)
            canvas.drawText(item.type, colType, y, valuePaint)
            canvas.drawText("₹ ${item.amount}", colAmount, y, valuePaint)
            canvas.drawText(item.note ?: "", colNote, y, valuePaint)

            y += lineHeight
        }

        pdf.finishPage(page)
        saveAndOpenPdf(context, pdf)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveAndOpenPdf(context: Context, pdfDocument: PdfDocument) {
        val fileName = "OkLedgerStatement_${System.currentTimeMillis()}.pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            Toast.makeText(context, "PDF saved in Downloads ✅", Toast.LENGTH_LONG).show()

            openPdf(context, uri)

        } ?: Toast.makeText(context, "Failed to save PDF ❌", Toast.LENGTH_SHORT).show()

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
            Toast.makeText(context, "Install a PDF Viewer App ❌", Toast.LENGTH_SHORT).show()
        }
    }
}
