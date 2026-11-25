package com.hefimer.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.hefimer.app.ui.theme.HefimerColors

data class LanguageInfo(val id: String, val label: String, val ext: String) {
    fun localizedLabel(): String =
        if (id == "text") com.hefimer.app.ui.theme.tr("Plain Text", "Văn bản thuần") else label
}

data class SmartAction(
    val type: String,
    val label: String,
    val icon: ImageVector,
    val url: String,
)

/**
 * Live syntax-highlighting VisualTransformation for Compose BasicTextField.
 * Preserves 1:1 character offsets so cursor, selection, and IME composition stay exact.
 */
class CodeSyntaxVisualTransformation(
    private val langId: String,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (langId == "text" || text.text.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        return TransformedText(
            text = CodeHighlighter.highlight(text.text, langId),
            offsetMapping = OffsetMapping.Identity,
        )
    }

    override fun equals(other: Any?): Boolean =
        other is CodeSyntaxVisualTransformation && other.langId == langId

    override fun hashCode(): Int = langId.hashCode()
}

object CodeHighlighter {
    private const val MAX_HIGHLIGHT_CHARS = 120_000

    val SUPPORTED_LANGUAGES = listOf(
        LanguageInfo("text", "Plain Text", "txt"),
        LanguageInfo("sql", "SQL", "sql"),
        LanguageInfo("javascript", "JavaScript", "js"),
        LanguageInfo("typescript", "TypeScript", "ts"),
        LanguageInfo("python", "Python", "py"),
        LanguageInfo("java", "Java", "java"),
        LanguageInfo("kotlin", "Kotlin", "kt"),
        LanguageInfo("swift", "Swift", "swift"),
        LanguageInfo("cpp", "C++", "cpp"),
        LanguageInfo("csharp", "C#", "cs"),
        LanguageInfo("go", "Go", "go"),
        LanguageInfo("rust", "Rust", "rs"),
        LanguageInfo("php", "PHP", "php"),
        LanguageInfo("ruby", "Ruby", "rb"),
        LanguageInfo("bash", "Bash", "sh"),
        LanguageInfo("markup", "HTML", "html"),
        LanguageInfo("css", "CSS", "css"),
        LanguageInfo("json", "JSON", "json"),
        LanguageInfo("markdown", "Markdown", "md"),
        LanguageInfo("yaml", "YAML", "yaml"),
        LanguageInfo("docker", "Dockerfile", "dockerfile"),
        LanguageInfo("makefile", "Makefile", "makefile"),
        LanguageInfo("lua", "Lua", "lua"),
        LanguageInfo("graphql", "GraphQL", "graphql"),
        LanguageInfo("scss", "SCSS", "scss"),
    )

    fun detectLanguage(text: String): LanguageInfo {
        val t = text.trim()
        if (t.isEmpty()) return SUPPORTED_LANGUAGES[0]
        val sample = if (t.length > 8_000) t.substring(0, 8_000) else t
        val firstNonComment = sample.lineSequence()
            .map { it.trim() }
            .firstOrNull {
                it.isNotEmpty() && !it.startsWith("--") && !it.startsWith("//") &&
                    !it.startsWith("/*") && !it.startsWith("#")
            }.orEmpty()

        return when {
            Regex("""^<!DOCTYPE html|<html|<body|<div|<span|<p|<a """, RegexOption.IGNORE_CASE).containsMatchIn(sample) ->
                LanguageInfo("markup", "HTML", "html")
            Regex("""<svg""", RegexOption.IGNORE_CASE).containsMatchIn(sample) ->
                LanguageInfo("markup", "SVG", "svg")
            Regex("""^<\?xml""", RegexOption.IGNORE_CASE).containsMatchIn(sample) ->
                LanguageInfo("markup", "XML", "xml")
            ((t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"))) &&
                Regex("""^[\{\[]\s*("|\d|true|false|null)""").containsMatchIn(sample) ->
                LanguageInfo("json", "JSON", "json")
            Regex("""^FROM\s+[a-z0-9]""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)).containsMatchIn(sample) &&
                Regex("""\b(RUN|EXPOSE|ENV|COPY|WORKDIR|CMD)\s+""").containsMatchIn(sample) ->
                LanguageInfo("docker", "Dockerfile", "dockerfile")
            Regex("""^(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|WITH)\s""", RegexOption.IGNORE_CASE).containsMatchIn(firstNonComment) ||
                Regex("""\b(CREATE TABLE|INSERT INTO|SELECT .* FROM|PRIMARY KEY|FOREIGN KEY)\b""", RegexOption.IGNORE_CASE).containsMatchIn(sample) ->
                LanguageInfo("sql", "SQL", "sql")
            Regex("""^(def |class |import |from .* import |if __name__ ==)""", RegexOption.MULTILINE).containsMatchIn(sample) ||
                sample.contains(":\n    ") ->
                LanguageInfo("python", "Python", "py")
            Regex("""^(package |fun |val |data class |sealed class |suspend fun )""", RegexOption.MULTILINE).containsMatchIn(sample) ->
                LanguageInfo("kotlin", "Kotlin", "kt")
            Regex("""^(public class |using System;|namespace )""", RegexOption.MULTILINE).containsMatchIn(sample) ->
                if (sample.contains("using System;")) LanguageInfo("csharp", "C#", "cs") else LanguageInfo("java", "Java", "java")
            Regex("""^#include\s*<""", RegexOption.MULTILINE).containsMatchIn(sample) ->
                LanguageInfo("cpp", "C++", "cpp")
            Regex("""^package main""", RegexOption.MULTILINE).containsMatchIn(sample) && sample.contains("func main()") ->
                LanguageInfo("go", "Go", "go")
            Regex("""^(fn main\(\)|use std::|let mut)""", RegexOption.MULTILINE).containsMatchIn(sample) ->
                LanguageInfo("rust", "Rust", "rs")
            Regex("""^<\?php""", RegexOption.IGNORE_CASE).containsMatchIn(sample) ->
                LanguageInfo("php", "PHP", "php")
            Regex("""^#!/bin/(bash|sh|zsh)|^(echo|ls|mkdir|rm|sudo|npm|git|curl|export)\s""", RegexOption.MULTILINE).containsMatchIn(sample) ->
                LanguageInfo("bash", "Bash", "sh")
            Regex("""^(const|let|var|function|import|export|interface|type)\s""", RegexOption.MULTILINE).containsMatchIn(sample) ||
                sample.contains("=>") || sample.contains("console.log") ->
                if (Regex("""interface |type [A-Z]|: (string|number|boolean)""").containsMatchIn(sample)) {
                    LanguageInfo("typescript", "TypeScript", "ts")
                } else {
                    LanguageInfo("javascript", "JavaScript", "js")
                }
            Regex("""^[.#a-zA-Z0-9-]+\s*\{|@media|@import""").containsMatchIn(sample) ->
                LanguageInfo("css", "CSS", "css")
            Regex("""^(#\s|##\s|-\s|\[.*\]\(.*\))""", RegexOption.MULTILINE).containsMatchIn(sample) ->
                LanguageInfo("markdown", "Markdown", "md")
            Regex("""^[a-zA-Z0-9_-]+:\s+""").containsMatchIn(sample) && (sample.contains("\n  ") || sample.contains("\n- ")) ->
                LanguageInfo("yaml", "YAML", "yaml")
            else -> SUPPORTED_LANGUAGES[0]
        }
    }

    fun getSmartAction(text: String): SmartAction? {
        val t = text.trim()
        if (t.isEmpty()) return null

        if (Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""").matches(t)) {
            return SmartAction("email", com.hefimer.app.ui.theme.tr("Send Email", "Gửi Email"), Icons.Rounded.Email, "mailto:$t")
        }
        if (Regex("""^\+?[\d\s-]{8,15}$""").matches(t) && !t.contains('\n')) {
            val clean = t.replace(Regex("""[^\d+]"""), "")
            return SmartAction("phone", com.hefimer.app.ui.theme.tr("Call Now", "Gọi ngay"), Icons.Rounded.Call, "tel:$clean")
        }
        if (Regex("""^-?\d+(\.\d+)?,\s?-?\d+(\.\d+)?$""").matches(t)) {
            val q = java.net.URLEncoder.encode(t, "UTF-8")
            return SmartAction("map", com.hefimer.app.ui.theme.tr("Open Maps", "Mở bản đồ"), Icons.Rounded.LocationOn, "https://www.google.com/maps?q=$q")
        }
        if (t.length <= 9 && Regex("""^#(?:[0-9a-fA-F]{3,4}){1,2}$""").matches(t)) {
            return SmartAction("color", com.hefimer.app.ui.theme.tr("Color Info", "Chi tiết màu"), Icons.Rounded.Palette, "https://www.color-hex.com/color/${t.removePrefix("#")}")
        }
        if (Regex("""^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$""").matches(t)) {
            return SmartAction("ip", com.hefimer.app.ui.theme.tr("Whois IP", "Tra cứu IP"), Icons.Rounded.Shield, "https://who.is/whois-ip/ip-address/$t")
        }
        if (!t.any { it.isWhitespace() } &&
            (t.startsWith("http://", true) || t.startsWith("https://", true) || t.startsWith("www.", true) ||
                Regex("""^[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)+\.[a-zA-Z]{2,}(/\S*)?$""").matches(t))
        ) {
            val url = if (t.startsWith("http://", true) || t.startsWith("https://", true)) t else "https://$t"
            return SmartAction("link", com.hefimer.app.ui.theme.tr("Open Link", "Mở liên kết"), Icons.Rounded.OpenInNew, url)
        }
        return null
    }

    private val KEYWORDS = setOf(
        // Kotlin / Java / Swift / C#
        "fun", "val", "var", "class", "object", "interface", "data", "sealed", "enum", "override",
        "open", "abstract", "final", "suspend", "inline", "companion", "init", "constructor", "lateinit",
        "private", "public", "protected", "internal", "static", "void", "extends", "implements", "throws",
        "package", "import", "return", "if", "else", "when", "switch", "case", "default", "for", "while",
        "do", "try", "catch", "finally", "throw", "break", "continue", "this", "super", "new", "is", "as",
        "in", "out", "by", "where", "typealias", "guard", "defer", "protocol", "extension", "struct",
        "using", "namespace", "async", "await", "readonly", "virtual", "ref",
        // JS / TS
        "const", "let", "function", "export", "from", "of", "typeof", "instanceof", "delete", "yield",
        "type", "declare", "module", "require", "debugger",
        // Python / Ruby / Lua
        "def", "lambda", "with", "pass", "elif", "except", "raise", "assert", "global", "nonlocal",
        "and", "or", "not", "del", "end", "then", "elsif", "unless", "until", "begin", "rescue", "ensure",
        "local", "elseif", "repeat",
        // Go / Rust / C++ / PHP
        "func", "go", "chan", "select", "range", "fallthrough", "map",
        "fn", "mut", "pub", "use", "mod", "crate", "impl", "trait", "match", "loop", "move", "unsafe",
        "extern", "dyn", "sizeof", "typedef", "template", "typename", "constexpr", "auto", "nullptr",
        "echo", "foreach", "endif", "endforeach", "include", "define", "ifdef", "ifndef", "endif",
        // Bash / Docker / GraphQL
        "fi", "done", "esac", "export", "source", "alias", "unset", "shift", "exit",
        "FROM", "RUN", "CMD", "LABEL", "EXPOSE", "ENV", "ADD", "COPY", "ENTRYPOINT", "VOLUME", "USER",
        "WORKDIR", "ARG", "HEALTHCHECK", "SHELL",
        "query", "mutation", "subscription", "fragment", "schema", "scalar", "union", "input", "directive",
    )

    private val SQL_KEYWORDS = setOf(
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE",
        "ALTER", "DROP", "TABLE", "INDEX", "VIEW", "DATABASE", "SCHEMA", "JOIN", "INNER", "LEFT",
        "RIGHT", "OUTER", "FULL", "CROSS", "ON", "AS", "ORDER", "BY", "GROUP", "HAVING", "LIMIT",
        "OFFSET", "DISTINCT", "ALL", "UNION", "AND", "OR", "NOT", "IN", "IS", "NULL", "LIKE",
        "BETWEEN", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END", "PRIMARY", "KEY", "FOREIGN",
        "REFERENCES", "UNIQUE", "DEFAULT", "CHECK", "CONSTRAINT", "ASC", "DESC", "WITH", "RECURSIVE",
        "RETURNING", "COALESCE", "COUNT", "SUM", "AVG", "MIN", "MAX", "CAST", "TRUE", "FALSE",
    )

    private val LITERAL_CONSTANTS = setOf(
        "true", "false", "null", "nil", "None", "True", "False", "undefined", "NaN", "Infinity",
    )

    private val HASH_COMMENT_LANGS = setOf("python", "bash", "yaml", "ruby", "docker", "makefile", "php", "graphql")
    private val DASH_COMMENT_LANGS = setOf("sql", "lua")
    private val MARKUP_LANGS = setOf("markup", "xml", "svg", "php")
    private val CSS_LANGS = setOf("css", "scss")

    private fun isIdentStart(c: Char): Boolean = c.isLetter() || c == '_'
    private fun isIdentPart(c: Char): Boolean = c.isLetterOrDigit() || c == '_'
    private fun isHexDigit(c: Char): Boolean =
        c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'

    /**
     * Fast single-pass O(N) Dracula syntax highlighter matching Hefimer web's full color palette.
     * Supports up to 120,000 characters end-to-end with zero regex backtracking.
     */
    fun highlight(code: String, langId: String): AnnotatedString {
        if (langId == "text" || code.isEmpty() || code.length > MAX_HIGHLIGHT_CHARS) {
            return AnnotatedString(code)
        }

        val commentColor = HefimerColors.SyntaxComment
        val keywordColor = HefimerColors.SyntaxKeyword
        val numberColor = HefimerColors.SyntaxNumber
        val functionColor = HefimerColors.SyntaxFunction
        val stringColor = HefimerColors.SyntaxString
        val typeColor = HefimerColors.SyntaxType
        val decoratorColor = HefimerColors.SyntaxDecorator

        return buildAnnotatedString {
            append(code)
            val n = code.length
            var i = 0
            var lineStart = true

            fun span(color: Color, start: Int, end: Int, italic: Boolean = false, bold: Boolean = false) {
                if (end > start) {
                    addStyle(
                        SpanStyle(
                            color = color,
                            fontStyle = if (italic) FontStyle.Italic else null,
                            fontWeight = if (bold) FontWeight.Bold else null,
                        ),
                        start,
                        end,
                    )
                }
            }

            while (i < n) {
                val c = code[i]
                val next = if (i + 1 < n) code[i + 1] else '\u0000'

                if (c == '\n') {
                    lineStart = true
                    i++
                    continue
                }
                if (c == ' ' || c == '\t' || c == '\r') {
                    i++
                    continue
                }

                // 1. Markdown headings & inline code
                if (langId == "markdown") {
                    if (lineStart && c == '#') {
                        val start = i
                        while (i < n && code[i] != '\n') i++
                        span(functionColor, start, i, bold = true)
                        continue
                    }
                    if (c == '`') {
                        val start = i
                        i++
                        while (i < n && code[i] != '`' && code[i] != '\n') i++
                        if (i < n && code[i] == '`') i++
                        span(stringColor, start, i)
                        lineStart = false
                        continue
                    }
                    if (c == '*' && next == '*') {
                        val start = i
                        i += 2
                        while (i + 1 < n && !(code[i] == '*' && code[i + 1] == '*') && code[i] != '\n') i++
                        if (i + 1 < n && code[i] == '*' && code[i + 1] == '*') i += 2
                        span(decoratorColor, start, i, bold = true)
                        lineStart = false
                        continue
                    }
                }

                // 2. HTML / XML comments: <!-- ... -->
                if (c == '<' && i + 3 < n && code[i + 1] == '!' && code[i + 2] == '-' && code[i + 3] == '-') {
                    val start = i
                    i += 4
                    while (i + 2 < n && !(code[i] == '-' && code[i + 1] == '-' && code[i + 2] == '>')) i++
                    i = if (i + 2 < n) i + 3 else n
                    span(commentColor, start, i, italic = true)
                    lineStart = false
                    continue
                }

                // 3. Block comments: /* ... */
                if (c == '/' && next == '*') {
                    val start = i
                    i += 2
                    while (i + 1 < n && !(code[i] == '*' && code[i + 1] == '/')) i++
                    i = if (i + 1 < n) i + 2 else n
                    span(commentColor, start, i, italic = true)
                    lineStart = false
                    continue
                }

                // 4. Line comments: // ...
                if (c == '/' && next == '/') {
                    val start = i
                    while (i < n && code[i] != '\n') i++
                    span(commentColor, start, i, italic = true)
                    continue
                }

                // 5. SQL / Lua line comments: -- ...
                if (c == '-' && next == '-' && langId in DASH_COMMENT_LANGS) {
                    val start = i
                    while (i < n && code[i] != '\n') i++
                    span(commentColor, start, i, italic = true)
                    continue
                }

                // 6. Hash comments (#...) or CSS/SCSS hex colors (#RGB / #RRGGBB) or C++ #include
                if (c == '#') {
                    if (langId in HASH_COMMENT_LANGS) {
                        val start = i
                        while (i < n && code[i] != '\n') i++
                        span(commentColor, start, i, italic = true)
                        continue
                    }
                    if (langId == "cpp" && lineStart) {
                        val start = i
                        i++
                        while (i < n && isIdentPart(code[i])) i++
                        span(keywordColor, start, i)
                        lineStart = false
                        continue
                    }
                    if (isHexDigit(next)) {
                        val start = i
                        i++
                        while (i < n && isHexDigit(code[i])) i++
                        span(numberColor, start, i)
                        lineStart = false
                        continue
                    }
                }

                // 7. Triple-quoted strings ("""...""" or '''...''')
                if ((c == '"' || c == '\'') && i + 2 < n && code[i + 1] == c && code[i + 2] == c) {
                    val quote = c
                    val start = i
                    i += 3
                    while (i + 2 < n && !(code[i] == quote && code[i + 1] == quote && code[i + 2] == quote)) {
                        if (code[i] == '\\' && i + 1 < n) i += 2 else i++
                    }
                    i = if (i + 2 < n) i + 3 else n
                    span(stringColor, start, i)
                    lineStart = false
                    continue
                }

                // 8. Strings ("...", '...', `...`) — with JSON key detection
                if (c == '"' || c == '\'' || c == '`') {
                    val quote = c
                    val start = i
                    i++
                    while (i < n && code[i] != quote) {
                        if (code[i] == '\\' && i + 1 < n) {
                            i += 2
                        } else if (code[i] == '\n' && quote != '`') {
                            break
                        } else {
                            i++
                        }
                    }
                    if (i < n && code[i] == quote) i++
                    // Check if JSON object key ("key":)
                    var isJsonKey = false
                    if (quote == '"' && (langId == "json" || langId == "javascript" || langId == "typescript")) {
                        var k = i
                        while (k < n && (code[k] == ' ' || code[k] == '\t')) k++
                        if (k < n && code[k] == ':') isJsonKey = true
                    }
                    span(if (isJsonKey) typeColor else stringColor, start, i)
                    lineStart = false
                    continue
                }

                // 9. HTML / XML / JSX tags: <tagName ...> and </tagName>
                if (c == '<' && (langId in MARKUP_LANGS || langId == "javascript" || langId == "typescript")) {
                    val slash = next == '/'
                    val nameStart = if (slash) i + 2 else i + 1
                    if (nameStart < n && (code[nameStart].isLetter() || code[nameStart] == '_' || code[nameStart] == '!')) {
                        var j = nameStart
                        while (j < n && (code[j].isLetterOrDigit() || code[j] == '-' || code[j] == '_' || code[j] == ':' || code[j] == '.')) j++
                        span(keywordColor, nameStart, j)
                        i = j
                        // Highlight attributes inside tag until '>'
                        while (i < n && code[i] != '>') {
                            val ch = code[i]
                            if (ch == '"' || ch == '\'') {
                                val q = ch
                                val sStart = i
                                i++
                                while (i < n && code[i] != q) {
                                    if (code[i] == '\\' && i + 1 < n) i += 2 else i++
                                }
                                if (i < n && code[i] == q) i++
                                span(stringColor, sStart, i)
                            } else if (ch.isLetter() || ch == '_' || ch == '@') {
                                val aStart = i
                                while (i < n && (code[i].isLetterOrDigit() || code[i] == '-' || code[i] == '_' || code[i] == ':' || code[i] == '@')) i++
                                var peek = i
                                while (peek < n && code[peek] == ' ') peek++
                                if (peek < n && code[peek] == '=') {
                                    span(functionColor, aStart, i)
                                }
                            } else {
                                i++
                            }
                        }
                        if (i < n && code[i] == '>') i++
                        lineStart = false
                        continue
                    }
                }

                // 10. Decorators / Annotations / CSS at-rules (@Composable, @media)
                if (c == '@' && next != '\u0000' && (isIdentStart(next) || next == '-')) {
                    val start = i
                    i++
                    while (i < n && (isIdentPart(code[i]) || code[i] == '-')) i++
                    span(if (langId in CSS_LANGS) keywordColor else decoratorColor, start, i)
                    lineStart = false
                    continue
                }

                // 11. Shell / PHP / SCSS variables ($var, ${var})
                if (c == '$' && (isIdentStart(next) || next == '{')) {
                    val start = i
                    i++
                    if (i < n && code[i] == '{') {
                        while (i < n && code[i] != '}' && code[i] != '\n') i++
                        if (i < n && code[i] == '}') i++
                    } else {
                        while (i < n && (isIdentPart(code[i]) || code[i] == '-')) i++
                    }
                    span(decoratorColor, start, i)
                    lineStart = false
                    continue
                }

                // 12. Numbers (hex 0xFF, binary 0b10, floats, scientific notation, CSS units)
                if (c.isDigit()) {
                    val start = i
                    if (c == '0' && (next == 'x' || next == 'X')) {
                        i += 2
                        while (i < n && (isHexDigit(code[i]) || code[i] == '_')) i++
                    } else {
                        while (i < n && (code[i].isDigit() || code[i] == '_')) i++
                        if (i + 1 < n && code[i] == '.' && code[i + 1].isDigit()) {
                            i++
                            while (i < n && (code[i].isDigit() || code[i] == '_')) i++
                        }
                        if (i < n && (code[i] == 'e' || code[i] == 'E')) {
                            i++
                            if (i < n && (code[i] == '+' || code[i] == '-')) i++
                            while (i < n && code[i].isDigit()) i++
                        }
                        // Suffixes like f, L, u, px, rem, %, vh, vw, ms, s
                        while (i < n && (code[i].isLetter() || code[i] == '%')) i++
                    }
                    span(numberColor, start, i)
                    lineStart = false
                    continue
                }

                // 13. Identifiers, Keywords, YAML/CSS keys, Function calls, Types
                if (isIdentStart(c) || (langId in CSS_LANGS && c == '.' && isIdentStart(next))) {
                    val start = i
                    if (c == '.') i++
                    while (i < n && (isIdentPart(code[i]) || ((langId in CSS_LANGS || langId == "yaml") && code[i] == '-'))) i++
                    val word = code.substring(start, i)

                    var peek = i
                    while (peek < n && (code[peek] == ' ' || code[peek] == '\t')) peek++
                    val nextNonSpace = if (peek < n) code[peek] else '\u0000'

                    val color = when {
                        langId == "yaml" && lineStart && nextNonSpace == ':' -> typeColor
                        langId in CSS_LANGS && word.startsWith(".") -> functionColor
                        langId in CSS_LANGS && nextNonSpace == ':' -> typeColor
                        word in LITERAL_CONSTANTS -> numberColor
                        langId == "sql" && word.uppercase() in SQL_KEYWORDS -> keywordColor
                        word in KEYWORDS -> keywordColor
                        nextNonSpace == '(' -> functionColor
                        word.length > 1 && word[0].isUpperCase() -> typeColor
                        else -> null
                    }
                    if (color != null) {
                        span(color, start, i)
                    }
                    lineStart = false
                    continue
                }

                // 14. Multi-char operators (=>, ->, ::, ===, !==, &&, ||)
                if ((c == '=' && next == '>') || (c == '-' && next == '>') ||
                    (c == '&' && next == '&') || (c == '|' && next == '|') ||
                    (c == ':' && next == ':')
                ) {
                    span(keywordColor, i, i + 2)
                    i += 2
                    lineStart = false
                    continue
                }

                lineStart = false
                i++
            }
        }
    }
}
