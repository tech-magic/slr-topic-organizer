package slr.excel

import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import slr.domain.ResearchPaper
import slr.maps.CountAndResearchPapersInfo_perKeyword
import java.io.File
import java.io.FileInputStream

class ExcelUtils {

    companion object {

        fun printColumnHeaders(excelFile: File, sheetName: String) {
            val fileInputStream = FileInputStream(excelFile)

            val workbook = XSSFWorkbook(fileInputStream)
            val sheet = workbook.getSheet(sheetName)
            val firstRow = sheet.getRow(0)
            val totalNoOfCols: Int = firstRow.lastCellNum - 1

            val df = DataFormatter()

            for (columnIndex in 1..totalNoOfCols) {
                val c = firstRow.getCell(columnIndex)
                val cellData = df.formatCellValue(c)
                println("$columnIndex $cellData")
            }
        }

        fun readColumn(excelFile: File, sheetName: String, columnIndex: Int) {
            val fileInputStream = FileInputStream(excelFile)

            val workbook = XSSFWorkbook(fileInputStream)
            val sheet = workbook.getSheet(sheetName)
            val firstRow = sheet.getRow(0)
            val lastRowIndex = sheet.lastRowNum + 1
            println("Last row index :$lastRowIndex")
            val totalNoOfCols: Int = firstRow.lastCellNum - 1
            println("Total columns :$totalNoOfCols")

            val df = DataFormatter()

            for (j in 0 until lastRowIndex) {
                val currRow = sheet.getRow(j)
                val c = currRow.getCell(columnIndex)
                val cellData: String = df.formatCellValue(c)
                println(cellData)
            }
        }

        fun getKeywordsCountMap(
            excelFile: File,
            sheetName: String,
            keywordsColumnIndex: Int,
            authorsColumnIndex: Int = -1,
            titleColumnIndex: Int = -1,
            abstractColumnIndex: Int = -1,
            synonymMap: Map<String, List<String>> = emptyMap(),
            unwantedKeywords: List<String> = emptyList(),
            userAssignedKeywords: Map<Int, List<String>> = emptyMap(),
        ): Map<String, CountAndResearchPapersInfo_perKeyword> {

            val keywordsCountMap = HashMap<String, CountAndResearchPapersInfo_perKeyword>()

            val fileInputStream = FileInputStream(excelFile)

            val workbook = XSSFWorkbook(fileInputStream)
            val sheet = workbook.getSheet(sheetName)

            val firstRow = sheet.getRow(0)
            val totalNoOfCols: Int = firstRow.lastCellNum - 1

            val lastRowIndex = sheet.lastRowNum + 1

            val df = DataFormatter()

            val missedResearchPapers = ArrayList<Int>()
            for (j in 1 until lastRowIndex) {
                val currRow = sheet.getRow(j)

                val currKeywordsCellData: String = df.formatCellValue(currRow.getCell(keywordsColumnIndex))

                val currPaperIndex: Int = j
                val currAuthorsCellData: String? =
                    if (authorsColumnIndex in 1..totalNoOfCols) df.formatCellValue(currRow.getCell(authorsColumnIndex)) else null
                val currTitleCellData: String? =
                    if (titleColumnIndex in 1..totalNoOfCols) df.formatCellValue(currRow.getCell(titleColumnIndex)) else null
                val currAbstractCellData: String? =
                    if (abstractColumnIndex in 1..totalNoOfCols) df.formatCellValue(currRow.getCell(abstractColumnIndex)) else null

                val currPaperRecord = ResearchPaper(
                    referenceIndex = currPaperIndex,
                    paperTitle = currTitleCellData,
                    authors = currAuthorsCellData,
                    paperAbstract = currAbstractCellData
                )

                if (currKeywordsCellData.trim().isNotEmpty()) {
                    val phrasesFromExcel =  currKeywordsCellData.split(",")
                    val currPhrasesSubList = userAssignedKeywords[currPaperIndex] ?.let { customKeywords ->
                        listOf(*phrasesFromExcel.toTypedArray(), *customKeywords.toTypedArray()).toSet().toList()
                    } ?: phrasesFromExcel.toSet().toList()
                    for (currPhrase in currPhrasesSubList) {
                        val formattedText = currPhrase.trim().lowercase()
                        val currKeyword =
                            isASynonymForAMainKeyword(formattedText, synonymMap)?.let { mainKeyword -> mainKeyword }
                                ?: formattedText
                        if (currKeyword.trim().isNotEmpty() && !unwantedKeywords.contains(currKeyword)) {
                            keywordsCountMap[currKeyword] =
                                keywordsCountMap[currKeyword]?.let { associatedKeywordsInfo ->
                                    val paperInformation =
                                        if (associatedKeywordsInfo.researchPapers.any { paperRecord -> paperRecord.referenceIndex == currPaperRecord.referenceIndex }) {
                                            associatedKeywordsInfo.researchPapers
                                        } else {
                                            listOf(
                                                *associatedKeywordsInfo.researchPapers.toTypedArray(),
                                                currPaperRecord
                                            )
                                        }

                                    CountAndResearchPapersInfo_perKeyword(
                                        totalCount = associatedKeywordsInfo.totalCount + 1,
                                        researchPapers = paperInformation
                                    )
                                } ?: CountAndResearchPapersInfo_perKeyword(1, listOf(currPaperRecord))
                        }
                    }
                } else {
                    missedResearchPapers.add(currPaperIndex)
                }
            }

            val missedPapersAfterSecondPass = ArrayList<ResearchPaper>()

            // if (missedResearchPapers.isEmpty()) {
            if (missedResearchPapers.isNotEmpty()) {
                for (currPaperIndex in missedResearchPapers) {
                    val currRow = sheet.getRow(currPaperIndex)

                    val currAuthorsCellData: String? =
                        if (authorsColumnIndex in 1..totalNoOfCols) df.formatCellValue(currRow.getCell(authorsColumnIndex)) else null
                    val currTitleCellData: String? =
                        if (titleColumnIndex in 1..totalNoOfCols) df.formatCellValue(currRow.getCell(titleColumnIndex)) else null
                    val currAbstractCellData: String? =
                        if (abstractColumnIndex in 1..totalNoOfCols) df.formatCellValue(currRow.getCell(abstractColumnIndex)) else null

                    val currPaperRecord = ResearchPaper(
                        referenceIndex = currPaperIndex,
                        paperTitle = currTitleCellData,
                        authors = currAuthorsCellData,
                        paperAbstract = currAbstractCellData
                    )

                    var containsKeyword = false
                    for(currKeyword in keywordsCountMap.keys) {

                        val synonyms = synonymMap[currKeyword]
                        val phrasesToCheck =
                            if (synonyms != null) listOf(*synonyms.toTypedArray(), currKeyword) else listOf(currKeyword)

                        var containsExistingPhrase = false
                        for (currPhrase in phrasesToCheck) {
                            if (currTitleCellData != null && currTitleCellData.lowercase().indexOf(currPhrase.lowercase()) != -1) {
                                containsExistingPhrase = true
                            } else if (currAbstractCellData != null && currAbstractCellData.lowercase().indexOf(currPhrase.lowercase()) != -1) {
                                containsExistingPhrase = true
                            }
                        }

                        if (containsExistingPhrase) {
                            val existingKeywordRecord = keywordsCountMap[currKeyword]
                            if (existingKeywordRecord != null && !existingKeywordRecord.researchPapers.contains(currPaperRecord)) {
                                keywordsCountMap[currKeyword] =
                                    CountAndResearchPapersInfo_perKeyword(
                                        totalCount = existingKeywordRecord.totalCount + 1,
                                        researchPapers = listOf(
                                            *existingKeywordRecord.researchPapers.toTypedArray(),
                                            currPaperRecord
                                        ).toSet().toList()
                                    )
                                containsKeyword = true
                            }
                        }
                    }

                    if (!containsKeyword) {
                        missedPapersAfterSecondPass.add(currPaperRecord)
                    }
                }
            }

            if (missedPapersAfterSecondPass.isNotEmpty()) {
                println("###### Missed ${missedPapersAfterSecondPass.size} during parsing ...")
                for (currPaper in missedPapersAfterSecondPass) {
                    println("${currPaper.referenceIndex} ${currPaper.paperTitle}")
                }
            }

            return keywordsCountMap.filter { entry -> !unwantedKeywords.contains(entry.key) }
                .toList()
                .sortedByDescending { (_, value) -> value.totalCount }
                .toMap()
        }

        private fun isASynonymForAMainKeyword(inspectingWord: String, synonymMap: Map<String, List<String>>): String? {
            for (currEntry in synonymMap.entries) {
                if (currEntry.value.contains(inspectingWord)) {
                    return currEntry.key
                }
            }
            return null
        }

        fun printAllData(excelFile: File, sheetName: String) {
            val fileInputStream = FileInputStream(excelFile)

            val workbook = XSSFWorkbook(fileInputStream)
            val sheet = workbook.getSheet(sheetName)
            var row = sheet.getRow(0)
            val lastRowIndex = sheet.lastRowNum + 1
            println("Last row index :$lastRowIndex")
            val totalNoOfCols: Int = row.getLastCellNum() - 1
            println("Total columns :$totalNoOfCols")

            val df = DataFormatter()

            for (i in 1..totalNoOfCols) {
                for (j in 0 until lastRowIndex) {
                    row = sheet.getRow(j)
                    val c = row.getCell(i)
                    val cellData: String = df.formatCellValue(c)
                    println(cellData)
                }
                println("-----------")
            }
        }
    }
}
