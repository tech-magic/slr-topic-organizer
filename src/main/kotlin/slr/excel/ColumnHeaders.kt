package slr.excel

class ColumnHeaders {
    companion object {
        fun printColumnHeaders() {
            ExcelUtils.printColumnHeaders(
                excelFile = InputFileConstants.EXCEL_FILE,
                sheetName = InputFileConstants.EXCEL_SHEET_NAME
            )
        }
    }
}