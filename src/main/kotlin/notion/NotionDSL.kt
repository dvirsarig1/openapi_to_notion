package notion

import notion.api.v1.model.blocks.*
import notion.api.v1.model.blocks.HeadingOneBlock.Element
import notion.api.v1.model.common.BlockColor
import notion.api.v1.model.common.Emoji
import notion.api.v1.model.common.RichTextColor
import notion.api.v1.model.pages.PageProperty
import notion.api.v1.model.pages.PageProperty.Date
import notion.api.v1.model.pages.PageProperty.RichText
import notion.api.v1.model.pages.PageProperty.RichText.Annotations
import notion.api.v1.model.pages.PageProperty.RichText.Text
import java.time.ZonedDateTime


internal fun titleProperty(text: String) = PageProperty(title = listOf(RichText(text = Text(text))))
internal fun dateProperty(datetime: ZonedDateTime) = PageProperty(id = "", date = Date(start = datetime.toString()))

internal fun richText(
    text: String,
    bold: Boolean? = null,
    italic: Boolean? = null,
    strikethrough: Boolean? = null,
    underline: Boolean? = null,
    code: Boolean? = null,
    color: RichTextColor? = null,
): RichText {
    return RichText(
        text = Text(text),
        annotations = Annotations(
            bold = bold,
            italic = italic,
            strikethrough = strikethrough,
            underline = underline,
            code = code,
            color = color
        )
    )
}

fun richText(
    text: String,
    link: String? = null,
    color: RichTextColor? = null,
    bold: Boolean? = null,
    italic: Boolean? = null,
    strikethrough: Boolean? = null,
    underline: Boolean? = null,
    code: Boolean? = null
): RichText {
    return RichText(
        text = Text(content = text, link = link?.let { RichText.Link(url = it) }),
        annotations = Annotations(
            bold = bold,
            italic = italic,
            strikethrough = strikethrough,
            underline = underline,
            code = code,
            color = color
        )
    )
}

internal fun blocks(content: BlocksBuilder.() -> Unit) = BlocksBuilder().apply(content).build()

internal class BlocksBuilder {

    private val blocks = mutableListOf<Block>()

    fun heading1(text: String, color: RichTextColor? = RichTextColor.Default, bold: Boolean = false, icon: String = "") {
        val richTextElement = richText("$icon $text", color = color, bold = bold)
        blocks.add(HeadingOneBlock(heading1 = Element(richText = listOf(richTextElement))))
    }

    fun heading2(text: String, color: RichTextColor? = RichTextColor.Default, bold: Boolean = false, icon: String = "") {
        val richTextElement = richText("$icon $text", color = color, bold = bold)
        blocks.add(HeadingTwoBlock(heading2 = HeadingTwoBlock.Element(richText = listOf(richTextElement))))
    }

    fun heading3(text: String, color: RichTextColor? = RichTextColor.Default, bold: Boolean = false, icon: String = "") {
        val richTextElement = richText("$icon $text", color = color, bold = bold)
        blocks.add(HeadingThreeBlock(heading3 = HeadingThreeBlock.Element(richText = listOf(richTextElement))))
    }

    fun paragraph(vararg text: String) {
        blocks.add(ParagraphBlock(paragraph = ParagraphBlock.Element(richText = text.map { richText(it) })))
    }

    fun paragraph(vararg text: RichText, color: BlockColor? = null) {
        blocks.add(ParagraphBlock(
            paragraph = ParagraphBlock.Element(richText = text.toList(), color = color),
        ))
    }

    fun paragraph(vararg text: String, color: BlockColor? = null, content: BlocksBuilder.() -> Unit) {
        paragraph(*text.map { richText(it) }.toTypedArray(), color = color, content = content)
    }

    fun paragraph(vararg text: RichText, color: BlockColor? = null, content: BlocksBuilder.() -> Unit) {
        val children = BlocksBuilder().apply(content).build()
        children.chunked(100).forEach { chunk ->
            blocks.add(ParagraphBlock(
                paragraph = ParagraphBlock.Element(
                    richText = text.toList(),
                    color = color,
                    children = chunk
                )
            ))
        }
    }

    fun divider() {
        blocks.add(DividerBlock(divider = DividerBlock.Element()))
    }

    fun bullet(vararg text: String) {
        blocks.add(BulletedListItemBlock(
            bulletedListItem = BulletedListItemBlock.Element(richText = text.map { richText(it) }))
        )
    }

    fun quote(vararg text: String, color: BlockColor? = null) {
        quote(*text.map { richText(it) }.toTypedArray(), color = color)
    }

    fun quote(vararg text: RichText, color: BlockColor? = null) {
        blocks.add(QuoteBlock(
            quote = QuoteBlock.Element(
                richText = text.toList(),
                color = color
            )
        ))
    }

    fun toggle(
        title: String,
        icon: String = "➡️",
        color: RichTextColor = RichTextColor.Default,
        bold: Boolean = true,
        underline: Boolean = false,
        content: BlocksBuilder.() -> Unit
    ): String? {
        val richTextElement = richText("$icon $title", color = color, bold = bold, underline = underline)
        val toggleBlock = ToggleBlock(
            toggle = ToggleBlock.Element(
                richText = listOf(richTextElement),
                children = BlocksBuilder().apply(content).build()
            )
        )
        blocks.add(toggleBlock)
        return toggleBlock.id
    }

    fun codeBlock(language: String, content: String) {
        blocks.add(CodeBlock(
            code = CodeBlock.Element(
                richText = content.chunked(2000).map { richText(it) },
                language = language
            )
        ))
    }

    fun callout(
        vararg text: String,
        icon: String = "ℹ️",
        color: RichTextColor = RichTextColor.Default,
        backgroundColor: BlockColor = BlockColor.GrayBackground,
        bold: Boolean = false
    ) {
        callout(
            *text.map { richText(it, color = color, bold = bold) }.toTypedArray(),
            icon = icon,
            backgroundColor = backgroundColor
        )
    }

    fun callout(
        vararg text: RichText,
        icon: String,
        backgroundColor: BlockColor = BlockColor.GrayBackground
    ) {
        blocks.add(
            CalloutBlock(
                callout = CalloutBlock.Element(
                    richText = text.toList(),
                    icon = Emoji(emoji = icon),
                    color = backgroundColor
                )
            )
        )
    }

    fun table(
        columns: Int,
        hasColumnHeader: Boolean = false,
        hasRowHeader: Boolean = false,
        builder: RowsBuilder.() -> Unit
    ) {
        blocks.add(
            TableBlock(
                id = null,
                table = TableBlock.Element(
                    tableWidth = columns,
                    hasColumnHeader = hasColumnHeader,
                    hasRowHeader = hasRowHeader,
                    children = RowsBuilder().apply(builder).build()
                )
            )
        )
    }

    class RowsBuilder {
        private val rows = mutableListOf<TableRowBlock>()

        class RowBuilder {
            private val cells = mutableListOf<List<RichText>>()

            fun cell(vararg content: RichText?) {
                cells.add(content.filterNotNull().toList())
            }

            fun cell(vararg content: String) {
                cell(*content.map { richText(it) }.toTypedArray())
            }

            fun build(): List<List<RichText>> = cells
        }

        fun row(cells: RowBuilder.() -> Unit) {
            val row = TableRowBlock(
                id = null,
                tableRow = TableRowBlock.Element(
                    cells = RowBuilder().apply(cells).build()
                )
            )
            rows.add(row)
        }

        fun build(): List<TableRowBlock> = rows
    }

    fun build() = blocks
}

