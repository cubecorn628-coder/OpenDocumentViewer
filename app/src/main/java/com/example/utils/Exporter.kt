package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.parser.SheetData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    /**
     * Shared canvas drawing method. Draws a segment of spreadsheet rows onto a Canvas.
     * Returns the vertical pixels consumed.
     */
    fun drawTableToCanvas(
        canvas: Canvas,
        sheet: SheetData,
        width: Float,
        height: Float,
        startRow: Int,
        maxRowsToDraw: Int,
        title: String
    ): Int {
        val margin = 40f
        val topPadding = 100f
        val bottomPadding = 40f
        
        val paintText = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        
        val paintTitle = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(34, 139, 34) // Forest Green Accent
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val paintSub = Paint().apply {
            isAntiAlias = true
            color = Color.DKGRAY
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val paintGrid = Paint().apply {
            color = Color.rgb(200, 200, 200)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val paintHeaderBg = Paint().apply {
            color = Color.rgb(230, 245, 230) // Soft Green
            style = Paint.Style.FILL
        }

        val paintAltRowBg = Paint().apply {
            color = Color.rgb(248, 248, 248) // Soft Off-white
            style = Paint.Style.FILL
        }

        val paintBorder = Paint().apply {
            color = Color.rgb(100, 100, 100)
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }

        // 1. Draw Title & Header Metadata
        canvas.drawText(title, margin, margin + 15f, paintTitle)
        
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Lembar: ${sheet.name}  |  Diekspor pada: $dateStr", margin, margin + 35f, paintSub)
        
        // Draw a green aesthetic top accent line
        val accentPaint = Paint().apply {
            color = Color.rgb(34, 139, 34)
            strokeWidth = 3f
        }
        canvas.drawLine(margin, margin + 45f, width - margin, margin + 45f, accentPaint)

        // 2. Compute Column Widths dynamically
        val colCount = if (sheet.rows.isNotEmpty()) sheet.rows.first().size else 0
        if (colCount == 0) {
            canvas.drawText("Tidak ada data dalam sheet ini.", margin, topPadding + 40f, paintText)
            return (topPadding + 100f).toInt()
        }

        // Add 1 column for row index numbers
        val totalCols = colCount + 1
        val colWidths = FloatArray(totalCols)
        
        val availableWidth = width - (margin * 2)
        
        // Allocate first column for index (e.g. 1, 2, 3...)
        colWidths[0] = 35f
        val remainingWidth = availableWidth - colWidths[0]
        
        // Calculate proportional width based on maximum character length in each column
        val colCharMax = IntArray(colCount) { 4 } // Minimum 4 chars
        val endRowIndex = minOf(startRow + maxRowsToDraw, sheet.rows.size)
        
        for (r in startRow until endRowIndex) {
            val row = sheet.rows[r]
            for (c in row.indices) {
                if (c < colCount) {
                    val len = row[c].length
                    if (len > colCharMax[c]) {
                        colCharMax[c] = len
                    }
                }
            }
        }
        
        // Cap max characters to keep proportional width balanced
        val colWeights = FloatArray(colCount)
        var totalWeight = 0f
        for (c in 0 until colCount) {
            val weight = minOf(colCharMax[c], 15) // cap at 15 chars weight
            colWeights[c] = weight.toFloat()
            totalWeight += colWeights[c]
        }
        
        for (c in 0 until colCount) {
            colWidths[c + 1] = (colWeights[c] / totalWeight) * remainingWidth
        }

        // 3. Draw Header Row
        var currentY = topPadding + 10f
        val rowHeight = 24f
        
        // Header background
        canvas.drawRect(margin, currentY, width - margin, currentY + rowHeight, paintHeaderBg)
        
        // Draw header text (Col indexes A, B, C...)
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var currentX = margin
        
        // Col Index header label (e.g. "#")
        drawCellText(canvas, "No", currentX, currentY, colWidths[0], rowHeight, paintText)
        currentX += colWidths[0]
        
        for (c in 0 until colCount) {
            val label = colIndexToLetter(c)
            drawCellText(canvas, label, currentX, currentY, colWidths[c + 1], rowHeight, paintText)
            currentX += colWidths[c + 1]
        }
        
        // Border of Header
        canvas.drawRect(margin, currentY, width - margin, currentY + rowHeight, paintGrid)
        currentY += rowHeight

        // 4. Draw Rows
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        for (r in startRow until endRowIndex) {
            // Check if we exceed printable height
            if (currentY + rowHeight > height - bottomPadding) {
                break
            }
            
            // Alternating row background
            if (r % 2 == 1) {
                canvas.drawRect(margin, currentY, width - margin, currentY + rowHeight, paintAltRowBg)
            }
            
            currentX = margin
            
            // Draw row index
            val rowIndexStr = (r + 1).toString()
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            drawCellText(canvas, rowIndexStr, currentX, currentY, colWidths[0], rowHeight, paintText)
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            currentX += colWidths[0]
            
            // Draw columns
            val rowData = sheet.rows[r]
            for (c in 0 until colCount) {
                val value = if (c < rowData.size) rowData[c] else ""
                drawCellText(canvas, value, currentX, currentY, colWidths[c + 1], rowHeight, paintText)
                currentX += colWidths[c + 1]
            }
            
            // Draw row border
            canvas.drawRect(margin, currentY, width - margin, currentY + rowHeight, paintGrid)
            currentY += rowHeight
        }
        
        // Draw enclosing thick border
        canvas.drawRect(margin, topPadding + 10f, width - margin, currentY, paintBorder)
        
        // Draw Footer Page / Info
        canvas.drawText("Halaman: ${startRow / maxRowsToDraw + 1}  |  Total Baris: ${sheet.rows.size}", margin, height - 20f, paintSub)
        
        return currentY.toInt()
    }

    private fun drawCellText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        paint: Paint
    ) {
        val textPadding = 4f
        val availableWidth = width - (textPadding * 2)
        
        // Truncate text if it is longer than available width
        var dispText = text
        var textWidth = paint.measureText(dispText)
        if (textWidth > availableWidth) {
            while (dispText.isNotEmpty() && textWidth > availableWidth - 10f) {
                dispText = dispText.dropLast(1)
                textWidth = paint.measureText(dispText + "...")
            }
            dispText += "..."
        }
        
        // Vertically center text
        val bounds = Rect()
        paint.getTextBounds(dispText, 0, dispText.length, bounds)
        val textHeight = bounds.height()
        val textY = y + (height / 2f) + (textHeight / 2f) - 2f
        
        // Align text left with small padding
        val textX = x + textPadding
        canvas.drawText(dispText, textX, textY, paint)
    }

    private fun colIndexToLetter(index: Int): String {
        var temp = index
        var result = ""
        while (temp >= 0) {
            result = ('A' + (temp % 26)).toString() + result
            temp = temp / 26 - 1
        }
        return result
    }

    /**
     * Exports sheet to PDF file. Returns absolute path of file on success.
     */
    fun exportToPdf(sheet: SheetData, outputFile: File, title: String): String {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        
        val rowsPerPage = 28
        var startRow = 0
        var pageIndex = 1
        
        while (startRow < sheet.rows.size || (startRow == 0 && sheet.rows.isEmpty())) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            val page = pdfDocument.startPage(pageInfo)
            
            drawTableToCanvas(
                canvas = page.canvas,
                sheet = sheet,
                width = pageWidth.toFloat(),
                height = pageHeight.toFloat(),
                startRow = startRow,
                maxRowsToDraw = rowsPerPage,
                title = title
            )
            
            pdfDocument.finishPage(page)
            
            startRow += rowsPerPage
            pageIndex++
            
            // Break loop if there are empty rows and we have drawn at least 1 page
            if (sheet.rows.isEmpty()) break
        }
        
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        
        return outputFile.absolutePath
    }

    /**
     * Exports sheet to PNG or WebP file. Returns absolute path on success.
     */
    fun exportToImage(
        sheet: SheetData,
        outputFile: File,
        title: String,
        format: Bitmap.CompressFormat
    ): String {
        // We will create a high-resolution bitmap (e.g. 1200 x 1700, which corresponds to A4 proportion)
        val width = 1200
        // Calculate height dynamically based on rows, but with a standard minimum of 1700 pixels
        val rowsCount = sheet.rows.size
        val rowHeight = 35f
        val paddingHeight = 220f
        val calculatedHeight = (rowsCount * rowHeight + paddingHeight).toInt()
        val height = maxOf(1700, calculatedHeight)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE) // White background
        
        // Scale paints inside drawing context for high resolution
        // The standard draws A4 at 595x842. We scale our Canvas drawing coordinates to 1200x1700.
        val scaleX = width.toFloat() / 595f
        val scaleY = height.toFloat() / 842f
        val minScale = minOf(scaleX, scaleY)
        
        canvas.save()
        canvas.scale(scaleX, scaleX) // Uniform scaling based on width
        
        drawTableToCanvas(
            canvas = canvas,
            sheet = sheet,
            width = 595f,
            height = height / scaleX,
            startRow = 0,
            maxRowsToDraw = sheet.rows.size + 10, // Draw all rows in one long beautiful list
            title = title
        )
        
        canvas.restore()
        
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(format, 95, out)
        }
        
        return outputFile.absolutePath
    }
}
