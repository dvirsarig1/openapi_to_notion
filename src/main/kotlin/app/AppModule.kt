package app

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import notion.api.v1.NotionClient
import notion.api.v1.http.JavaNetHttpClient
import notion.api.v1.logging.Slf4jLogger
import org.slf4j.LoggerFactory
import java.io.File

class AppModule(private val configFile: File, private val notionToken: String) : AbstractModule() {
    private val logger = LoggerFactory.getLogger(javaClass.simpleName)

    override fun configure() {
        bind(App::class.java).asEagerSingleton()
        bind(File::class.java).toInstance(configFile)
    }

    @Provides
    @Singleton
    fun provideNotionClient(): NotionClient {
        return NotionClient(
            token = notionToken,
            logger = Slf4jLogger(),
            httpClient = JavaNetHttpClient(connectTimeoutMillis = 60000, readTimeoutMillis = 60000)
        )
    }
}

