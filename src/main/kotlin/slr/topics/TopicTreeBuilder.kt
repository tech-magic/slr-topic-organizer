package slr.topics

import slr.domain.IndentationSettings
import slr.domain.KeywordCustomizationInfo
import slr.domain.ResearchPaper
import slr.maps.AssociatedKeywords_perResearchPaper
import slr.maps.KeywordMapsLoader

data class TopicTree(
    val topic: String,
    val topicPath: List<String>,
    val depth: Int,
    val researchPapers: List<ResearchPaper>,
    val childKeywords: List<String>,
    val children: List<TopicTree> = emptyList()
)

data class TopicPaths_perResearchPaper(
    val researchPaper: ResearchPaper,
    val topicPaths: List<List<String>>
)

class TopicTreeBuilder(val keywordMaps: KeywordMapsLoader) {

    val defaultKeywordCustomizationInfo = KeywordCustomizationInfo.default()
    var allPriorityValues: List<Int> = emptyList()

    fun priority(topic: String) = keywordMaps.synonymMap[topic]?.priority ?: defaultKeywordCustomizationInfo.priority

    init {
        this.allPriorityValues = listOf(
            defaultKeywordCustomizationInfo.priority,
            *this.keywordMaps.synonymMap.map { it.value.priority }.toTypedArray()
        ).toSet().toList()
    }

    private fun isAPriorityByPassedTopicPath(topicPath: List<String>): Boolean {
        var result = false
        for (currTopicIndex in 0 until topicPath.size - 2) {
            val currTopic = topicPath[currTopicIndex]
            val nextTopic = topicPath[currTopicIndex + 1]
            if (priority(currTopic) < priority(nextTopic)) {
                for (currPriority in allPriorityValues) {
                    if (currPriority > priority(currTopic) && currPriority < priority(nextTopic)) {
                        result = true
                    }
                }
            }
        }
        // println("$$$ ${result} ${topicPath.joinToString { it }}")
        return result
    }

    fun printTopicTree(
        localTopicTree: TopicTree,
        indentation: IndentationSettings = IndentationSettings.default()
    ) {
        println("${indentation.indent()} # ${localTopicTree.topic} - [${localTopicTree.topicPath.joinToString { it }}]")
        println("${indentation.indent()}   * [${localTopicTree.childKeywords.joinToString { it }}]")
        for (currPaper in localTopicTree.researchPapers) {
            println("${indentation.indent()}   - ${currPaper.referenceIndex} ${currPaper.paperTitle}")
        }
        for (currChild in localTopicTree.children) {
            printTopicTree(
                currChild,
                indentation = indentation.copy(indentationStartIndex = indentation.indentationStartIndex + 1)
            )
        }
    }

    fun printPapersWithTopicPathsMap(topicPathsPerResearchPaperList: List<TopicPaths_perResearchPaper>) {
        topicPathsPerResearchPaperList.forEach { currPaperInfo ->
            println("[${currPaperInfo.researchPaper.referenceIndex}] ${currPaperInfo.researchPaper.paperTitle}")

            currPaperInfo.topicPaths
                .sortedWith(compareBy { it.size })
                .forEach { it ->
                    println("   ${it.joinToString { it }}")
                }
        }
    }

    fun buildPapersWithTopicPathsMap(topicTree: TopicTree): List<TopicPaths_perResearchPaper> {
        val allResearchPapersMap = keywordMaps.allResearchPapersWithTheirAssociatedKeywords

        return buildPapersWithTopicPathsMapRecursive(topicTree).toList().map { currPair ->
            TopicPaths_perResearchPaper(
                researchPaper = (allResearchPapersMap[currPair.first] as AssociatedKeywords_perResearchPaper).record,
                topicPaths = currPair.second.map { currTopicPath ->
                    currTopicPath.sortedWith { a, b ->
                        when {
                            priority(a) > priority(b) -> 1
                            priority(a) < priority(b) -> -1
                            else -> 0
                        }
                    }
                }.filter { currTopicPath -> !isAPriorityByPassedTopicPath(currTopicPath) }.toSet().toList()
            )
        }.sortedWith { a, b ->
            when {
                a.researchPaper.referenceIndex > b.researchPaper.referenceIndex -> 1
                a.researchPaper.referenceIndex < b.researchPaper.referenceIndex -> -1
                else -> 0
            }
        }
    }

    private fun buildPapersWithTopicPathsMapRecursive(topicTree: TopicTree): Map<Int, List<List<String>>> {
        val papersWithTopicPaths = HashMap<Int, List<List<String>>>()

        for (currPaper in topicTree.researchPapers) {
            papersWithTopicPaths[currPaper.referenceIndex] =
                papersWithTopicPaths[currPaper.referenceIndex]?.let { topicPaths ->
                    listOf(*topicPaths.toTypedArray(), topicTree.topicPath)
                } ?: listOf(topicTree.topicPath)
        }

        for (currChildTree in topicTree.children) {
            buildPapersWithTopicPathsMapRecursive(currChildTree).forEach { (researchPaperRefIndex, topicPathList) ->
                papersWithTopicPaths[researchPaperRefIndex] =
                    papersWithTopicPaths[researchPaperRefIndex]?.let { topicPaths ->
                        listOf(*topicPaths.toTypedArray(), *topicPathList.toTypedArray())
                    } ?: listOf(*topicPathList.toTypedArray())
            }
        }

        return papersWithTopicPaths
    }

    fun buildTopicTree(
        interestedTopic: String,
        prunedTopicsList: List<String> = emptyList(),
        indentation: IndentationSettings = IndentationSettings.default(),
        thresholdChildResearchPaperCount: Int = 8,
        thresholdTreeDepth: Int = 8,
        thresholdResearchPaperPrintDepth: Int = 8,
        thresholdResearchPaperPrintCount: Int = 20
    ) = buildTopicTreeRecursive(
        interestedTopic = interestedTopic,
        prunedTopicsList = prunedTopicsList,
        indentation = indentation,
        thresholdChildResearchPaperCount = thresholdChildResearchPaperCount,
        thresholdTreeDepth = thresholdTreeDepth,
        thresholdResearchPaperPrintDepth = thresholdResearchPaperPrintDepth,
        thresholdResearchPaperPrintCount = thresholdResearchPaperPrintCount,
        recursiveTopicPath = emptyList(),
        recursiveTreeLevel = 0
    )

    private fun buildTopicTreeRecursive(
        interestedTopic: String,
        prunedTopicsList: List<String>,
        indentation: IndentationSettings,
        recursiveTopicPath: List<String>,
        recursiveTreeLevel: Int,
        thresholdChildResearchPaperCount: Int,
        thresholdTreeDepth: Int,
        thresholdResearchPaperPrintDepth: Int,
        thresholdResearchPaperPrintCount: Int
    ): TopicTree {

        // println("################ ${recursiveTopicPath.joinToString { it }}")

        val currTopicPath = listOf(*recursiveTopicPath.toTypedArray(), interestedTopic)
        val nextIndentation = indentation.copy(indentationStartIndex = indentation.indentationStartIndex + 1)

        val nestedResearchPapersMap = keywordMaps.allResearchPapersWithTheirAssociatedKeywords
            .filter { entry -> entry.value.keywords.containsAll(listOf(*currTopicPath.toTypedArray())) }

        val nestedResearchPapers = nestedResearchPapersMap.values.map { it.record }.toSet().toList()
        val nestedChildKeywords = nestedResearchPapersMap.values.flatMap { it.keywords }
            .toSet().toList()
            .filterNot { listOf(*currTopicPath.toTypedArray()).contains(it) }

        //println("${indent(indentation)} # $interestedTopic => (${nestedChildKeywords.size} keywords, ${nestedResearchPapers.size} papers)")
        //println("${indent(nextIndentation)} [topic path] ${currTopicPath.joinToString(separator = " -> ", transform = { it })}")
        //println("${indent(nextIndentation)} child keywords -> $nestedChildKeywords")

        val childTopicTrees = ArrayList<TopicTree>()
        if (recursiveTreeLevel <= thresholdTreeDepth) {

            if (nestedResearchPapers.size >= thresholdChildResearchPaperCount) {

                if (recursiveTreeLevel >= thresholdResearchPaperPrintDepth && nestedResearchPapers.size <= thresholdResearchPaperPrintCount) {
                    nestedResearchPapers.forEach {
                        //println("${indent(nextIndentation)} [${it.referenceIndex}] ${it.paperTitle}")
                    }
                }

                nestedChildKeywords.sorted().forEach { childKeyword ->

                    if (!currTopicPath.contains(childKeyword) &&
                        !prunedTopicsList.contains(childKeyword)
                    ) {

                        childTopicTrees.add(
                            buildTopicTreeRecursive(
                                interestedTopic = childKeyword,
                                indentation = nextIndentation,
                                prunedTopicsList = prunedTopicsList,
                                recursiveTopicPath = currTopicPath,
                                recursiveTreeLevel = recursiveTreeLevel + 1,
                                thresholdChildResearchPaperCount = thresholdChildResearchPaperCount,
                                thresholdTreeDepth = thresholdTreeDepth,
                                thresholdResearchPaperPrintDepth = thresholdResearchPaperPrintDepth,
                                thresholdResearchPaperPrintCount = thresholdResearchPaperPrintCount
                            )
                        )
                    }
                }
            }
        }

        return TopicTree(
            topic = interestedTopic,
            topicPath = currTopicPath,
            depth = recursiveTreeLevel,
            researchPapers = nestedResearchPapers,
            childKeywords = nestedChildKeywords,
            children = childTopicTrees
        )
    }
}
