package slr.maps

import slr.excel.InputFileConstants
import slr.domain.KeywordCustomizationInfo
import slr.domain.ResearchPaper
import slr.excel.ExcelUtils
import java.lang.IllegalArgumentException

data class CountAndResearchPapersInfo_perKeyword(
    val totalCount: Int,
    val researchPapers: List<ResearchPaper> = ArrayList()
)

data class AssociatedKeywordsAndAssociatedResearchPapers_perKeyword(
    val totalCount: Int,
    val researchPapers: List<ResearchPaper> = ArrayList(),
    val associatedKeywords: List<String> = ArrayList()
)

data class AssociatedKeywords_perResearchPaper(
    val record: ResearchPaper,
    val keywords: List<String> = ArrayList()
)

class KeywordMapsLoader(
    keywordCustomizationMap: Map<String, KeywordCustomizationInfo>,
    unwantedKeywords: List<String>,
    userAssignedKeywordsForResearchPapers: Map<Int, List<String>>
) {

    var allKeywordsWithCountAndAssociatedResearchPapers: Map<String, CountAndResearchPapersInfo_perKeyword> = emptyMap()
    var allResearchPapersWithTheirAssociatedKeywords: Map<Int, AssociatedKeywords_perResearchPaper> = emptyMap()
    var allKeywordsWithRelatedKeywordsAndAssociatedResearchPapers:  Map<String, AssociatedKeywordsAndAssociatedResearchPapers_perKeyword> = emptyMap()

    var synonymMap: Map<String, KeywordCustomizationInfo> = emptyMap()
    private var uselessKeywords: List<String> = emptyList()
    private var userAssignedKeywords: Map<Int, List<String>> = emptyMap()

    init {
        synonymMap = validatedKeywordCustomizationMap(keywordCustomizationMap)
        uselessKeywords = unwantedKeywords
        userAssignedKeywords = userAssignedKeywordsForResearchPapers
        allKeywordsWithCountAndAssociatedResearchPapers = constructAllKeywordsWithCountAndAssociatedResearchPapers()
        allResearchPapersWithTheirAssociatedKeywords = constructAllResearchPapersWithTheirAssociatedKeywords()
        allKeywordsWithRelatedKeywordsAndAssociatedResearchPapers =
            constructAllKeywordsWithRelatedKeywordsAndAssociatedResearchPapers()
    }

    private fun constructAllKeywordsWithCountAndAssociatedResearchPapers(): Map<String, CountAndResearchPapersInfo_perKeyword> {
        return ExcelUtils.getKeywordsCountMap(
            excelFile = InputFileConstants.EXCEL_FILE,
            sheetName = InputFileConstants.EXCEL_SHEET_NAME,
            keywordsColumnIndex = InputFileConstants.KEYWORDS_COLUMN_INDEX,
            authorsColumnIndex = InputFileConstants.AUTHORS_COLUMN_INDEX,
            titleColumnIndex = InputFileConstants.TITLE_COLUMN_INDEX,
            abstractColumnIndex = InputFileConstants.ABSTRACT_COLUMN_INDEX,
            synonymMap = synonymMap.map { (key, value) -> key to value.synonyms }.toMap(),
            unwantedKeywords = uselessKeywords,
            userAssignedKeywords = userAssignedKeywords
        )
    }

    private fun constructAllResearchPapersWithTheirAssociatedKeywords(): Map<Int, AssociatedKeywords_perResearchPaper> {
        val paperWithKeywordsMap = HashMap<Int, AssociatedKeywords_perResearchPaper>()

        for (currEntry in allKeywordsWithCountAndAssociatedResearchPapers.entries) {
            val currKeyword = currEntry.key
            currEntry.value.researchPapers.forEach { currPaperRecord ->
                paperWithKeywordsMap[currPaperRecord.referenceIndex] =
                    paperWithKeywordsMap[currPaperRecord.referenceIndex]?.let { paperWithKeywords ->
                        val keywordsList =
                            if (paperWithKeywords.keywords.contains(currKeyword)) {
                                paperWithKeywords.keywords
                            } else {
                                listOf(*paperWithKeywords.keywords.toTypedArray(), currKeyword)
                            }
                        AssociatedKeywords_perResearchPaper(paperWithKeywords.record, keywordsList)
                    } ?: AssociatedKeywords_perResearchPaper(currPaperRecord, listOf(currKeyword))
            }
        }

        return paperWithKeywordsMap
    }

    private fun constructAllKeywordsWithRelatedKeywordsAndAssociatedResearchPapers():
            Map<String, AssociatedKeywordsAndAssociatedResearchPapers_perKeyword> {

        val relatedKeywordsMap = HashMap<String, AssociatedKeywordsAndAssociatedResearchPapers_perKeyword>()
        val paperKeywordMap = allResearchPapersWithTheirAssociatedKeywords

        for (currEntry in allKeywordsWithCountAndAssociatedResearchPapers) {
            val currMainKeyword = currEntry.key
            val currSubKeywords = ArrayList<String>()
            currEntry.value.researchPapers.forEach { researchPaperRecord ->
                val keywordsForCurrentPaper =
                    paperKeywordMap[researchPaperRecord.referenceIndex]?.let { it.keywords } ?: listOf()
                for (currSubKeyword in keywordsForCurrentPaper) {
                    if (currSubKeyword != currMainKeyword && !currSubKeywords.contains(currSubKeyword)) {
                        currSubKeywords.add(currSubKeyword)
                    }
                }
            }
            relatedKeywordsMap[currMainKeyword] = AssociatedKeywordsAndAssociatedResearchPapers_perKeyword(
                totalCount = currEntry.value.totalCount,
                researchPapers = currEntry.value.researchPapers,
                associatedKeywords = currSubKeywords
            )
        }

        return relatedKeywordsMap.toList()
            .sortedByDescending { (_, value) -> value.totalCount }
            .toMap()
    }

    private fun validatedKeywordCustomizationMap(keywordCustomizationMap: Map<String, KeywordCustomizationInfo>): Map<String, KeywordCustomizationInfo> {

        val detectedErrors = ArrayList<String>()
        val allKeywordsInUppercase = keywordCustomizationMap.keys.map { currKeyword -> currKeyword.trim().uppercase() }
        keywordCustomizationMap.values.forEach { currCustomizationInfo ->
            for (currSynonym in currCustomizationInfo.synonyms) {
                if (allKeywordsInUppercase.contains(currSynonym.uppercase())) {
                    detectedErrors.add("Keyword \"$currSynonym\" has duplicate entries")
                }
            }
        }
        keywordCustomizationMap.forEach { (currKeyword, currCustomizationInfo) ->
            currCustomizationInfo.synonyms.forEach { currSynonym ->
                if (allKeywordsInUppercase.contains(currSynonym.trim().uppercase())) {
                    detectedErrors.add("Keyword \"$currSynonym\" is repeated as a synonym (within the synonym list for keyword \"$currKeyword\")")
                }
            }
        }

        if (detectedErrors.isNotEmpty()) {
            throw IllegalArgumentException(detectedErrors.joinToString { "${it}\r\n" })
        }

        return keywordCustomizationMap
    }
}
