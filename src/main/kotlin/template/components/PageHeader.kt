package template.components

import notion.BlocksBuilder
import notion.richText
import notion.api.v1.model.common.RichTextColor.Default
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal fun BlocksBuilder.pageHeader(fileName: String, visible: Boolean) {
    if (!visible) return
    val currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    callout(
        richText("Last update:\n"),
        richText("Date: $currentDate\n", color = Default),
        richText("Time: $currentTime\n", color = Default),
        richText("Generated from: "),
        richText(fileName, code = true, color = Default),
        icon = "\u2728"
    )
}

