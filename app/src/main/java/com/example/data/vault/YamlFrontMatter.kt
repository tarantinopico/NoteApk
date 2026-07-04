package com.example.data.vault

object YamlFrontMatter {
    
    data class ParsedNote(
        val metadata: Map<String, String>,
        val content: String
    )

    fun parse(fileContent: String): ParsedNote {
        val lines = fileContent.lines()
        if (lines.isEmpty() || lines[0].trim() != "---") {
            return ParsedNote(emptyMap(), fileContent)
        }

        val metadata = mutableMapOf<String, String>()
        var contentStartIndex = -1

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.trim() == "---") {
                contentStartIndex = i + 1
                break
            }
            
            val colonIndex = line.indexOf(':')
            if (colonIndex != -1) {
                val key = line.substring(0, colonIndex).trim()
                val value = line.substring(colonIndex + 1).trim()
                metadata[key] = value
            }
        }

        val content = if (contentStartIndex != -1 && contentStartIndex < lines.size) {
            lines.subList(contentStartIndex, lines.size).joinToString("\n")
        } else {
            fileContent // Fallback if no closing ---
        }

        return ParsedNote(metadata, content)
    }

    fun serialize(metadata: Map<String, String>, content: String): String {
        val builder = StringBuilder()
        if (metadata.isNotEmpty()) {
            builder.append("---\n")
            metadata.forEach { (key, value) ->
                builder.append("$key: $value\n")
            }
            builder.append("---\n")
        }
        builder.append(content)
        return builder.toString()
    }
}
