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
    val routesCategory = args.find { it.startsWith("--routes-category=") }?.removePrefix("--tags=")?.split(",") ?: emptyList()
    val fieldCategory = args.find { it.startsWith("--field-category=") }?.removePrefix("--field-category=")
    val injector = Guice.createInjector(AppModule(configFile, notionToken))
    val app = injector.getInstance(App::class.java)
    app.run(routesCategory, fieldCategory)
}

