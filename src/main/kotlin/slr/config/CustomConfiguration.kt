package slr.config

import slr.domain.KeywordCustomizationInfo

interface CustomConfiguration {
    fun getKeywordCustomizationMap(): Map<String, KeywordCustomizationInfo>
    fun getUnwantedKeywords(): List<String>
    fun getPrunedKeywordsListInTopicTree(): List<String>
    fun getCustomKeywordsForResearchPapersMap(): Map<Int, List<String>>
}
