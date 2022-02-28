package slr.renderer

import slr.classification.ClassificationTree
import slr.domain.IndentationSettings
import slr.maps.KeywordMapsLoader
import java.io.File
import java.util.*

class ClassificationTreeRenderer {
    companion object {

        fun initializeTargetDirectory(targetDirectory: File) {
            if (targetDirectory.exists()) {
                targetDirectory.deleteRecursively()
            }
            targetDirectory.mkdirs()
        }

        fun renderClassificationTree(classificationTree: ClassificationTree, keywordMapsLoader: KeywordMapsLoader, targetDirectory: File) {
            val classificationTreeAsHTML = renderClassificationTreeRecursive(classificationTree, keywordMapsLoader, IndentationSettings.default())
            val template = File(File("${System.getProperty("user.dir")}/src/main/resources/tree_template"), "tree.html").readText()
            val content = template.replace("<!-- ### ALL TREE ITEMS GO HERE ### -->", classificationTreeAsHTML)
            File(targetDirectory, "${classificationTree.topic.replace(" ", "_")}.html").writeText(content)
        }

        private fun renderClassificationTreeRecursive(
            localClassificationTree: ClassificationTree,
            keywordMapsLoader: KeywordMapsLoader,
            indentation: IndentationSettings
        ): String {
            val stringBuilder = StringBuilder()

            if (indentation.indentationStartIndex == IndentationSettings.default().indentationStartIndex) {
                stringBuilder.appendLine("${indentation.indent()} <ul class='tree'>")
            } else {
                stringBuilder.appendLine("${indentation.indent()} <ul>")
            }

            val topicId = "${localClassificationTree.topic.replace(" ", "_").replace("'", "_")}_${UUID.randomUUID()}"
            val topicCaption =
                if (localClassificationTree.children.isNotEmpty())
                    "[PARENT] ${localClassificationTree.topic}"
                else
                    "[LEAF] ${localClassificationTree.topic}"

            stringBuilder.appendLine("${indentation.indent()} <li class='section'>")
            stringBuilder.appendLine("${indentation.indent()} <input type='checkbox' id='${topicId}_checkbox'/>")
            stringBuilder.appendLine("${indentation.indent()} <label for='${topicId}_checkbox' id='${topicId}_label'>${topicCaption} (${localClassificationTree.researchPapers.size} papers)</label>")

            //stringBuilder.appendLine("${indentation.indent()} # ${localClassificationTree.topic} - ${localClassificationTree.topicSpeciality} {${localClassificationTree.topicPath.joinToString { "[${it.topic}, ${it.topicSpeciality}]" }}}")

            //stringBuilder.appendLine("${indentation.indent()} <label id='${topicId}_papers_label' class='collapse' for='${topicId}_papers_check'>${localClassificationTree.researchPapers.size} papers...</label>")

            //stringBuilder.appendLine("${indentation.indent()} <span id='${topicId}_papers_span'> ")

            val keywordsPerResearchPaperMap = keywordMapsLoader.allResearchPapersWithTheirAssociatedKeywords
            for (currPaper in localClassificationTree.researchPapers) {
                val paperKeywords = keywordsPerResearchPaperMap[currPaper.referenceIndex]?.let {
                        keywordInfo -> "[${keywordInfo.keywords.joinToString { it }}]"
                } ?: "[]"
                stringBuilder.appendLine("${indentation.indent()} <p> ${currPaper.referenceIndex} ${currPaper.paperTitle} | Keywords -> $paperKeywords </p>")
            }

            //stringBuilder.appendLine("${indentation.indent()} </span> ")

            for (currChild in localClassificationTree.children) {
                stringBuilder.appendLine(
                    renderClassificationTreeRecursive(
                        localClassificationTree = currChild,
                        keywordMapsLoader = keywordMapsLoader,
                        indentation = indentation.copy(indentationStartIndex = indentation.indentationStartIndex + 1)
                    )
                )
            }

            stringBuilder.appendLine("${indentation.indent()} </li>")
            stringBuilder.appendLine("${indentation.indent()} </ul>")

            return stringBuilder.toString()
        }
    }
}
