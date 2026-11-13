package com.filemate.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// Warna pewarnaan kode di editor teks
private val CommentColor = Color(0xFF8A8A8A)
private val StringColor = Color(0xFF2E7D32)
private val NumberColor = Color(0xFFB26A00)
private val KeywordColor = Color(0xFF1565C0)
private val TagColor = Color(0xFF7B1FA2)

/** Teks yang lebih panjang dari ini tidak diwarnai, supaya mengetik tetap lancar. */
const val HIGHLIGHT_LIMIT = 100_000

/**
 * Aturan pewarnaan satu keluarga bahasa. [pattern] berisi beberapa kelompok dalam kurung;
 * kelompok ke-n yang cocok diberi warna ke-n dari [colors].
 */
private class Syntax(val pattern: Regex, val colors: List<Color>)

private const val STRINGS = "\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'"
private const val NUMBER = "\\b(?:0[xX][0-9a-fA-F]+|\\d+(?:\\.\\d+)?)\\b"

private val CODE_WORDS = listOf(
    "abstract", "as", "break", "case", "catch", "class", "const", "continue", "def", "default", "do", "else",
    "enum", "export", "extends", "false", "final", "finally", "for", "from", "fun", "function", "if",
    "implements", "import", "in", "interface", "is", "let", "new", "null", "object", "override", "package",
    "private", "protected", "public", "return", "static", "super", "switch", "this", "throw", "true", "try",
    "val", "var", "void", "when", "while", "int", "long", "float", "double", "boolean", "char", "byte",
    "short", "String", "None", "True", "False", "and", "or", "not", "elif", "lambda", "pass", "with", "yield",
).joinToString("|")

private val CODE_SYNTAX = Syntax(
    Regex("(//[^\\n]*|/\\*[\\s\\S]*?\\*/)|($STRINGS)|($NUMBER)|(\\b(?:$CODE_WORDS)\\b)"),
    listOf(CommentColor, StringColor, NumberColor, KeywordColor),
)

private val HASH_SYNTAX = Syntax(
    Regex("(#[^\\n]*)|($STRINGS)|($NUMBER)|(\\b(?:if|then|else|elif|fi|for|while|do|done|case|esac|function|return|exit|echo|export|local|true|false|null|yes|no|on|off)\\b)"),
    listOf(CommentColor, StringColor, NumberColor, KeywordColor),
)

private val XML_SYNTAX = Syntax(
    Regex("(<!--[\\s\\S]*?-->)|($STRINGS)|(</?[A-Za-z_][\\w:.-]*|/?>)"),
    listOf(CommentColor, StringColor, TagColor),
)

private val JSON_SYNTAX = Syntax(
    Regex("(\"(?:\\\\.|[^\"\\\\\\n])*\"(?=\\s*:))|(\"(?:\\\\.|[^\"\\\\\\n])*\")|($NUMBER)|(\\b(?:true|false|null)\\b)"),
    listOf(TagColor, StringColor, NumberColor, KeywordColor),
)

private val SMALI_SYNTAX_RULES = Syntax(
    Regex("(#[^\\n]*)|($STRINGS)|(\\b[vp]\\d+\\b|$NUMBER)|((?m)^\\s*\\.[a-z][a-z-]*|\\b(?:invoke|move|return|const|if|goto|new|check|iget|iput|sget|sput|aget|aput|throw|add|sub|mul|div|rem|and|or|xor|shl|shr|ushr|neg|not|cmp|monitor|nop|array|fill|packed|sparse|instance)[\\w/-]*)|(L[\\w/$]+;)"),
    listOf(CommentColor, StringColor, NumberColor, KeywordColor, TagColor),
)

/** Memilih aturan pewarnaan dari akhiran nama berkas. Null berarti tidak diwarnai. */
private fun syntaxFor(extension: String): Syntax? = when (extension.uppercase()) {
    "XML", "HTML", "HTM", "SVG" -> XML_SYNTAX
    "JSON" -> JSON_SYNTAX
    "SMALI" -> SMALI_SYNTAX_RULES
    "SH", "PROP", "CONF", "CFG", "INI", "RC", "PROPERTIES", "YML", "YAML", "PY" -> HASH_SYNTAX
    "JAVA", "KT", "KTS", "JS", "CSS", "C", "H", "CPP", "GRADLE" -> CODE_SYNTAX
    else -> null
}

/**
 * Pewarnaan kode untuk editor teks: komentar, teks dalam kutip, angka, kata kunci, dan tag.
 * Hanya tampilannya yang berubah, isi berkas tidak disentuh.
 */
class SyntaxTransformation(extension: String) : VisualTransformation {
    private val syntax = syntaxFor(extension)

    /** True jika berkas jenis ini punya aturan pewarnaan. */
    val active: Boolean get() = syntax != null

    override fun filter(text: AnnotatedString): TransformedText {
        val rules = syntax
        if (rules == null || text.text.length > HIGHLIGHT_LIMIT) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val colored = buildAnnotatedString {
            append(text.text)
            for (match in rules.pattern.findAll(text.text)) {
                // Kelompok pertama yang cocok menentukan warnanya
                for (index in rules.colors.indices) {
                    val group = match.groups[index + 1] ?: continue
                    addStyle(SpanStyle(color = rules.colors[index]), group.range.first, group.range.last + 1)
                    break
                }
            }
        }
        return TransformedText(colored, OffsetMapping.Identity)
    }
}
