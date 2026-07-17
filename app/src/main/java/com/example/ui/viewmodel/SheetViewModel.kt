package com.example.ui.viewmodel

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.RecentFile
import com.example.data.RecentFileRepository
import com.example.parser.ExcelParser
import com.example.parser.SampleData
import com.example.parser.SheetData
import com.example.parser.WorkbookData
import com.example.utils.Exporter
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val workbook: WorkbookData) : UiState()
    data class Error(val message: String) : UiState()
}

class SheetViewModel(
    private val repository: RecentFileRepository,
    private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("excel_reader_prefs", Context.MODE_PRIVATE)

    // Theme preference: "SYSTEM", "LIGHT", "DARK"
    private val _themePreference = MutableStateFlow(sharedPrefs.getString("theme_pref", "SYSTEM") ?: "SYSTEM")
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    fun setThemePreference(pref: String) {
        _themePreference.value = pref
        sharedPrefs.edit().putString("theme_pref", pref).apply()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Track recently opened files from database
    val recentFiles: StateFlow<List<RecentFile>> = repository.allRecentFiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentWorkbook = MutableStateFlow<WorkbookData?>(null)
    val currentWorkbook: StateFlow<WorkbookData?> = _currentWorkbook.asStateFlow()

    private val _currentSheetIndex = MutableStateFlow(0)
    val currentSheetIndex: StateFlow<Int> = _currentSheetIndex.asStateFlow()

    private val _currentFileName = MutableStateFlow("Spreadsheet")
    val currentFileName: StateFlow<String> = _currentFileName.asStateFlow()

    // Search and filter properties
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sorting properties
    private val _sortColumnIndex = MutableStateFlow(-1)
    val sortColumnIndex: StateFlow<Int> = _sortColumnIndex.asStateFlow()

    private val _isSortAscending = MutableStateFlow(true)
    val isSortAscending: StateFlow<Boolean> = _isSortAscending.asStateFlow()

    // Export UI properties
    private val _exportSuccessPath = MutableStateFlow<String?>(null)
    val exportSuccessPath: StateFlow<String?> = _exportSuccessPath.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSort(colIndex: Int) {
        if (_sortColumnIndex.value == colIndex) {
            _isSortAscending.value = !_isSortAscending.value
        } else {
            _sortColumnIndex.value = colIndex
            _isSortAscending.value = true
        }
    }

    fun clearSort() {
        _sortColumnIndex.value = -1
    }

    fun selectSheet(index: Int) {
        if (_currentWorkbook.value != null && index in 0 until (_currentWorkbook.value?.sheets?.size ?: 0)) {
            _currentSheetIndex.value = index
            clearSort()
            _searchQuery.value = ""
        }
    }

    fun clearExportState() {
        _exportSuccessPath.value = null
    }

    /**
     * Load preset template
     */
    fun loadTemplate(type: Int) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val name = when (type) {
                    1 -> "Template Penjualan"
                    2 -> "Template Nilai Mahasiswa"
                    else -> "Template Keuangan"
                }
                _currentFileName.value = name
                val workbook = SampleData.getWorkbookByTemplate(type)
                _currentWorkbook.value = workbook
                _currentSheetIndex.value = 0
                _searchQuery.value = ""
                clearSort()
                _uiState.value = UiState.Success(workbook)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Gagal memuat template: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Load Spreadsheet from local device URI
     */
    fun loadFromUri(context: Context, uri: Uri) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                
                // Query file metadata (name and size)
                var name = "Spreadsheet.xlsx"
                var sizeBytes = 0L
                val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            name = it.getString(nameIndex)
                        }
                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            sizeBytes = it.getLong(sizeIndex)
                        }
                    }
                }

                _currentFileName.value = name
                val sizeString = formatFileSize(sizeBytes)

                val workbook = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        if (name.lowercase().endsWith(".csv")) {
                            ExcelParser.parseCsv(stream, name)
                        } else {
                            ExcelParser.parseXlsx(stream)
                        }
                    } ?: throw Exception("Stream input tidak dapat dibuka")
                }

                if (workbook.sheets.isEmpty()) {
                    throw Exception("Tidak ada sheet valid ditemukan di dalam file")
                }

                _currentWorkbook.value = workbook
                _currentSheetIndex.value = 0
                _searchQuery.value = ""
                clearSort()

                // Save to Recents Room database
                val recent = RecentFile(
                    name = name,
                    uriString = uri.toString(),
                    size = sizeString
                )
                repository.insert(recent)

                _uiState.value = UiState.Success(workbook)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error("Gagal membaca dokumen: ${e.localizedMessage ?: "File rusak atau tidak didukung"}")
            }
        }
    }

    /**
     * Delete recent file from history
     */
    fun deleteRecent(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    /**
     * Clear all recent files history
     */
    fun clearAllRecent() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    /**
     * Exports the active sheet
     */
    fun exportCurrentSheet(context: Context, formatType: String) {
        val workbook = _currentWorkbook.value ?: return
        val sheetIndex = _currentSheetIndex.value
        if (sheetIndex !in workbook.sheets.indices) return
        
        val sheet = workbook.sheets[sheetIndex]
        _isExporting.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val exportsDir = File(context.getExternalFilesDir(null), "Exports").apply {
                    if (!exists()) mkdirs()
                }
                
                val cleanFileName = _currentFileName.value.substringBeforeLast(".")
                val timestamp = System.currentTimeMillis()
                
                val file = when (formatType) {
                    "PDF" -> File(exportsDir, "${cleanFileName}_${timestamp}.pdf")
                    "PNG" -> File(exportsDir, "${cleanFileName}_${timestamp}.png")
                    else -> File(exportsDir, "${cleanFileName}_${timestamp}.webp")
                }
                
                val filePath = when (formatType) {
                    "PDF" -> Exporter.exportToPdf(sheet, file, _currentFileName.value)
                    "PNG" -> Exporter.exportToImage(sheet, file, _currentFileName.value, Bitmap.CompressFormat.PNG)
                    else -> Exporter.exportToImage(sheet, file, _currentFileName.value, Bitmap.CompressFormat.WEBP)
                }
                
                withContext(Dispatchers.Main) {
                    _exportSuccessPath.value = filePath
                    _isExporting.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isExporting.value = false
                    // Handle failure gracefully or set an error state
                }
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb > 1) {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        } else {
            String.format(Locale.getDefault(), "%.1f KB", kb)
        }
    }

    /**
     * Helper to retrieve rows that match search filter & column sort.
     * Retains row 0 (headers) unchanged at index 0.
     */
    fun getProcessedSheetData(): SheetData? {
        val workbook = _currentWorkbook.value ?: return null
        val sheetIndex = _currentSheetIndex.value
        if (sheetIndex !in workbook.sheets.indices) return null

        val sheet = workbook.sheets[sheetIndex]
        if (sheet.rows.isEmpty()) return sheet

        val headerRow = sheet.rows.first()
        val dataRows = sheet.rows.drop(1)

        // 1. Filtering based on searchQuery
        val query = _searchQuery.value.trim().lowercase()
        val filteredData = if (query.isEmpty()) {
            dataRows
        } else {
            dataRows.filter { row ->
                row.any { cellValue -> cellValue.lowercase().contains(query) }
            }
        }

        // 2. Sorting based on sortColumnIndex and isSortAscending
        val sortColIdx = _sortColumnIndex.value
        val isAsc = _isSortAscending.value
        val processedData = if (sortColIdx != -1 && sortColIdx < headerRow.size) {
            filteredData.sortedWith { rowA, rowB ->
                val valA = if (sortColIdx < rowA.size) rowA[sortColIdx] else ""
                val valB = if (sortColIdx < rowB.size) rowB[sortColIdx] else ""

                // Try sorting numerically if both cells are numbers
                val numA = valA.toDoubleOrNull()
                val numB = valB.toDoubleOrNull()

                if (numA != null && numB != null) {
                    if (isAsc) numA.compareTo(numB) else numB.compareTo(numA)
                } else {
                    if (isAsc) valA.compareTo(valB, ignoreCase = true) else valB.compareTo(valA, ignoreCase = true)
                }
            }
        } else {
            filteredData
        }

        return SheetData(sheet.name, listOf(headerRow) + processedData)
    }
}

class SheetViewModelFactory(
    private val repository: RecentFileRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SheetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SheetViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
