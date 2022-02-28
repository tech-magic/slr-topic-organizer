package slr.excel

import java.io.File

class InputFileConstants {
    companion object {
        val EXCEL_FILE = File(
            System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "phd-work" + File.separator + "merged_references.xlsx"
        )

        const val EXCEL_SHEET_NAME = "References"

        const val KEYWORDS_COLUMN_INDEX = 20
        const val AUTHORS_COLUMN_INDEX = 1
        const val TITLE_COLUMN_INDEX = 3
        const val ABSTRACT_COLUMN_INDEX = 19
    }
}
