package slr.facade

import slr.classification.ClassificationTreeBuilder
import slr.config.CustomConfiguration
import slr.maps.KeywordMapsLoader
import slr.renderer.ClassificationTreeRenderer
import slr.topics.TopicTreeBuilder
import java.io.File

class ClassificationFacade(
    customConfiguration: CustomConfiguration,
    interestedTopic: String,
    targetDirectory: File
) {

    init {
        val keywordMapsLoader = KeywordMapsLoader(
            keywordCustomizationMap = customConfiguration.getKeywordCustomizationMap(),
            unwantedKeywords = customConfiguration.getUnwantedKeywords(),
            userAssignedKeywordsForResearchPapers = customConfiguration.getCustomKeywordsForResearchPapersMap()
        )
        val topicTreeBuilder = TopicTreeBuilder(keywordMapsLoader)

        val topicTree = topicTreeBuilder.buildTopicTree(
            interestedTopic = interestedTopic,
            prunedTopicsList = customConfiguration.getPrunedKeywordsListInTopicTree()
        )
        topicTreeBuilder.printTopicTree(topicTree)

        println("Total # of research papers -> ${keywordMapsLoader.allResearchPapersWithTheirAssociatedKeywords.size}")

        val papersForTheTopic = topicTreeBuilder.buildPapersWithTopicPathsMap(topicTree)
        println("Total # of research papers for {$interestedTopic} -> ${papersForTheTopic.size}")
        println("Topic Coverage -> ${papersForTheTopic.size.toDouble() / keywordMapsLoader.allResearchPapersWithTheirAssociatedKeywords.size.toDouble()}")
        println("================")

        val classificationTreeBuilder = ClassificationTreeBuilder(topicTreeBuilder)
        val classificationTree = classificationTreeBuilder.buildClassificationTree(papersForTheTopic)

        ClassificationTreeRenderer.initializeTargetDirectory(targetDirectory)
        ClassificationTreeRenderer.renderClassificationTree(classificationTree, keywordMapsLoader, targetDirectory)
    }
}
