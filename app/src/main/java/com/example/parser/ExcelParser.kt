package com.example.parser

import java.io.InputStream
import java.lang.StringBuilder
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

data class SheetData(
    val name: String,
    val rows: List<List<String>>
)

data class WorkbookData(
    val sheets: List<SheetData>
)

object ExcelParser {

    /**
     * Parses an .xlsx Excel workbook from an InputStream
     */
    fun parseXlsx(inputStream: InputStream): WorkbookData {
        val sheets = mutableListOf<SheetData>()
        val zipEntries = mutableMapOf<String, ByteArray>()
        
        try {
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        zipEntries[entry.name] = zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return WorkbookData(emptyList())
        }

        // 1. Read shared strings
        val sharedStrings = mutableListOf<String>()
        val sharedStringsBytes = zipEntries["xl/sharedStrings.xml"]
        if (sharedStringsBytes != null) {
            try {
                val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                val doc = builder.parse(sharedStringsBytes.inputStream())
                val siList = doc.getElementsByTagName("si")
                for (i in 0 until siList.length) {
                    val si = siList.item(i) as Element
                    val tList = si.getElementsByTagName("t")
                    val sb = StringBuilder()
                    for (j in 0 until tList.length) {
                        val t = tList.item(j) as Element
                        sb.append(t.textContent ?: "")
                    }
                    sharedStrings.add(sb.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Read sheet names
        val sheetNames = mutableListOf<String>()
        val workbookBytes = zipEntries["xl/workbook.xml"]
        if (workbookBytes != null) {
            try {
                val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                val doc = builder.parse(workbookBytes.inputStream())
                val sheetList = doc.getElementsByTagName("sheet")
                for (i in 0 until sheetList.length) {
                    val sheetEl = sheetList.item(i) as Element
                    val name = sheetEl.getAttribute("name") ?: "Sheet ${i + 1}"
                    sheetNames.add(name)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (sheetNames.isEmpty()) {
            sheetNames.add("Sheet 1")
        }

        // 3. Parse each sheet
        for (index in sheetNames.indices) {
            val sheetName = sheetNames[index]
            // Standard Excel sheet names: xl/worksheets/sheet1.xml, etc.
            val sheetFileName = "xl/worksheets/sheet${index + 1}.xml"
            val sheetBytes = zipEntries[sheetFileName] ?: zipEntries.entries.firstOrNull { 
                it.key.startsWith("xl/worksheets/sheet") 
            }?.value
            
            if (sheetBytes != null) {
                try {
                    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    val doc = builder.parse(sheetBytes.inputStream())
                    
                    val rowsMap = mutableMapOf<Int, MutableMap<Int, String>>()
                    var maxRow = 0
                    var maxCol = 0
                    
                    val rowList = doc.getElementsByTagName("row")
                    for (rIdx in 0 until rowList.length) {
                        val rowEl = rowList.item(rIdx) as Element
                        val cList = rowEl.getElementsByTagName("c")
                        
                        for (cIdx in 0 until cList.length) {
                            val cEl = cList.item(cIdx) as Element
                            val ref = cEl.getAttribute("r") ?: ""
                            if (ref.isEmpty()) continue
                            
                            val (rowNum, colNum) = parseCellReference(ref)
                            maxRow = maxOf(maxRow, rowNum)
                            maxCol = maxOf(maxCol, colNum)
                            
                            val type = cEl.getAttribute("t") ?: ""
                            val vList = cEl.getElementsByTagName("v")
                            var rawVal = ""
                            if (vList.length > 0) {
                                rawVal = vList.item(0).textContent ?: ""
                            }
                            
                            var displayVal = ""
                            when (type) {
                                "s" -> {
                                    val strIdx = rawVal.toIntOrNull()
                                    displayVal = if (strIdx != null && strIdx in sharedStrings.indices) {
                                        sharedStrings[strIdx]
                                    } else {
                                        ""
                                    }
                                }
                                "str" -> {
                                    displayVal = rawVal
                                }
                                "b" -> {
                                    displayVal = if (rawVal == "1") "TRUE" else "FALSE"
                                }
                                "inlineStr" -> {
                                    val tList = cEl.getElementsByTagName("t")
                                    if (tList.length > 0) {
                                        displayVal = tList.item(0).textContent ?: ""
                                    }
                                }
                                else -> {
                                    // Number, format details can be parsed or displayed raw
                                    displayVal = rawVal
                                }
                            }
                            
                            if (!rowsMap.containsKey(rowNum)) {
                                rowsMap[rowNum] = mutableMapOf()
                            }
                            rowsMap[rowNum]!![colNum] = displayVal
                        }
                    }
                    
                    // Reconstruct table grid
                    val sheetGrid = mutableListOf<List<String>>()
                    for (r in 0..maxRow) {
                        val rowList = mutableListOf<String>()
                        val colMap = rowsMap[r]
                        for (c in 0..maxCol) {
                            rowList.add(colMap?.get(c) ?: "")
                        }
                        sheetGrid.add(rowList)
                    }
                    
                    // Skip trailing completely empty rows to keep render performance smooth
                    var lastNonEmptyRow = sheetGrid.size - 1
                    while (lastNonEmptyRow >= 0 && sheetGrid[lastNonEmptyRow].all { it.isEmpty() }) {
                        lastNonEmptyRow--
                    }
                    val trimmedGrid = if (lastNonEmptyRow >= 0) {
                        sheetGrid.subList(0, lastNonEmptyRow + 1)
                    } else {
                        sheetGrid
                    }
                    
                    sheets.add(SheetData(name = sheetName, rows = trimmedGrid))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        return WorkbookData(sheets)
    }

    /**
     * Parses a .csv file from an InputStream
     */
    fun parseCsv(inputStream: InputStream, filename: String = "Spreadsheet"): WorkbookData {
        val rows = mutableListOf<List<String>>()
        var delimiter = ','
        
        try {
            val lines = inputStream.bufferedReader().use { it.readLines() }
            if (lines.isEmpty()) return WorkbookData(emptyList())
            
            // Auto-detect delimiter based on first line
            val firstLine = lines.first()
            val commas = firstLine.count { it == ',' }
            val semicolons = firstLine.count { it == ';' }
            val tabs = firstLine.count { it == '\t' }
            
            if (semicolons > commas && semicolons > tabs) delimiter = ';'
            if (tabs > commas && tabs > semicolons) delimiter = '\t'
            
            for (line in lines) {
                rows.add(parseCsvLine(line, delimiter))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return WorkbookData(emptyList())
        }
        
        // Find max columns to ensure balanced grid rows
        val maxCols = rows.maxOfOrNull { it.size } ?: 0
        val balancedRows = rows.map { row ->
            if (row.size < maxCols) {
                row + List(maxCols - row.size) { "" }
            } else {
                row
            }
        }
        
        val sheetName = filename.substringBeforeLast(".").take(20)
        return WorkbookData(listOf(SheetData(name = sheetName, rows = balancedRows)))
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    curVal.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(curVal.toString().trim())
                curVal = StringBuilder()
            } else {
                curVal.append(c)
            }
            i++
        }
        tokens.add(curVal.toString().trim())
        return tokens
    }

    /**
     * Helper to parse cell references like "AA12" into Pair(row, col) indices
     */
    fun parseCellReference(ref: String): Pair<Int, Int> {
        var colStr = ""
        var rowStr = ""
        for (char in ref) {
            if (char.isLetter()) {
                colStr += char
            } else if (char.isDigit()) {
                rowStr += char
            }
        }
        val colIndex = colLetterToIndex(colStr)
        val rowIndex = (rowStr.toIntOrNull() ?: 1) - 1
        return Pair(rowIndex, colIndex)
    }

    private fun colLetterToIndex(col: String): Int {
        var result = 0
        val upperCol = col.uppercase()
        for (i in 0 until upperCol.length) {
            result = result * 26 + (upperCol[i] - 'A' + 1)
        }
        return result - 1
    }

    fun colIndexToLetter(index: Int): String {
        var temp = index
        var result = ""
        while (temp >= 0) {
            result = ('A' + (temp % 26)).toString() + result
            temp = temp / 26 - 1
        }
        return result
    }
}
