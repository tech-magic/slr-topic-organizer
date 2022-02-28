package slr.work.sentimentanalysis

import slr.config.CustomConfiguration
import slr.domain.KeywordCustomizationInfo

class SentimentAnalysisConfiguration: CustomConfiguration {

    override fun getKeywordCustomizationMap(): Map<String, KeywordCustomizationInfo> =
        mapOf(
            "blockchain" to KeywordCustomizationInfo(synonyms = listOf("blockchain and crypto currency", "block-chain", "blockchains", "blockchain 3.0", "blockchain information", "block", "blockchain technology", "block chain")),
            "bitcoin" to KeywordCustomizationInfo(synonyms = listOf("bitcoin (btc)", "bitcoin price", "bitcoins", "bitcoin sentiment", "bitcoin futures", "bitcoin price trends", "bitcoin prediction", "bitcoin dollar rate")),
            "lstm" to KeywordCustomizationInfo(synonyms = listOf("long short-term memory", "long short term memory", "long-short-term-memory", "long short-term memory networks", "long short-term memory(lstm)")),
            "machine learning" to KeywordCustomizationInfo(synonyms = listOf("machine learning (ml)", "machine learning approach", "machine learning approaches", "data mining and machine learning", "machine learning methods", "machine learning algorithms", "machine learning models", "machine learning module")),
            "svm" to KeywordCustomizationInfo(synonyms = listOf("support vector machines", "support vector machine", "support vector machine (svm)")),
            "cnn" to KeywordCustomizationInfo(synonyms = listOf("convolutional neural networks", "convolutional neural network", "optimal cnn")),
            "covid-19" to KeywordCustomizationInfo(synonyms = listOf("covid-19 pandemic", "sars-cov-2")),
            "iot" to KeywordCustomizationInfo(synonyms = listOf("internet of things (iot)")),
            "time series analysis" to KeywordCustomizationInfo(priority = 30, synonyms = listOf("time-series analysis", "time-series", "time series and forecasting", "time series", "time-series data", "time-series forecasting", "time-series-analysis")),
            "cryptocurrency" to KeywordCustomizationInfo(synonyms = listOf("cryptocurrencies", "crypto currency", "crypto-currency", "the cryptocurrency market", "cryptocurrency market", "cryptocurrency index", "cryptocurrency mining", "cryptocurrency exchanges")),
            "price prediction" to KeywordCustomizationInfo(synonyms = listOf("prediction", "forecasting", "prediction", "cryptocurrency price prediction", "bitcoin price prediction", "cryptocurrency prediction", "price prediction models")),
            "knn" to KeywordCustomizationInfo(synonyms = listOf("k-means algorithm")),
            "natural language processing" to KeywordCustomizationInfo(priority=30, synonyms = listOf("language processing", "nlp", "neural language processing")),
            "text mining" to KeywordCustomizationInfo(synonyms = listOf("text-mining")),
            "stock market" to KeywordCustomizationInfo(synonyms = listOf("stock markets")),
            "application" to KeywordCustomizationInfo(synonyms = listOf("applications")),
            "anomaly detection" to KeywordCustomizationInfo(synonyms = listOf("anomalies")),
            "anonymity" to KeywordCustomizationInfo(synonyms = listOf("online anonymity")),
            "short-term forecasting" to KeywordCustomizationInfo(synonyms = listOf("short-term forecasting model", "short-term prediction")),
            "web mining" to KeywordCustomizationInfo(synonyms = listOf("web scraping", "web-mining")),
            "volatility prediction" to KeywordCustomizationInfo(synonyms = listOf("volatility modeling", "volatility forecasting", "volatility")),
            "sentiment analysis" to KeywordCustomizationInfo(priority = 1, synonyms = listOf("sentiment", "twitter sentiment analysis", "twitter", "social networking (online)", "90.01 social phenomena", "social media", "social networking", "reddit")),
            "arima" to KeywordCustomizationInfo(synonyms = listOf("auto-regressive integrated moving average with exogenous input (arimax)")),
            "visualization platforms" to KeywordCustomizationInfo(synonyms = listOf("visualization")),
            "market prediction" to KeywordCustomizationInfo(synonyms = listOf("market analysis", "market trends", "market movement")),
            "bots" to KeywordCustomizationInfo(priority = 50, synonyms = emptyList()),
            "return volatility" to KeywordCustomizationInfo(priority = 50, synonyms = emptyList())
        )

    override fun getUnwantedKeywords(): List<String> =
        listOf(
            "price", "patterns", "buildings", "software engineering", "assertions",
            "overwhelming\\", "information and communication technology", "filter\\", "modeling",
            "software", "trend", "hybrid architecture\\", "keywords", "var", "elsevier\\", "companies", "computers"
        )

    // "artificial intelligence", "machine learning"
    override fun getPrunedKeywordsListInTopicTree(): List<String> =
        listOf(
            "bitcoin", "blockchain", "cryptocurrency", "data science", "deep learning", "big data",
            "ethereum", "litecoin", "price prediction", "classification", "front end", "data handling", "data fusion", "computer vision",
            "investments", "validation measures", "costs", "dogecoin", "spark", "pagerank"
        )

    override fun getCustomKeywordsForResearchPapersMap(): Map<Int, List<String>> =
        mapOf(
            190 to listOf("natural language processing")
        )
}