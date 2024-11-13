package template

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem

object PathCategorizer {

    val categoryOrder = listOf(
        "GENERIC",
        "ONBOARDING",
        "ENGAGEMENT",
        "CASHOUT",
        "PAYMENTS",
        "TRACKING",
        "VERIFICATION",
        "FEEDBACK",
        "GAMES",
        "TO_DELETE"
    )

    /**
     * Categorizes API paths based on "Platform" and "Category" tags.
     *
     * @param paths A map of OpenAPI paths where the key is a string representing the path,
     * and the value is a PathItem containing operations (HTTP methods) for that path.
     *
     * @return A nested map categorizing paths by platform and category.
     *  - The outer map's key is the platform tag.
     *  - The value of the outer map is a map where the key is the category tag.
     *  - Each value in the inner map is a list of triples representing:
     *      - [path] - the API path as a string
     *      - [method] - the HTTP method name as a string
     *      - [operation] - the Operation object containing details of the API operation.
     */
    fun categorizePathsByTags(paths: Map<String, PathItem>): Map<String, Map<String, List<Triple<String, String, Operation>>>> {
        val categorizedPaths = mutableMapOf<String, MutableMap<String, MutableList<Triple<String, String, Operation>>>>()

        for ((path, pathItem) in paths) {
            for ((method, operation) in pathItem.readOperationsMap()) {
                val tags = operation.tags ?: listOf("Both", "GENERIC")
                val platformTag = tags.getOrNull(0) ?: "Both"
                val categoryTag = tags.getOrNull(1) ?: "GENERIC"

                val platformPaths = categorizedPaths.getOrPut(platformTag) { mutableMapOf() }
                platformPaths.getOrPut(categoryTag) { mutableListOf() }
                    .add(Triple(path, method.name, operation))
            }
        }

        return categorizedPaths
    }
}

