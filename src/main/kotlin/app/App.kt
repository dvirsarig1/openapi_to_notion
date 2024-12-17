package app

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import config.Config
import jakarta.inject.Inject
import template.PageProcessor
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

class App @Inject constructor(
    private val pageProcessor: PageProcessor,
    configFile: File,
) {

    private val config = Yaml.default.decodeFromStream<Config>(configFile.inputStream())

    fun run(selectedCategories: List<String> = emptyList(), fieldCategory: String? = null) {
        processPages(selectedCategories, fieldCategory)
        processCollectionGeneration()
    }

    private fun processPages(selectedCategories: List<String>, fieldCategory: String?) {
        config.pages.forEach { targetPage ->
            val apiFiles = getApiFiles(targetPage.apiFolder)
            apiFiles.forEach { file ->
                pageProcessor.process(targetPage.notionPageId, file, selectedCategories, fieldCategory)
            }
        }
    }

    private fun processCollectionGeneration() {
        config.generateCollection?.let { generateCollection ->
            val folders = config.pages.map { it.apiFolder }
            GenerateCollection.generate(folders, generateCollection)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun getApiFiles(apiFolder: String): Sequence<Path> {
        val folderPath = Path.of(apiFolder)
        return folderPath.walk().filter { it.extension in arrayOf("yaml", "yml") }
    }
}