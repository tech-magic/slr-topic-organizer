package slr.classification

import slr.domain.IndentationSettings
import slr.domain.ResearchPaper
import slr.topics.TopicPaths_perResearchPaper
import slr.topics.TopicTreeBuilder
import java.lang.IllegalArgumentException

data class ClassificationTopic(
    val topic: String,
    val topicSpeciality: Int
)

data class ClassificationTree(
    val topic: String,
    val topicSpeciality: Int,
    val topicPath: List<ClassificationTopic>,
    val level: Int,
    val researchPapers: List<ResearchPaper>,
    val children: List<ClassificationTree>
)

class ClassificationTreeBuilder(val topicTreeBuilder: TopicTreeBuilder) {

    fun printClassificationTree(
        localClassificationTree: ClassificationTree,
        indentation: IndentationSettings = IndentationSettings.default()
    ) {
        println("${indentation.indent()} # ${localClassificationTree.topic} - ${localClassificationTree.topicSpeciality} {${localClassificationTree.topicPath.joinToString { "[${it.topic}, ${it.topicSpeciality}]" }}}")
        for (currPaper in localClassificationTree.researchPapers) {
            println("${indentation.indent()}   - ${currPaper.referenceIndex} ${currPaper.paperTitle}")
        }
        for (currChild in localClassificationTree.children) {
            printClassificationTree(
                currChild,
                indentation = indentation.copy(indentationStartIndex = indentation.indentationStartIndex + 1)
            )
        }
    }

    fun buildClassificationTree(papersWithTopicPaths: List<TopicPaths_perResearchPaper>): ClassificationTree {

        val papersWithSortedTopicPaths = papersWithTopicPaths.map { paperWithTopicPaths ->
            Pair(
                paperWithTopicPaths.researchPaper,
                paperWithTopicPaths.topicPaths.map { topicPath ->
                    topicPath.map { topic ->
                        ClassificationTopic(topic, topicTreeBuilder.priority(topic))
                    }
                }.flatMap { classificationTopicPath ->
                    levelClassificationTopicPath(classificationTopicPath)
                }.sortedWith(
                    compareBy { classificationTopicList ->
                        classificationTopicList.size
                    }
                )
            )
        }

        val allTopicPaths =
            papersWithSortedTopicPaths.flatMap { paperWithSortedTopicPaths -> paperWithSortedTopicPaths.second }

        val filteredMainTopicPaths = allTopicPaths
            .filter { topicPath -> topicPath.size == 1 }
            .toSet()

        val filteredSubTopicPaths = allTopicPaths
            .filter { topicPath -> topicPath.size > 1 }
            .toSet()

        if (filteredMainTopicPaths.isEmpty()) {
            throw IllegalArgumentException("Input does not contain any root topics")
        } else if (filteredMainTopicPaths.size > 1) {
            throw IllegalArgumentException(
                "Input contains more than one root topics ${
                    filteredMainTopicPaths.map { mainTopic -> "[${mainTopic[0].topic},${mainTopic[0].topicSpeciality}]" }
                        .joinToString { it }
                }"
            )
        }

        val researchPapersPerTopicPathMap = HashMap<List<ClassificationTopic>, List<ResearchPaper>>()
        allTopicPaths.forEach { topicPath ->
            researchPapersPerTopicPathMap[topicPath] = papersWithSortedTopicPaths
                .filter { sortedTopicPathsPerPaper ->
                    sortedTopicPathsPerPaper.second.contains(topicPath)
                }
                .map { topicPathsPerPaper ->
                    topicPathsPerPaper.first
                }
                .toSortedSet(compareBy { it.referenceIndex })
                .toList()
        }
        researchPapersPerTopicPathMap.toSortedMap(compareBy { topicPath -> topicPath.size })

        val mainTopic = filteredMainTopicPaths.toList()[0]

        var classificationTree = ClassificationTree(
            topic = mainTopic[0].topic,
            topicSpeciality = mainTopic[0].topicSpeciality,
            topicPath = mainTopic,
            level = 1,
            researchPapers = researchPapersPerTopicPathMap[mainTopic] ?: emptyList(),
            children = emptyList()
        )

        filteredSubTopicPaths.forEach { topicPath ->
            classificationTree =
                insertToClassificationTree(topicPath, researchPapersPerTopicPathMap, classificationTree)
        }

        val topicMaxLevelMap = HashMap<String, Int>()
        topicMaxLevelMap[mainTopic[0].topic] = 1
        calculateMaxLevelForEachTopic(localClassificationTree = classificationTree, topicMaxLevelMap = topicMaxLevelMap)
        return removeDuplicateTopics(classificationTree, topicMaxLevelMap)
    }

    private fun calculateMaxLevelForEachTopic(
        localClassificationTree: ClassificationTree,
        topicMaxLevelMap: HashMap<String, Int>
    ) {
        for (currChild in localClassificationTree.children) {
            val topicForCurrChild = currChild.topic
            if (topicMaxLevelMap.containsKey(topicForCurrChild)) {
                if ((topicMaxLevelMap[topicForCurrChild] as Int) < currChild.level) {
                    topicMaxLevelMap[topicForCurrChild] = currChild.level
                }
            } else {
                topicMaxLevelMap[topicForCurrChild] = currChild.level
            }
            calculateMaxLevelForEachTopic(currChild, topicMaxLevelMap)
        }
    }

    private fun removeDuplicateTopics(
        localClassificationTree: ClassificationTree,
        topicMaxLevelMap: HashMap<String, Int>
    ): ClassificationTree {
        val childrenToInclude = ArrayList<ClassificationTree>()

        for (currChild in localClassificationTree.children) {
            if (topicMaxLevelMap[currChild.topic] != null && topicMaxLevelMap[currChild.topic] == currChild.level) {
                childrenToInclude.add(removeDuplicateTopics(currChild, topicMaxLevelMap))
            }
        }

        return localClassificationTree.copy(children = childrenToInclude)
    }

    private fun levelClassificationTopicPath(inputClassificationTopicPath: List<ClassificationTopic>): List<List<ClassificationTopic>> {

        val sortedInputClassificationTopicPath = inputClassificationTopicPath
            .toSortedSet(
                compareBy<ClassificationTopic> { classificationTopic ->
                    classificationTopic.topicSpeciality
                }
                    .thenBy { classificationTopic ->
                        classificationTopic.topic
                    }
            )
            .toList()

        return generateAllPossibilitiesOfTopicSignificance(
            sortedInputClassificationTopicPath.groupBy { classificationTopic -> classificationTopic.topicSpeciality }
        )
    }

    private fun generateAllPossibilitiesOfTopicSignificance(
        topicSignificanceBasedTopicGroups: Map<Int, List<ClassificationTopic>>
    ): List<List<ClassificationTopic>> {

        if (topicSignificanceBasedTopicGroups.keys.isEmpty()) {
            return emptyList()
        } else if (topicSignificanceBasedTopicGroups.keys.size == 1) {

            val listOfAllPossibilitiesWithCurrentTopicSignificanceKey = ArrayList<List<ClassificationTopic>>()

            val listOfAllPossibilitiesWithNextTopicSignificanceKeys =
                topicSignificanceBasedTopicGroups[topicSignificanceBasedTopicGroups.keys.first()] as List<ClassificationTopic>

            for (currentTopicPath in listOfAllPossibilitiesWithNextTopicSignificanceKeys) {
                listOfAllPossibilitiesWithCurrentTopicSignificanceKey.add(listOf(currentTopicPath))
            }

            return listOfAllPossibilitiesWithCurrentTopicSignificanceKey.map { currTopicPath ->
                currTopicPath.sortedWith(
                    compareBy<ClassificationTopic> { classificationTopic ->
                        classificationTopic.topicSpeciality
                    }
                        .thenBy { classificationTopic ->
                            classificationTopic.topic
                        }
                )
            }

        } else {

            val allSortedTopicSignificanceBasedGroupKeys = topicSignificanceBasedTopicGroups.keys.sorted()
            val currentTopicSignificanceKey = allSortedTopicSignificanceBasedGroupKeys.first()
            val classificationTopicPathForCurrentKey =
                topicSignificanceBasedTopicGroups[currentTopicSignificanceKey] as List<ClassificationTopic>
            val topicSignificanceBasedGroupsExcludingCurrentTopicSignificanceKey =
                topicSignificanceBasedTopicGroups.filterNot { entry -> entry.key.equals(currentTopicSignificanceKey) }

            val listOfAllPossibilitiesWithCurrentTopicSignificanceKey = ArrayList<List<ClassificationTopic>>()

            for (currentClassificationTopic in classificationTopicPathForCurrentKey) {

                val listOfAllPossibilitiesWithNextTopicSignificanceKeys =
                    generateAllPossibilitiesOfTopicSignificance(
                        topicSignificanceBasedGroupsExcludingCurrentTopicSignificanceKey
                    )

                for (currTopicPath in listOfAllPossibilitiesWithNextTopicSignificanceKeys) {
                    listOfAllPossibilitiesWithCurrentTopicSignificanceKey.add(
                        listOf(
                            currentClassificationTopic,
                            *currTopicPath.toTypedArray()
                        )
                    )
                }
            }

            return listOfAllPossibilitiesWithCurrentTopicSignificanceKey.map { currTopicPath ->
                currTopicPath.sortedWith(
                    compareBy<ClassificationTopic> { classificationTopic ->
                        classificationTopic.topicSpeciality
                    }
                        .thenBy { classificationTopic ->
                            classificationTopic.topic
                        }
                )
            }
        }
    }

    private fun insertToClassificationTree(
        currTopicPath: List<ClassificationTopic>,
        researchPapersPerTopicPathMap: Map<List<ClassificationTopic>, List<ResearchPaper>>,
        parentClassificationTree: ClassificationTree
    ): ClassificationTree {

        // println("##### ${inputClassificationTree.level} ${inputClassificationTree.topic} => ${currTopicPath.joinToString { it.topic }}")

        if (!researchPapersPerTopicPathMap.containsKey(currTopicPath)) {
            return parentClassificationTree
        } else if (parentClassificationTree.level >= currTopicPath.size) {
            return parentClassificationTree
        } else {

            val topicForNextLevel =
                currTopicPath[parentClassificationTree.level] // list indexes start from 0 (therefore no need to do +1 here)
            val matchingChildTopicsAtNextLevel = parentClassificationTree.children.filter { classificationTree ->
                classificationTree.topic == topicForNextLevel.topic && classificationTree.topicSpeciality == topicForNextLevel.topicSpeciality
            }
            val nonMatchingChildTopicsAtNextLevel =
                parentClassificationTree.children.filterNot { classificationTree ->
                    classificationTree.topic == topicForNextLevel.topic && classificationTree.topicSpeciality == topicForNextLevel.topicSpeciality
                }

            if (parentClassificationTree.level == (currTopicPath.size - 1)) {
                if (matchingChildTopicsAtNextLevel.isNotEmpty()) {
                    return parentClassificationTree.copy(
                        children = listOf(
                            matchingChildTopicsAtNextLevel[0].copy(
                                researchPapers = listOf(
                                    *matchingChildTopicsAtNextLevel[0].researchPapers.toTypedArray(),
                                    *(researchPapersPerTopicPathMap[currTopicPath] as List<ResearchPaper>).toTypedArray()
                                ).toSet().toList()
                            ),
                            *nonMatchingChildTopicsAtNextLevel.toTypedArray()
                        ).sortedWith(compareBy<ClassificationTree> { it.topicSpeciality }.thenBy { it.topic })
                    )
                } else {
                    return parentClassificationTree.copy(
                        children = listOf(
                            ClassificationTree(
                                topic = topicForNextLevel.topic,
                                topicSpeciality = topicForNextLevel.topicSpeciality,
                                topicPath = currTopicPath,
                                level = parentClassificationTree.level + 1,
                                researchPapers = listOf(*(researchPapersPerTopicPathMap[currTopicPath] as List<ResearchPaper>).toTypedArray()),
                                children = emptyList()
                            ),
                            *nonMatchingChildTopicsAtNextLevel.toTypedArray()
                        ).sortedWith(compareBy<ClassificationTree> { it.topicSpeciality }.thenBy { it.topic })
                    )
                }
            } else {
                // which means; inputClassificationTree.level < currTopicPath.size AND researchPapersPerTopicPathMap.containsKey(currTopicPath)
                if (matchingChildTopicsAtNextLevel.isNotEmpty()) {
                    return parentClassificationTree.copy(
                        children = listOf(
                            insertToClassificationTree(
                                currTopicPath,
                                researchPapersPerTopicPathMap,
                                matchingChildTopicsAtNextLevel[0]
                            ),
                            *nonMatchingChildTopicsAtNextLevel.toTypedArray()
                        ).sortedWith(compareBy<ClassificationTree> { it.topicSpeciality }.thenBy { it.topic })
                    )
                } else {
                    return parentClassificationTree.copy(
                        children = listOf(
                            insertToClassificationTree(
                                currTopicPath,
                                researchPapersPerTopicPathMap,
                                ClassificationTree(
                                    topic = topicForNextLevel.topic,
                                    topicSpeciality = topicForNextLevel.topicSpeciality,
                                    topicPath = currTopicPath,
                                    level = parentClassificationTree.level + 1,
                                    researchPapers = emptyList(),
                                    children = emptyList()
                                )
                            ),
                            *nonMatchingChildTopicsAtNextLevel.toTypedArray()
                        ).sortedWith(compareBy<ClassificationTree> { it.topicSpeciality }.thenBy { it.topic })
                    )
                }
            }
        }
    }
}

