package notion

import jakarta.inject.Inject
import notion.api.v1.NotionClient
import notion.api.v1.exception.NotionAPIError
import notion.api.v1.model.blocks.Block
import notion.api.v1.model.blocks.BlockType
import notion.api.v1.model.blocks.ChildPageBlock
import notion.api.v1.model.common.Emoji
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageParent
import org.slf4j.LoggerFactory
import java.io.Closeable
import kotlin.math.min

class NotionAdapter @Inject constructor(
    private val client: NotionClient,
): Closeable {

    private val logger = LoggerFactory.getLogger(javaClass.simpleName)

    /** Prepares a page by creating it if it does not exist or deleting its contents if it does */
    fun getOrCreatePage(parentPage: String, pageTitle: String): String {
        val children = withRetry { client.retrieveBlockChildren(parentPage) }
        val pageId = children.results
            .filterIsInstance<ChildPageBlock>()
            .firstOrNull { it.childPage.title == pageTitle }
            ?.id

        if (pageId != null) {
            logger.info("Page '$pageTitle' already exists, deleting and recreating it.")
            withRetry { client.deleteBlock(pageId) }
        }
        return createPage(parentPage, pageTitle).id
    }

    /** Writes a template to a page */
    fun writeTemplate(blockId: String, blocks: List<Block>): List<Block> {
        return blocks.chunked(100).flatMap { chunk ->
            val saved = withRetry {
                client.appendBlockChildren(blockId, chunk)
            }
            saved.results
        }
    }

    /** Writes a template to a page and builds a map of heading links */
    fun writeTemplateWithLinks(pageId: String, blocks: List<Block>): Map<String, String> {
        val pathLinksMap = mutableMapOf<String, String>()
        blocks.chunked(100).forEach { chunk ->
            val saved = withRetry {
                client.appendBlockChildren(pageId, chunk)
            }

            saved.results.forEach { block ->
                if (block.type == BlockType.Toggle) {
                    val titleText = block.asToggle().toggle.richText.firstOrNull()?.plainText ?: ""
                    val cleanedTitle = titleText.replace(Regex("^[^\\w\\s]+\\s*"), "")
                    val headingLink = block.id!!.replace("-", "")
                    pathLinksMap[cleanedTitle] = headingLink
                }
            }
        }
        return pathLinksMap
    }

    private fun createPage(targetPage: String, title: String): Page {
        logger.info("Creating Page '$title'")
        return withRetry {
            client.createPage(
                parent = PageParent(pageId = targetPage),
                properties = mapOf("title" to titleProperty(title)),
                icon = Emoji(emoji = "\uD83D\uDCBD")
            )
        }
    }

    fun deletePageContents(pageId: String, pageTitle: String) {
        do {
            val children = withRetry { client.retrieveBlockChildren(pageId) }
            children.results.forEach { block ->
                logger.info("Deleting block ${block.id} from page $pageTitle")
                withRetry { client.deleteBlock(block.id!!) }
            }
        } while (children.hasMore)
    }

    override fun close() {
        client.close()
    }

    private fun <T> withRetry(maxTries: Int = 20, block: () -> T): T {
        var tries = 0
        var backoff = 1L
        while (tries++ < maxTries) {
            try {
                return block()
            } catch (e: NotionAPIError) {
                val status = e.httpResponse.status
                if (status == 429 || status >= 500) {
                    backoff = min(2 * backoff, 10)
                    val seconds = backoff
                    logger.warn("Received status=$status, waiting for $seconds seconds")
                    Thread.sleep(1000 * seconds)
                } else {
                    logger.error("Request failed", e)
                    throw e
                }
            } catch (e: Exception) {
                logger.error("Request failed", e)
                throw e
            }
        }
        throw RuntimeException("Too many retries")
    }

    fun findTitleBlockId(pageId: String, titleText: String): String? {
        val blocksResponse = client.retrieveBlockChildren(pageId)
        val blocks = blocksResponse.results

        val titleBlock = blocks.find { block ->
            block.type == BlockType.Callout &&
                    block.asCallout().callout?.richText?.any { it.plainText == titleText } == true
        }
        return titleBlock?.id
    }
}