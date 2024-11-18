package app

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import config.Config
import notion.NotionAdapter
import jakarta.inject.Inject
import template.PageProcessor
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

class App @Inject constructor(
    private val notionAdapter: NotionAdapter,
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

//    private fun createMainPage(targetPage: String, pageTitle: String, notionTemplate: NotionTemplate) {
//        createPage(
//            targetPage = targetPage,
//            pageTitle = pageTitle,
//            notionTemplate = notionTemplate,
//            fieldCategory = null,
//            endpointCategory = null
//        )
//    }
//
//    private fun createCategorySubpages(
//        targetPage: String,
//        notionTemplate: NotionTemplate,
//        selectedCategories: List<String>
//    ) {
//        selectedCategories.forEach { category ->
//            createPage(
//                targetPage = targetPage,
//                pageTitle = "$category API",
//                notionTemplate = notionTemplate,
//                fieldCategory = null,
//                endpointCategory = category
//            )
//            logger.info("Creating subpage for category '$category'")
//
//            val categoryPageId = notionAdapter.getOrCreatePage(targetPage, "$category API")
//            notionAdapter.deletePageContents(categoryPageId, "$category API")
//
//            val categoryTemplate = notionTemplate.renderCategoriesDoc(category)
//            val categoryPathLinksMap = notionAdapter.writeTemplateWithLinks(categoryPageId, categoryTemplate)
//            notionTemplate.fillPathTogglesWithContent(notionAdapter, categoryPathLinksMap)
//
//            logger.info("Subpage for category '$category' created successfully")
//        }
//    }
//
//    private fun createFilteredFieldPage(targetPage: String, notionTemplate: NotionTemplate, fieldCategory: String?) {
//        createPage(
//            targetPage = targetPage,
//            pageTitle = "$fieldCategory Information",
//            notionTemplate = notionTemplate,
//            fieldCategory = fieldCategory,
//            endpointCategory = null
//        )
//    }
//
//    private fun createPage(
//        targetPage: String,
//        pageTitle: String,
//        notionTemplate: NotionTemplate,
//        fieldCategory: String?,
//        endpointCategory: String?
//    ) {
//        logger.info("Preparing page '$pageTitle'")
//        val pageId = notionAdapter.getOrCreatePage(targetPage, pageTitle)
//        notionAdapter.deletePageContents(pageId, pageTitle)
//
//        logger.info("Writing template to page '$pageTitle'")
//        val mainPathLinksMap = if (fieldCategory != null) {
//            notionAdapter.writeTemplateWithLinks(pageId, notionTemplate.renderFilteredFieldsDoc(fieldCategory))
//        } else if (endpointCategory != null) {
//            notionAdapter.writeTemplateWithLinks(pageId, notionTemplate.renderCategoriesDoc(endpointCategory))
//        } else {
//            notionAdapter.writeTemplateWithLinks(pageId, notionTemplate.render())
//        }
//
//        notionTemplate.fillPathTogglesWithContent(notionAdapter, mainPathLinksMap)
//        logger.info("Page content created successfully")
//
//        val tableBlocks = notionTemplate.createTable(pageId, mainPathLinksMap)
//        val titleBlockId = notionAdapter.findTitleBlockId(pageId, "Endpoints")
//
//        if (titleBlockId != null) {
//            notionAdapter.writeTemplate(titleBlockId, tableBlocks)
//            logger.info("Table with links added successfully")
//        } else {
//            logger.info("Title block not found.")
//        }
//    }
//
//
//}