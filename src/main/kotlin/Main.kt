import com.google.inject.Guice
import app.App
import app.AppModule
import java.io.File

fun main(args: Array<String>) {
    val configFile = File("config.yaml")
    if (!configFile.exists()) {
        error("Configuration file 'config.yaml' not found in the specified directory.")
    }
    val notionToken = System.getenv("NOTION_TOKEN") ?: error("Missing NOTION_TOKEN environment variable")
    val tags = args.find { it.startsWith("--tags=") }?.removePrefix("--tags=")?.split(",") ?: emptyList()
    val injector = Guice.createInjector(AppModule(configFile, notionToken))
    val app = injector.getInstance(App::class.java)
    app.run(tags)
}



