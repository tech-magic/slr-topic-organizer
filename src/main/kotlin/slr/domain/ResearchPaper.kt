package slr.domain

data class ResearchPaper(
    val referenceIndex: Int,
    val authors: String? = null,
    val paperTitle: String? = null,
    val paperAbstract: String? = null
)