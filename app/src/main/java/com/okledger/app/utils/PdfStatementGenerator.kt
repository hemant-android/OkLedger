package com.okledger.app.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import android.text.StaticLayout
import android.text.Layout
import android.text.TextPaint
import com.okledger.app.data.model.Transaction

object PdfStatementGenerator {

    // ============================================================
    // MULTILINE TEXT HANDLER
    // ============================================================
    @Suppress("DEPRECATION")
    private fun drawMultilineText(
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        canvas: Canvas,
        maxWidth: Int
    ): Float {
        val tp = TextPaint(paint)
        val layout = StaticLayout(text, tp, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false)

        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()

        return layout.height.toFloat()
    }

    // ============================================================
    // SEPARATOR LINE (NORMAL OR DOTTED)
    // ============================================================
    private fun drawSeparatorLine(
        canvas: Canvas,
        y: Float,
        pageWidth: Int,
        dotted: Boolean = false
    ) {
        val paint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 2f
            if (dotted) pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }

        canvas.drawLine(20f, y, pageWidth - 20f, y, paint)
    }

    // ============================================================
    // WATERMARK
    // ============================================================
    private fun drawWatermark(canvas: Canvas, pageWidth: Int, pageHeight: Int) {
        val watermarkPaint = Paint().apply {
            color = Color.parseColor("#20AAAAAA")
            textSize = 120f
            typeface = Typeface.DEFAULT_BOLD
        }

        canvas.save()
        canvas.rotate(-35f, (pageWidth / 2).toFloat(), (pageHeight / 2).toFloat())
        canvas.drawText(
            "OK LEDGER",
            (pageWidth / 2).toFloat(),
            (pageHeight / 2).toFloat(),
            watermarkPaint
        )
        canvas.restore()
    }

    // ============================================================
    // HEADER DRAWER
    // ============================================================
    private fun drawHeader(
        canvas: Canvas,
        partyName: String,
        pageWidth: Int
    ): Float {

        val titlePaint = Paint().apply {
            color = Color.BLACK
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
        }

        // Column titles
        val colDate = 40f
        val colNote = 220f
        val colReceived = 600f
        val colGiven = 800f
        val colBalance = 1000f

        canvas.apply {
            titlePaint.textSize = 40f
            drawText(partyName, (pageWidth / 2).toFloat(), 80f, titlePaint)

            titlePaint.textSize = 32f
            drawText("Ok Ledger", (pageWidth / 2).toFloat(), 130f, titlePaint)

            titlePaint.textSize = 28f
            drawText("Statement Report", (pageWidth / 2).toFloat(), 180f, titlePaint)
        }

        // Underline with spacing below
        drawSeparatorLine(canvas, 210f, pageWidth)
        val nextY = 270f

        canvas.drawText("Date", colDate, nextY, headerPaint)
        canvas.drawText("Note", colNote, nextY, headerPaint)
        canvas.drawText("Received", colReceived, nextY, headerPaint)
        canvas.drawText("Given", colGiven, nextY, headerPaint)
        canvas.drawText("Balance", colBalance, nextY, headerPaint)

        return nextY + 40f // return new Y pointer
    }

    // ============================================================
    // ENTRY ROW DRAWER
    // ============================================================
    private fun drawEntryRow(
        canvas: Canvas,
        tx: Transaction,
        y: Float,
        runningBalance: Double
    ): Float {

        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 26f
        }

        // Column positions
        val colDate = 40f
        val colNote = 220f
        val colReceived = 600f
        val colGiven = 800f
        val colBalance = 1000f

        val dateStr = DateUtils.formatDateOrTime(tx.date)
        val note = tx.note ?: ""

        val noteHeight = drawMultilineText(
            note, colNote, y, valuePaint, canvas, 350
        )

        val rowHeight = maxOf(40f, noteHeight + 10f)

        canvas.drawText(dateStr, colDate, y + 30f, valuePaint)
        // ----- Draw Received only if > 0 -----
        val received = tx.receivedAmount()
        if (received > 0.0) {
            canvas.drawText("₹%.2f".format(received), colReceived, y + 30f, valuePaint)
        }

        // ----- Draw Given only if > 0 -----
        val given = tx.givenAmount()
        if (given > 0.0) {
            canvas.drawText("₹%.2f".format(given), colGiven, y + 30f, valuePaint)
        }

        // ----- Always draw balance -----
        canvas.drawText("₹%.2f".format(runningBalance), colBalance, y + 30f, valuePaint)

        return y + rowHeight + 20f

    }

    // Helpers
    private fun Transaction.receivedAmount() = if (type.equals("Received", true)) amount else 0.0
    private fun Transaction.givenAmount() = if (type.equals("Given", true)) amount else 0.0

    // ============================================================
    // SUMMARY DRAWER
    // ============================================================
    private fun drawSummary(
        canvas: Canvas,
        y: Float,
        pageWidth: Int,
        txns: List<Transaction>
    ) {

        val footerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
        }

        drawSeparatorLine(canvas, y, pageWidth, dotted = true)

        val spaceY = y + 70f

        val totalReceived = txns.sumOf { it.receivedAmount() }
        val totalGiven = txns.sumOf { it.givenAmount() }
        val finalBalance = totalReceived - totalGiven

        canvas.drawText("Total Received: ₹${"%.2f".format(totalReceived)}", 40f, spaceY, footerPaint)
        canvas.drawText("Total Given: ₹${"%.2f".format(totalGiven)}", 450f, spaceY, footerPaint)
        canvas.drawText("Balance: ₹${"%.2f".format(finalBalance)}", 900f, spaceY, footerPaint)
    }

    // ============================================================
    // MAIN PDF CREATOR
    // ============================================================
    @RequiresApi(Build.VERSION_CODES.Q)
    fun createStatementPdf(context: Context, partyName: String, txns: List<Transaction>) {

        if (txns.isEmpty()) {
            Toast.makeText(context, "No transactions available!", Toast.LENGTH_SHORT).show()
            return
        }

        val pdf = PdfDocument()
        val pageWidth = 1200
        val pageHeight = 1800

        var y: Float
        var runningBalance = 0.0
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas

        // Draw watermark on every page
        drawWatermark(canvas, pageWidth, pageHeight)

        // Draw header
        y = drawHeader(canvas, partyName, pageWidth)

        // ---------------- MAIN LOOP ----------------
        txns.forEach { tx ->

            if (y > pageHeight - 200) {
                pdf.finishPage(page)
                pageNumber++

                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas

                drawWatermark(canvas, pageWidth, pageHeight)
                y = drawHeader(canvas, partyName, pageWidth)
            }

            runningBalance += (tx.receivedAmount() - tx.givenAmount())
            y = drawEntryRow(canvas, tx, y, runningBalance)
        }

        // Draw summary
        drawSummary(canvas, y + 40f, pageWidth, txns)

        pdf.finishPage(page)
        saveAndOpenPdf(context, pdf)
    }

    // ============================================================
    // SAVE + OPEN PDF
    // ============================================================
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
            resolver.openOutputStream(it)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(it, contentValues, null, null)

            Toast.makeText(context, "PDF saved!", Toast.LENGTH_LONG).show()
            openPdf(context, it)
        }

        pdfDocument.close()
    }

    private fun openPdf(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Install a PDF viewer", Toast.LENGTH_SHORT).show()
        }
    }
}
