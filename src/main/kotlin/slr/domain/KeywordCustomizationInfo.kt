package slr.domain

data class KeywordCustomizationInfo(
    val priority: Int = 50, // 0 is a pruned, 1 is commonly general, 100 is extremely specific
    val synonyms: List<String> = emptyList()
) {
    companion object {
        fun default(): KeywordCustomizationInfo =
            KeywordCustomizationInfo(synonyms = emptyList(), priority = 50)
    }
}