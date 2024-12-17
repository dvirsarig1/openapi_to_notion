package template

import notion.BlocksBuilder
import notion.BlocksBuilder.RowsBuilder
import notion.NotionAdapter
import notion.blocks
import notion.richText
import template.components.exampleItem
import template.components.pageHeader
import template.components.serverUrl
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.parser.core.models.SwaggerParseResult
import notion.api.v1.model.blocks.Block
import notion.api.v1.model.common.BlockColor
import notion.api.v1.model.common.RichTextColor.*

class NotionTemplate(
    private val swagger: SwaggerParseResult,
    private val fileName: String,
)  {

    private val consumedComponents = mutableListOf<Schema<*>>()
    private val categorizedPaths = PathCategorizer.categorizePathsByTags(swagger.openAPI.paths)
    private val toggleIds = mutableMapOf<String, String>()

    fun render(fieldCategory: String? = null): List<Block> = blocks {
        pageHeader(fileName, true)
        summarySectionForMainDoc()
        renderEndpoints(selectedCategory = null, fieldCategory = fieldCategory)
        val components = getComponents(fieldCategory = fieldCategory)
        if (components.isNotEmpty()) {
            componentsSection(components, fieldCategory)
        }
        authenticationSection()
    }

    fun renderCategoriesDoc(selectedCategory: String): List<Block> = blocks {
        pageHeader("$selectedCategory API", true)
        summarySectionForSingleCategory(selectedCategory)
        renderEndpoints(selectedCategory = selectedCategory, fieldCategory = null)
        authenticationSection()
    }

    private fun BlocksBuilder.summarySectionForSingleCategory(selectedCategory: String) {
        heading1("🗺️ Customer’s Data Journey Map For $selectedCategory", color = Default, bold = true)
        serverUrl(swagger.openAPI.servers)
    }

    private fun BlocksBuilder.summarySectionForMainDoc() {
        heading1("🗺️ Customer’s Data Journey Map", color = Default, bold = true)
        serverUrl(swagger.openAPI.servers)
        callout(
            "Endpoints",
            icon = "🔗",
            color = Blue,
            backgroundColor = BlockColor.YellowBackground,
            bold = true
        )
    }

    private fun BlocksBuilder.renderEndpoints(selectedCategory: String?, fieldCategory: String?) {
        for (platform in categorizedPaths.keys) {
            heading1("🚀 $platform Endpoints", color = Blue, bold = true)
            for (category in PathCategorizer.categoryOrder) {
                if (selectedCategory != null && selectedCategory != category) continue
                val paths = categorizedPaths[platform]?.get(category)?.map { it.first } ?: continue
                val filteredPaths = if (fieldCategory == null) paths else paths.filter { path ->
                    doesPathContainFieldWithCategory(path, fieldCategory)
                }
                if (filteredPaths.isEmpty()) continue
                heading2("📂 $category", color = Orange, bold = true)
                createPathToggles(filteredPaths, fieldCategory)
            }
        }
    }

    private fun getComponents(fieldCategory: String?): Map<String, Schema<*>> {
        return swagger.openAPI.components?.schemas
            ?.filter { (_, schema) ->
                !consumedComponents.contains(schema) && doesSchemaContainCategory(schema, fieldCategory)
            } ?: emptyMap()
    }

    private fun BlocksBuilder.componentsSection(schemas: Map<String, Schema<*>>, fieldCategory: String? = null) {
        heading1("📄 Schemas", color = Blue, bold = true)
        for ((name, schema) in schemas) {
            heading3("📑 $name", color = Gray, bold = true)
            schema.description?.let { desc ->
                paragraph(desc)
            }
            schemaTable(schema, fieldCategory)
        }
    }

    private fun BlocksBuilder.authenticationSection() {
        val securitySchemeMap = swagger.openAPI.components?.securitySchemes ?: emptyMap()

        if (securitySchemeMap.isEmpty()) return

        heading1("🔒 Authentication", color = Blue, bold = true)

        for ((name, security) in securitySchemeMap) {
            heading2("🔐 $name", color = Green, bold = true)

            security.description?.let { desc ->
                paragraph(desc)
            }

            table(2, hasRowHeader = true) {
                row {
                    cell(richText("Type"))
                    cell(richText(security.type.toString()))
                }
                security.`in`?.let {
                    row {
                        cell(richText("In"))
                        cell(richText(it.toString()))
                    }
                    row {
                        cell(richText("Name"))
                        cell(richText(security.name ?: ""))
                    }
                }
                security?.openIdConnectUrl?.let { url ->
                    row {
                        cell(richText("Connect URL"))
                        cell(richText(url))
                    }
                }
                security.flows?.let { flows ->
                    // TODO oauth2 flows
                }
                security.scheme?.let { scheme ->
                    row {
                        cell(richText("Scheme"))
                        cell(richText(scheme))
                    }
                }
            }
        }
    }


    private fun BlocksBuilder.operationRequestSection(operation: Operation, fieldCategory: String?) {
        operation.requestBody?.let { request ->
            heading3("📬 Request", color = Blue, bold = true)
            request.description?.let { desc ->
                paragraph(desc)
            }
            request.content?.let { contents ->
                for ((contentType, content) in contents) {
                    val actualContentType = if (contentType == "*/*") "application/json" else contentType.ifBlank { "application/json" }
                    paragraph(richText("Content-Type: "), richText(actualContentType, code = true, color = Default))

                    content.schema.externalDocs?.let {
                        paragraph(richText("Documentation: ", bold = true), richText(it.description ?: it.url, link = it.url))
                    }

                    schemaTable(content.schema, fieldCategory)

                    exampleItem(content)
                }
            }
        }
    }

    private fun BlocksBuilder.schemaTable(schema: Schema<*>, fieldCategory: String? = null) {
        schema.externalDocs?.let {
            paragraph(richText("Documentation: ", bold = true), richText(it.description ?: it.url, link = it.url))
        }

        if (schema.properties.isNullOrEmpty()
            && schema.additionalProperties == null
            && schema.items == null
            && schema.`$ref` == null
        ) {
            return
        }
        table(3, hasColumnHeader = true) {
            row {
                cell(richText("Name"))
                cell(richText("Type"))
                cell(richText("Description"))
            }
            propertiesRow("", schema, fieldCategory)
        }
    }

    private fun BlocksBuilder.operationResponseSection(operation: Operation, fieldCategory: String?) {
        operation.responses?.let { responses ->
            heading3("📥 Response", color = Green, bold = true)
            for ((code, response) in responses) {
                operationResponseBody(response, code, fieldCategory)
                divider()
            }
        }
    }

    private fun BlocksBuilder.operationResponseBody(response: ApiResponse, code: String, fieldCategory: String?) {
        response.content?.takeIf { it.isNotEmpty() }?.let { contents ->
            for ((contentType, content) in contents) {
                val actualContentType = if (contentType == "*/*") "application/json" else contentType.ifBlank { "application/json" }
                responseBodyHeader(code, response, actualContentType)
                schemaTable(content.schema, fieldCategory)
                exampleItem(content)
            }
        } ?: run {
            responseBodyHeader(code, response, "application/json")
        }
    }


    private fun BlocksBuilder.responseBodyHeader(code: String, response: ApiResponse, contentType: String?) {
        quote(
            richText(
                "$code ${response.description ?: ""}",
                code = true,
                bold = true,
                color = if (code.startsWith("2")) Green else Orange
            ),
            richText(contentType?.let { "  Content-Type: " } ?: "  No Content", color = Default),
            
            richText(contentType ?: "", code = contentType != null, color = Default),
            color = if (code.startsWith("2")) BlockColor.Green else BlockColor.Orange
        )
    }

    private fun BlocksBuilder.operationParams(operation: Operation, fieldCategory: String?) {
        if (!operation.parameters.isNullOrEmpty()) {
            heading3("📋 Parameters", color = Brown, bold = true)
            table(4, hasRowHeader = true, hasColumnHeader = true) {
                row {
                    cell(richText("Name"))
                    cell(richText("Type"))
                    cell(richText("Location"))
                    cell(richText("Description"))
                }
                for (parameter in operation.parameters) {
                    val isValidCategory = fieldCategory == null || parameter.schema?.extensions
                        ?.get("x-category")
                        ?.let { it as? List<*> }
                        ?.contains(fieldCategory) == true

                    if (isValidCategory) {
                        val required = parameter.required == true
                        row {
                            cell(richText(parameter.name + if (required) "" else "?", code = true, color = Default))
                            cell(richText(parameter.schema?.type ?: ""))
                            cell(richText(parameter.`in`))
                            cell(richText(parameter.description ?: ""))
                        }
                    }
                }
            }
        }
    }

    private fun BlocksBuilder.operationAuth(operation: Operation) {
        val security = operation.security ?: swagger.openAPI.security
        security?.takeIf { it.isNotEmpty() }?.let { _ ->
            heading3("🔑 Authentication", color = Purple, bold = true)
            security.flatMap { it.keys }.forEach {
                bullet(it)
            }
        }
    }

    private fun RowsBuilder.propertiesRow(path: String, schema: Schema<*>, fieldCategory: String?) {
        schema.`$ref`?.let { ref ->
            val resolvedSchema = swagger.resolveSchema(ref)
            consumedComponents.add(resolvedSchema)
            propertiesRow(path, resolvedSchema, fieldCategory)
            return
        }

        when (schema) {
            is ObjectSchema -> {
                schema.properties?.forEach { (property, value) ->
                    propertiesRowItem(path, property, value, schema, fieldCategory)
                }
            }

            is MapSchema, is JsonSchema -> {
                schema.properties?.forEach { (property, value) ->
                    propertiesRowItem(path, property, value, schema, fieldCategory)
                }
                schema.additionalProperties?.let { additionalProperties ->
                    if (additionalProperties !is Schema<*>) return@let
                    propertiesRowItem(path, "<*>", additionalProperties, fieldCategory = fieldCategory)
                }
            }

            else -> {
                propertiesRowItem(path, "", schema, fieldCategory = fieldCategory)
            }
        }
    }

    private fun SwaggerParseResult.resolveSchema(ref: String): Schema<*> {
        val components = openAPI.components
        val schema = components.schemas[ref]
        if (schema != null) return schema
        val refSchema = components.schemas[ref.substringAfterLast("/")]
        if (refSchema != null) return refSchema
        error("Could not resolve schema $ref")
    }

    private fun RowsBuilder.propertiesRowItem(
        path: String,
        property: String,
        value: Schema<*>,
        parentSchema: Schema<*>? = null,
        fieldCategory: String?
    ) {
        if (fieldCategory != null && value.extensions?.get("x-category")?.let { it as? List<*> }?.contains(fieldCategory) != true) {
            return
        }
        val rowPath = "$path.$property".removePrefix(".").removeSuffix(".")
        val required = parentSchema?.required?.contains(property) == true
        val description = value.description ?: ""
        val defaultStr = value.default?.toString()?.takeIf { it.isNotBlank() }?.let { " (default: $it)" } ?: ""
        val component = value.`$ref`?.substringAfterLast("/")
            ?: value.items?.`$ref`?.substringAfterLast("/")?.let { "array<$it>" }
        val type = component ?: value.type ?: value.types?.firstOrNull() ?: ""
        val requiredStr = if (required) "" else "?"

        row {
            cell(
                richText(rowPath.dropLastWhile { it != '.' }, code = true, color = Default),
                richText(rowPath.takeLastWhile { it != '.' }, code = true, color = Default, bold = true)
            )
            cell(richText(type + requiredStr, code = true, color = Pink))
            cell(
                richText("$description$defaultStr "),
                value.externalDocs?.let { richText(it.description ?: "Docs", link = it.url) }
            )
        }

        if (component != null) {
            return
        }
        if (value is ObjectSchema || value is MapSchema || value is JsonSchema) {
            propertiesRow(rowPath, value, fieldCategory)
        }
        if (value is ArraySchema || value.items != null) {
            propertiesRow("$rowPath[]", value.items, fieldCategory)
        }
    }

    private fun BlocksBuilder.createPathToggles(paths: List<String>, fieldCategory: String? = null) {
        for (path in paths) {
            if (fieldCategory != null && !doesPathContainFieldWithCategory(path, fieldCategory)) continue
            val toggleId = toggle(
                title = path,
                color = Blue,
                bold = true,
                underline = true
            ) {}

            if (toggleId != null) {
                toggleIds[path] = toggleId.replace("-", "")
            }
        }
    }

    private fun generateOperationContent(path: String, method: String, operation: Operation, fieldCategory: String?): List<Block> = blocks {
        paragraph(
            richText(" $method ", code = true, bold = true, color = Green),
            richText(" $path", code = true, color = Default)
        )
        paragraph(operation.description ?: "")
        operation.externalDocs?.takeIf { it.url.isNotBlank() }?.let { docs ->
            paragraph(richText("Documentation: ", bold = true), richText(docs.description ?: docs.url, link = docs.url))
        }
        operationAuth(operation)
        operationParams(operation, fieldCategory)
        operationRequestSection(operation, fieldCategory)
        operationResponseSection(operation, fieldCategory)
    }

    fun fillPathTogglesWithContent(
        client: NotionAdapter,
        toggleIds: Map<String, String>,
        fieldCategory: String? = null
    ) {
        for ((_, categories) in categorizedPaths) {
            for ((_, paths) in categories) {
                for ((path, method, operation) in paths) {
                    val toggleId = toggleIds[path]
                    val contentBlocks = generateOperationContent(path, method, operation, fieldCategory)
                    if (toggleId != null) {
                        client.writeTemplate(toggleId, contentBlocks)
                    }
                }
            }
        }
    }

    fun createTable(pageId: String, pathLinksMap: Map<String, String>, fieldCategory: String?): List<Block> = blocks {
        table(4, hasColumnHeader = true) {
            row {
                cell(richText("Method"))
                cell(richText("Endpoint"))
                cell(richText("Authentication"))
                cell(richText("Description"))
            }

            for (path in swagger.openAPI.paths) {
                for ((method, operation) in path.value.readOperationsMap()) {
                    if (!doesPathContainFieldWithCategory(path.key, fieldCategory)) continue
                    val security = operation.security ?: swagger.openAPI.security
                    val authText = if (security.isNullOrEmpty()) {
                        swagger.openAPI.components?.securitySchemes?.get("BasicAuth")?.let { "BasicAuth" } ?: ""
                    } else {
                        security.flatMap { it.keys }.joinToString(", ")
                    }

                    row {
                        cell(richText(method.name, code = true, color = Green))
                        val blockId = pathLinksMap[path.key]
                        if (blockId.isNullOrEmpty()) {
                            cell(richText(path.key, code = true, color = Default))
                        } else {
                            cell(richText(path.key, link = "https://www.notion.so/JustPlay-API-${pageId.replace("-", "")}?pvs=97#$blockId"))
                        }
                        cell(richText(authText))
                        cell(richText(operation.summary ?: operation.description ?: ""))
                    }
                }
            }
        }
    }

    private fun doesPathContainFieldWithCategory(path: String, fieldCategory: String?): Boolean {
        if (fieldCategory == null) return true
        val operation = swagger.openAPI.paths[path]?.readOperations()?.firstOrNull() ?: return false
        return operation.requestBody?.content?.values?.any { content ->
            content.schema?.properties?.values?.any { property ->
                property.extensions?.get("x-category")?.let { it as? List<*> }?.contains(fieldCategory) == true
            } == true
        } == true
    }

    private fun doesSchemaContainCategory(schema: Schema<*>, fieldCategory: String?): Boolean {
        if (fieldCategory == null) return true
        return schema.properties?.values?.any { property ->
            property.extensions?.get("x-category")?.let { it as? List<*> }?.contains(fieldCategory) == true
        } == true
    }
}