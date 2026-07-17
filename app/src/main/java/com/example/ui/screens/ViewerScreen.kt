package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.ui.viewmodel.SheetViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    viewModel: SheetViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fileName by viewModel.currentFileName.collectAsState()
    val workbook by viewModel.currentWorkbook.collectAsState()
    val currentSheetIdx by viewModel.currentSheetIndex.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortColIdx by viewModel.sortColumnIndex.collectAsState()
    val isSortAsc by viewModel.isSortAscending.collectAsState()
    
    val exportPath by viewModel.exportSuccessPath.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    var showExportSheet by remember { mutableStateOf(false) }
    var showCellDetailDialog by remember { mutableStateOf<String?>(null) }

    val processedSheet = viewModel.getProcessedSheetData()
    val horizontalScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Text(
                        text = fileName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    if (sortColIdx != -1) {
                        IconButton(onClick = { viewModel.clearSort() }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Hapus Urutan",
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(
                        onClick = { showExportSheet = true },
                        modifier = Modifier.testTag("export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Ekspor",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Multiple Sheets Selector Tab (if workbook has > 1 sheets)
            workbook?.let { wb ->
                if (wb.sheets.size > 1) {
                    ScrollableTabRow(
                        selectedTabIndex = currentSheetIdx,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        wb.sheets.forEachIndexed { idx, sheet ->
                            Tab(
                                selected = currentSheetIdx == idx,
                                onClick = { viewModel.selectSheet(idx) },
                                text = {
                                    Text(
                                        text = sheet.name,
                                        fontWeight = if (currentSheetIdx == idx) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 2. Search & Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Cari kata kunci dalam baris...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("search_input")
                )
            }

            // 3. Grid Spreadsheet Table View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                processedSheet?.let { sheet ->
                    if (sheet.rows.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada data.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        // Two-way scrollable spreadsheet container
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            // Column counts (excluding No column)
                            val colCount = sheet.rows.first().size
                            
                            LazyColumn(
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                // A. Top Column Headers Index (A, B, C...)
                                item {
                                    Row(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    ) {
                                        // Top-left intersection corner (No.)
                                        Box(
                                            modifier = Modifier
                                                .width(45.dp)
                                                .height(35.dp)
                                                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }
                                        
                                        // Grid column header buttons
                                        for (c in 0 until colCount) {
                                            val letter = colIndexToLetter(c)
                                            val isSortedOnThis = sortColIdx == c
                                            
                                            Row(
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .height(35.dp)
                                                    .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                                    .clickable { viewModel.toggleSort(c) }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = letter,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp,
                                                    color = if (isSortedOnThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                )
                                                if (isSortedOnThis) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = if (isSortAsc) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                        contentDescription = "Urutan",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // B. Spreadsheet Body Rows
                                itemsIndexed(sheet.rows) { rIdx, row ->
                                    val isHeaderRow = rIdx == 0
                                    
                                    Row(
                                        modifier = Modifier
                                            .background(
                                                when {
                                                    isHeaderRow -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                    rIdx % 2 == 1 -> MaterialTheme.colorScheme.surface
                                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                                                }
                                            )
                                    ) {
                                        // Left row numbers index column (1, 2, 3...)
                                        Box(
                                            modifier = Modifier
                                                .width(45.dp)
                                                .height(44.dp)
                                                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isHeaderRow) "H" else rIdx.toString(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isHeaderRow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }

                                        // Values Columns
                                        for (c in 0 until colCount) {
                                            val cellValue = if (c < row.size) row[c] else ""
                                            Box(
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .height(44.dp)
                                                    .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                                    .clickable { 
                                                        if (cellValue.isNotEmpty()) {
                                                            showCellDetailDialog = cellValue
                                                        }
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text(
                                                    text = cellValue,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isHeaderRow) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isHeaderRow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // 4. Modal Bottom Sheet for Export Format Options
    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    text = "Pilih Format Ekspor Halaman",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Pilih format dokumen terbaik untuk menjaga kualitas teks dan struktur tabel spreadsheet saat dibagikan.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Option 1: PDF Vector
                ExportOptionRow(
                    title = "PDF Vector (.pdf)",
                    description = "Format PDF vektor asli. Teks tajam, bisa dizoom tanpa pecah, sangat cocok untuk cetak formulir resmi.",
                    icon = Icons.Default.PictureAsPdf,
                    iconColor = Color(0xFFC62828),
                    onClick = {
                        showExportSheet = false
                        viewModel.exportCurrentSheet(context, "PDF")
                    },
                    modifier = Modifier.testTag("export_pdf_option")
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Option 2: PNG Full Resolution
                ExportOptionRow(
                    title = "PNG Full-Res (.png)",
                    description = "Gambar resolusi tinggi tanpa kompresi kehilangan data. Pilihan terbaik untuk grafik dan tabel visual.",
                    icon = Icons.Default.Image,
                    iconColor = Color(0xFF1565C0),
                    onClick = {
                        showExportSheet = false
                        viewModel.exportCurrentSheet(context, "PNG")
                    },
                    modifier = Modifier.testTag("export_png_option")
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Option 3: WebP Image
                ExportOptionRow(
                    title = "WebP Gambar (.webp)",
                    description = "Format gambar modern dengan kompresi efisien. Ukuran file kecil dengan kualitas visual terjaga.",
                    icon = Icons.Default.Image,
                    iconColor = Color(0xFF2E7D32),
                    onClick = {
                        showExportSheet = false
                        viewModel.exportCurrentSheet(context, "WEBP")
                    },
                    modifier = Modifier.testTag("export_webp_option")
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 5. Fullscreen Loading Screen when Exporting
    AnimatedVisibility(
        visible = isExporting,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sedang Mengekspor...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Merender baris spreadsheet ke format berkualitas tinggi",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // 6. Export Success Dialog
    exportPath?.let { path ->
        Dialog(onDismissRequest = { viewModel.clearExportState() }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("export_success_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Dokumen Berhasil Diekspor!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "File disimpan di folder internal aplikasi:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Display filename in a highlighted box
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = path.substringAfterLast("/"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearExportState() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Tutup", fontSize = 13.sp)
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                shareExportedFile(context, path)
                                viewModel.clearExportState()
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("share_file_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            border = null
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Bagikan", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // 7. Cell Detail Dialog (for full value inspection)
    showCellDetailDialog?.let { value ->
        Dialog(onDismissRequest = { showCellDetailDialog = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Isi Sel Lengkap",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = value,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = { showCellDetailDialog = null },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Tutup", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ExportOptionRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconColor.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
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

private fun shareExportedFile(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val mime = when {
            filePath.endsWith(".pdf") -> "application/pdf"
            filePath.endsWith(".png") -> "image/png"
            else -> "image/webp"
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Bagikan Dokumen Spreadsheet"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
