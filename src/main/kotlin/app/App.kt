package app

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import config.Config
import notion.NotionAdapter
import template.NotionTemplate
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.parser.core.models.SwaggerParseResult
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

class App @Inject constructor(
    private val notionAdapter: NotionAdapter,
    configFile: File,
) {

    private val config = Yaml.default.decodeFromStream<Config>(configFile.inputStream())
    private val logger = LoggerFactory.getLogger(javaClass.simpleName)

    @OptIn(ExperimentalPathApi::class)
    fun run(selectedCategories: List<String> = emptyList()) {
        notionAdapter.use {
            config.pages.forEach { targetPage ->
                val apiFolder = Path.of(targetPage.apiFolder)
                val apiFiles = apiFolder.walk().filter { it.extension in arrayOf("yaml", "yml") }
                apiFiles.forEach { file ->
                    createDocumentationPage(targetPage.notionPageId, file, selectedCategories)
                }
            }
        }
        if (!config.generateCollection.isNullOrBlank()) {
            val folders = config.pages.map { it.apiFolder }
            GenerateCollection.generate(folders, config.generateCollection)
        }
    }

    private fun createDocumentationPage(
        targetPage: String,
        file: Path,
        selectedCategories: List<String>
    ) {
        val swagger = parseSwaggerFile(file) ?: return

        val pageTitle = swagger.openAPI.info.title ?: return
        val notionTemplate = NotionTemplate(swagger, file.fileName.toString())
        createMainPage(targetPage, pageTitle, notionTemplate)
        createCategorySubpages(targetPage, notionTemplate, selectedCategories)
    }

    private fun parseSwaggerFile(file: Path): SwaggerParseResult? {
        val modified = file.getLastModifiedTime()
        logger.info("Processing ${file.fileName} $modified")

        val options = ParseOptions().apply {
            isResolve = true
            isResolveFully = false
            isValidateExternalRefs = true
        }
        val swagger = OpenAPIParser().readLocation(file.absolutePathString(), null, options)

        if (swagger.messages?.any { it.startsWith("Exception safe-checking yaml content") } == true) {
            error("Invalid YAML file: ${file.fileName}\n${swagger.messages.joinToString("\n")} ")
        }
        if (swagger.openAPI?.info == null) {
            logger.warn("Skipping ${file.fileName} because it does not have an info section")
            return null
        }
        return swagger
    }

    private fun createMainPage(targetPage: String, pageTitle: String, notionTemplate: NotionTemplate) {
        logger.info("Preparing main page '$pageTitle'")
        val pageId = notionAdapter.getOrCreatePage(targetPage, pageTitle)
        notionAdapter.deletePageContents(pageId, pageTitle)

        logger.info("Writing template to main page '$pageTitle'")
        val mainPathLinksMap = notionAdapter.writeTemplateWithLinks(pageId, notionTemplate.render())
        notionTemplate.fillPathTogglesWithContent(notionAdapter, mainPathLinksMap)
        logger.info("Main page content created successfully")
        val tableBlocks = notionTemplate.createTable(pageId, mainPathLinksMap)
        val titleBlockId = notionAdapter.findTitleBlockId(pageId, "Endpoints")

        if (titleBlockId != null) {
            notionAdapter.writeTemplate(titleBlockId, tableBlocks)
            logger.info("Table with links added successfully")
        } else {
            logger.info("Title block not found.")
        }
    }

    private fun createCategorySubpages(
        targetPage: String,
        notionTemplate: NotionTemplate,
        selectedCategories: List<String>
    ) {
        selectedCategories.forEach { category ->
            logger.info("Creating subpage for category '$category'")

            val categoryPageId = notionAdapter.getOrCreatePage(targetPage, "$category API")
            notionAdapter.deletePageContents(categoryPageId, "$category API")

            val categoryTemplate = notionTemplate.renderCategoriezDoc(category)
            val categoryPathLinksMap = notionAdapter.writeTemplateWithLinks(categoryPageId, categoryTemplate)
            notionTemplate.fillPathTogglesWithContent(notionAdapter, categoryPathLinksMap)

            logger.info("Subpage for category '$category' created successfully")
        }
    }
}