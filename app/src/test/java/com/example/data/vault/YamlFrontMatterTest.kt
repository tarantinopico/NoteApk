package com.example.data.vault

import org.junit.Assert.*
import org.junit.Test

class YamlFrontMatterTest {

    @Test
    fun parse_validFrontMatter_returnsParsedMetadataAndContent() {
        val markdown = """
            ---
            title: Test Note
            tags: android, compose
            ---
            # Hello
            This is a test.
        """.trimIndent()

        val parsed = YamlFrontMatter.parse(markdown)

        assertEquals("Test Note", parsed.metadata["title"])
        assertEquals("android, compose", parsed.metadata["tags"])
        assertEquals("# Hello\nThis is a test.", parsed.content.trim())
    }

    @Test
    fun parse_noFrontMatter_returnsEmptyMetadataAndFullContent() {
        val markdown = "# Hello\nThis is a test."

        val parsed = YamlFrontMatter.parse(markdown)

        assertTrue(parsed.metadata.isEmpty())
        assertEquals(markdown, parsed.content)
    }

    @Test
    fun serialize_withMetadata_generatesCorrectMarkdown() {
        val metadata = mapOf("title" to "Test", "id" to "123")
        val content = "# Content"

        val result = YamlFrontMatter.serialize(metadata, content)

        val expected = """
            ---
            title: Test
            id: 123
            ---
            # Content
        """.trimIndent()

        assertEquals(expected, result.trim())
    }
}
