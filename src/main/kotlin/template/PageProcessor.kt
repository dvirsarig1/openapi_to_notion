package template

import com.google.inject.Inject
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.parser.core.models.ParseOptions
import io.swagger.v3.parser.core.models.SwaggerParseResult
import notion.NotionAdapter
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.getLastModifiedTime

class PageProcessor @Inject constructor(
    private val notionAdapter: NotionAdapter,
) {

    private val logger = LoggerFactory.getLogger(javaClass.simpleName)

    fun process(
        targetPage: String,
        file: Path,
        selectedCategories: List<String>,
        fieldCategory: String?
    ) {
        val swagger = parseSwaggerFile(file) ?: return
        val notionTemplate = NotionTemplate(swagger, file.fileName.toString())

        //createMainPage(targetPage, swagger.openAPI.info.title ?: return, notionTemplate)
        //createCategorySubpages(targetPage, notionTemplate, selectedCategories)
        createFilteredFieldPage(targetPage, notionTemplate, fieldCategory)
        notionAdapter.close()
    }

    private fun createMainPage(targetPage: String, pageTitle: String, notionTemplate: NotionTemplate) {
        createPage(targetPage, pageTitle, notionTemplate)
    }

    private fun createCategorySubpages(targetPage: String, notionTemplate: NotionTemplate, selectedCategories: List<String>) {
        selectedCategories.forEach { category ->
            createPage(targetPage, "$category API", notionTemplate, endpointCategory = category)
        }
    }

    private fun createFilteredFieldPage(targetPage: String, notionTemplate: NotionTemplate, fieldCategory: String?) {
        fieldCategory?.let {
            createPage(targetPage, "$fieldCategory Information", notionTemplate, fieldCategory = it)
        }
    }

    private fun createPage(
        targetPage: String,
        pageTitle: String,
        notionTemplate: NotionTemplate,
        fieldCategory: String? = null,
        endpointCategory: String? = null
    ) {
        logger.info("Creating page: $pageTitle")
        val pageId = notionAdapter.getOrCreatePage(targetPage, pageTitle)
        notionAdapter.deletePageContents(pageId, pageTitle)

        val contentBlocks = when {
            fieldCategory != null -> notionTemplate.renderFilteredFieldsDoc(fieldCategory)
            endpointCategory != null -> notionTemplate.renderCategoriesDoc(endpointCategory)
            else -> notionTemplate.render()
        }

        val pathLinksMap = notionAdapter.writeTemplateWithLinks(pageId, contentBlocks)
        notionTemplate.fillPathTogglesWithContent(notionAdapter, pathLinksMap, fieldCategory)
        val tableBlocks = notionTemplate.createTable(pageId, pathLinksMap, fieldCategory)
        val titleBlockId = notionAdapter.findTitleBlockId(pageId, "Endpoints")
        if (titleBlockId != null) { notionAdapter.writeTemplate(titleBlockId, tableBlocks) }
        logger.info("Page created: $pageTitle")
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
}
