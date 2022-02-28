import slr.facade.ClassificationFacade
import slr.work.sentimentanalysis.SentimentAnalysisConfiguration
import java.io.File

fun main(args: Array<String>) {

    ClassificationFacade(
        customConfiguration = SentimentAnalysisConfiguration(),
        interestedTopic = "sentiment analysis",
        targetDirectory = File("${System.getProperty("user.dir")}/results")
    )
}
