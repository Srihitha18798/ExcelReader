package com.example.demoappp


import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import org.apache.poi.EncryptedDocumentException
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.poifs.crypt.Decryptor
import org.apache.poi.poifs.crypt.EncryptionInfo
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem
import org.apache.poi.poifs.filesystem.OfficeXmlFileException
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class ExcellReaderViewModel : BaseViewModel() {
    var excelDataListLiveData: MutableLiveData<List<ListItems>> = MutableLiveData()
    var excelExceptionListData: MutableLiveData<String> = MutableLiveData()
    var excellFirstRowData: MutableLiveData<List<String>> = MutableLiveData()
    private val list = ArrayList<ListItems>()
    lateinit var workbook: Workbook
    lateinit var file: File
    lateinit var fileDir: File

    fun readExcelFileFromAssets(filePath: String, password: String = "") {
        list.clear()
        file = File(filePath)
        try {
            if (!file.exists() || file.length() == 0L) {
                excelExceptionListData.postValue("Invalid File")
            }
            if (file.length() > Int.MAX_VALUE) {
                excelExceptionListData.postValue("File too big")
            }
            launch {
                val myInput = FileInputStream(file)
                val firstRow: MutableList<String> = arrayListOf()
                if (password.isNotEmpty()) {
                    workbook = WorkbookFactory.create(file, password)
                    val posFile = POIFSFileSystem(file, true)
                    if (file.name.endsWith("xlsx")) {
                        val info = EncryptionInfo(posFile)
                        val d = Decryptor.getInstance((info))
                        if (!d.verifyPassword(password)) {
                            excelExceptionListData.postValue("Wrong password! ")
                            return@launch
                        }
                        workbook = XSSFWorkbook(d.getDataStream(posFile))
                    } else {
                        org.apache.poi.hssf.record.crypto.Biff8EncryptionKey.setCurrentUserPassword(
                            password
                        )
                        workbook = HSSFWorkbook(posFile.root, true)
                    }
                } else {
                    if (file.name.endsWith("xlsx")) {
                        workbook = XSSFWorkbook(myInput)
                    } else {
                        workbook = HSSFWorkbook(myInput)
                    }
                }
                workbook = addColumnIfNotAdded(workbook)
                val mySheet = workbook.getSheetAt(0)
                val rowIter: Iterator<Row> = mySheet.iterator()
                while (rowIter.hasNext()) {
                    val row: Row = rowIter.next()
                    val cellIter1: Iterator<Cell> = row.cellIterator()
                    if (row.rowNum == 0) {
                        while (cellIter1.hasNext()) {
                            val firstCell: Cell = cellIter1.next()
                            firstRow.add(firstCell.toString())
                        }
                    }
                    val cellIter: Iterator<Cell> = row.cellIterator()
                    val singleRowList: MutableList<SingleRow> = arrayListOf()
                    if (row.rowNum > 0) {
                        while (cellIter.hasNext()) {
                            for (i in firstRow) {
                                if (cellIter.hasNext()) {
                                    val cell: Cell = cellIter.next()
                                    singleRowList.add(SingleRow(i.toString(), cell.toString()))
                                }
                            }
                        }
                        if (singleRowList.isEmpty() == false) {
                            try {
                                if (singleRowList.get(singleRowList.size - 1).value?.isNotEmpty() == true) {
                                    if (singleRowList.get(singleRowList.size - 1).value?.equals(
                                            AppConstant.Completed
                                        ) == false
                                    ) {
                                        singleRowList.get(singleRowList.size - 1).value =
                                            AppConstant.Pending
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        list.add(ListItems(singleRowList))
                    }
                }
                excelDataListLiveData.postValue(list)
                excellFirstRowData.postValue(firstRow)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            excelExceptionListData.postValue(e.message.orEmpty())
        }
    }

    private fun addColumnIfNotAdded(workBook: Workbook): Workbook {
        val sheet = workBook.getSheetAt(0)
        val rowIterator: Iterator<Row> = sheet.iterator()
        while (rowIterator.hasNext()) {
            val row: Row = rowIterator.next()
            Log.e("ApachPOI row count : ", row.count().toString())
            val cellIterator: Iterator<Cell> = row.cellIterator()
            while (cellIterator.hasNext()) {
                val column: Cell = cellIterator.next()
                if (!cellIterator.hasNext()) {
                    if (column.cellType == Cell.CELL_TYPE_STRING) {
                        if (column.stringCellValue.equals(AppConstant.Status) ||
                            column.stringCellValue.equals(AppConstant.Completed)
                        ) {
                            if (column.stringCellValue.equals(AppConstant.Completed)) {
                                val style = workBook.createCellStyle()
                                style.fillBackgroundColor = IndexedColors.YELLOW.index
                                style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
                                row.rowStyle = style
                                Log.e("Already completed", "so setting green")
                            }
                        } else {
                            val cell = row.createCell(row.lastCellNum + 1)
                            cell.setCellValue(AppConstant.Status)
                            Log.e("column.stringCellValue ", "else case")
                        }
                    } else {
                        val cell = row.createCell(row.lastCellNum + 1)
                        cell.setCellValue(AppConstant.Status)
                        Log.e("column.cellType ", "else case")
                    }
                }
            }
        }
        return workBook
    }

    fun sortBy(sortBy: String) {
        launch {
            try {
                val filtered = list
                    .map { excelData ->
                        excelData.apply {
                            this.filterItem = this.singleRowList.find {
                                it.name == sortBy
                            }?.value?.toLowerCase(Locale.ROOT)
                        }
                    }.sortedWith(compareBy { it.filterItem })
                excelDataListLiveData.postValue(filtered)
            } catch (e: Exception) {
                e.printStackTrace()
                excelDataListLiveData.postValue(list)
            }
        }
    }

    fun isEncrypt(filepath: String): Boolean {
        val file = File(filepath)
        val myInput = FileInputStream(file)
        val workbook: Workbook
        try {
            if (file.name.contains("xlsx")) {
                return try {
                    try {
                        POIFSFileSystem(myInput)
                    } catch (ex: IOException) {
                    }
                    true
                } catch (e: OfficeXmlFileException) {
                    false
                }
            } else {
                workbook = HSSFWorkbook(myInput)
            }
        } catch (e: EncryptedDocumentException) {
            return true
        }
        return false
    }

    fun checkPassword(password: String, path: String): Boolean {
        try {
            val file = File(path)
            val wb = WorkbookFactory.create(
                file,
                password
            )
        } catch (e: EncryptedDocumentException) {
            return false
        }
        return true
    }

    fun setPositionCompleted(rowNum: Int) {
        if (::workbook.isInitialized == true) {
            val sheet = workbook.getSheetAt(0)
            val row = sheet.getRow(rowNum + 1)
            Log.e("Last count : ", row.lastCellNum.toString())
            val cell = row.getCell(row.lastCellNum.toInt() - 1)
            if (cell != null) {
                Log.e("File Save Cell : ", cell.stringCellValue.toString())
                val status = cell.stringCellValue.toString()
                if (status.contentEquals(AppConstant.Completed) == true) {
                    cell.setCellValue(AppConstant.Status)
                } else {
                    cell.setCellValue(AppConstant.Completed)
                }
                val newFile = File(fileDir, file?.name.orEmpty())
                if (file?.exists() == true) {
                    file?.delete()
                }
                newFile.createNewFile()
                val fout = FileOutputStream(newFile)
                workbook.write(fout)
                fout.close()

                Log.e("file saving", "File Save Successfully")
            } else {
                Log.e("Cell", "null")
            }
        }
    }
}
