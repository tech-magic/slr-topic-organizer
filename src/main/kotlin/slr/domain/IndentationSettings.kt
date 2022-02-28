package slr.domain

data class IndentationSettings(
    val indentationStartIndex: Int,
    val indentationUnitString: String,
    val indentationUnitSpan: Int,
    val indentationUnitSuffix: String
) {
    fun indent() =
        this.indentationUnitString.repeat(
            n = this.indentationStartIndex * this.indentationUnitSpan
        ).plus(this.indentationUnitSuffix)

    companion object {
        fun default(): IndentationSettings = IndentationSettings(
            indentationStartIndex = 0,
            indentationUnitString = " ",
            indentationUnitSpan = 3,
            indentationUnitSuffix = ""
        )
    }
}
