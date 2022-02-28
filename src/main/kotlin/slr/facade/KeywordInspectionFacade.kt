package slr.facade

import slr.config.CustomConfiguration
import slr.maps.KeywordMapsLoader

class KeywordInspectionFacade(customConfiguration: CustomConfiguration) {
    init {
        val keywordMapsLoader = KeywordMapsLoader(
            keywordCustomizationMap = customConfiguration.getKeywordCustomizationMap(),
            unwantedKeywords = customConfiguration.getUnwantedKeywords(),
            userAssignedKeywordsForResearchPapers = customConfiguration.getCustomKeywordsForResearchPapersMap()
        )

        val allKeywordsWithRelatedKeywords = keywordMapsLoader.allKeywordsWithRelatedKeywordsAndAssociatedResearchPapers

        allKeywordsWithRelatedKeywords.forEach { (t, u) ->
            println("$t (${u.researchPapers.size} papers) => [ ${u.associatedKeywords.joinToString { it }} ]")
            // println("$t (${u.researchPapers.size} papers)")
        }
    }
}
