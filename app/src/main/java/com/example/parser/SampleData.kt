package com.example.parser

object SampleData {

    fun getSalesSheet(): SheetData {
        val rows = listOf(
            listOf("No", "Tanggal", "Produk", "Kategori", "Jumlah", "Harga Satuan", "Total Pendapatan"),
            listOf("1", "2026-07-01", "Laptop Pro 15", "Elektronik", "12", "Rp 15.000.000", "Rp 180.000.000"),
            listOf("2", "2026-07-02", "Keyboard Mechanical", "Aksesoris", "45", "Rp 850.000", "Rp 38.250.000"),
            listOf("3", "2026-07-03", "Monitor 4K IPS", "Elektronik", "8", "Rp 4.500.000", "Rp 36.000.000"),
            listOf("4", "2026-07-04", "Mouse Gaming Wireless", "Aksesoris", "30", "Rp 650.000", "Rp 19.500.000"),
            listOf("5", "2026-07-05", "Headset Noise Cancelling", "Audio", "15", "Rp 1.200.000", "Rp 18.000.000"),
            listOf("6", "2026-07-06", "Smartphone OLED 5G", "Elektronik", "25", "Rp 8.000.000", "Rp 200.000.000"),
            listOf("7", "2026-07-07", "External SSD 1TB", "Penyimpanan", "20", "Rp 1.500.000", "Rp 30.000.000"),
            listOf("8", "2026-07-08", "Webcam 1080p Ultra", "Aksesoris", "18", "Rp 950.000", "Rp 17.100.000"),
            listOf("9", "2026-07-09", "Meja Ergonomis Listrik", "Furnitur", "5", "Rp 3.800.000", "Rp 19.000.000"),
            listOf("10", "2026-07-10", "Kursi Ergonomis Mesh", "Furnitur", "10", "Rp 2.500.000", "Rp 25.000.000")
        )
        return SheetData("Data Penjualan", rows)
    }

    fun getStudentGradesSheet(): SheetData {
        val rows = listOf(
            listOf("No", "NIM", "Nama Mahasiswa", "Tugas", "UTS", "UAS", "Nilai Akhir", "Status Lulus"),
            listOf("1", "10122001", "Andi Budiman", "85", "80", "90", "85.5", "LULUS"),
            listOf("2", "10122002", "Citra Lestari", "90", "88", "92", "90.2", "LULUS"),
            listOf("3", "10122003", "Dewi Handayani", "45", "60", "55", "54.0", "TIDAK LULUS"),
            listOf("4", "10122004", "Eko Prasetyo", "78", "75", "80", "77.9", "LULUS"),
            listOf("5", "10122005", "Fajar Nugraha", "88", "82", "85", "84.7", "LULUS"),
            listOf("6", "10122006", "Gita Savitri", "95", "92", "96", "94.6", "LULUS"),
            listOf("7", "10122007", "Hadi Syahputra", "60", "65", "62", "62.1", "LULUS"),
            listOf("8", "10122008", "Indah Permata", "82", "78", "84", "81.6", "LULUS"),
            listOf("9", "10122009", "Joko Susilo", "50", "48", "52", "50.4", "TIDAK LULUS"),
            listOf("10", "10122010", "Kartika Putri", "92", "90", "88", "89.8", "LULUS")
        )
        return SheetData("Nilai Mahasiswa", rows)
    }

    fun getFinanceSheet(): SheetData {
        val rows = listOf(
            listOf("No", "Bulan", "Pemasukan", "Pengeluaran", "Saldo Bulanan", "Keterangan"),
            listOf("1", "Januari", "Rp 45.000.000", "Rp 32.500.000", "Rp 12.500.000", "Rasio Operasional Baik"),
            listOf("2", "Februari", "Rp 48.200.000", "Rp 34.000.000", "Rp 14.200.000", "Rasio Operasional Baik"),
            listOf("3", "Maret", "Rp 52.000.000", "Rp 41.500.000", "Rp 10.500.000", "Pengeluaran Inventaris"),
            listOf("4", "April", "Rp 50.500.000", "Rp 33.000.000", "Rp 17.500.000", "Saldo Meningkat Tinggi"),
            listOf("5", "Mei", "Rp 47.000.000", "Rp 36.800.000", "Rp 10.200.000", "Rasio Operasional Cukup"),
            listOf("6", "Juni", "Rp 55.000.000", "Rp 39.000.000", "Rp 16.000.000", "Puncak Penjualan Semester"),
            listOf("7", "Juli", "Rp 58.500.000", "Rp 42.000.000", "Rp 16.500.000", "Rasio Operasional Baik"),
            listOf("8", "Agustus", "Rp 54.000.000", "Rp 35.500.000", "Rp 18.500.000", "Pemasukan Stabil"),
            listOf("9", "September", "Rp 51.200.000", "Rp 37.000.000", "Rp 14.200.000", "Pembayaran Pajak Tahunan"),
            listOf("10", "Oktober", "Rp 60.500.000", "Rp 44.000.000", "Rp 16.500.000", "Penjualan Kuartal 4 Naik")
        )
        return SheetData("Keuangan Bulanan", rows)
    }

    fun getWorkbookByTemplate(type: Int): WorkbookData {
        val sheet = when (type) {
            1 -> getSalesSheet()
            2 -> getStudentGradesSheet()
            else -> getFinanceSheet()
        }
        return WorkbookData(listOf(sheet))
    }
}
