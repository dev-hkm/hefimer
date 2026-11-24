package com.hefimer.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.hefimer.app.ui.theme.HefimerColors

data class LanguageInfo(val id: String, val label: String, val ext: String)

data class SmartAction(
    val type: String,
    val label: String,
    val icon: ImageVector,
    val url: String,
)

object CodeHighlighter {
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
        val firstNonComment = t.lineSequence()
            .map { it.trim() }
            .firstOrNull {
                it.isNotEmpty() && !it.startsWith("--") && !it.startsWith("//") &&
                    !it.startsWith("/*") && !it.startsWith("#")
            }.orEmpty()

        return when {
            Regex("""^<!DOCTYPE html|<html|<body|<div|<span|<p|<a """, RegexOption.IGNORE_CASE).containsMatchIn(t) ->
                LanguageInfo("markup", "HTML", "html")
            Regex("""<svg""", RegexOption.IGNORE_CASE).containsMatchIn(t) ->
                LanguageInfo("markup", "SVG", "svg")
            Regex("""^<\?xml""", RegexOption.IGNORE_CASE).containsMatchIn(t) ->
                LanguageInfo("markup", "XML", "xml")
            (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]")) &&
                Regex("""^[\{\[]\s*("|\d|true|false|null)""").containsMatchIn(t) ->
                LanguageInfo("json", "JSON", "json")
            Regex("""^FROM\s+[a-z0-9]""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)).containsMatchIn(t) &&
                Regex("""\b(RUN|EXPOSE|ENV)\s+""").containsMatchIn(t) ->
                LanguageInfo("docker", "Dockerfile", "dockerfile")
            Regex("""^(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP|WITH)\s""", RegexOption.IGNORE_CASE).containsMatchIn(firstNonComment) ||
                Regex("""\b(CREATE TABLE|INSERT INTO|SELECT .* FROM|PRIMARY KEY|FOREIGN KEY)\b""", RegexOption.IGNORE_CASE).containsMatchIn(t) ->
                LanguageInfo("sql", "SQL", "sql")
            Regex("""^(def |class |import |from .* import |if __name__ ==)""", RegexOption.MULTILINE).containsMatchIn(t) ||
                t.contains(":\n    ") ->
                LanguageInfo("python", "Python", "py")
            Regex("""^(package |fun |val |data class |sealed class )""", RegexOption.MULTILINE).containsMatchIn(t) ->
                LanguageInfo("kotlin", "Kotlin", "kt")
            Regex("""^(public class |using System;|namespace )""", RegexOption.MULTILINE).containsMatchIn(t) ->
                if (t.contains("using System;")) LanguageInfo("csharp", "C#", "cs") else LanguageInfo("java", "Java", "java")
            Regex("""^#include\s*<""", RegexOption.MULTILINE).containsMatchIn(t) ->
                LanguageInfo("cpp", "C++", "cpp")
            Regex("""^package main""", RegexOption.MULTILINE).containsMatchIn(t) && t.contains("func main()") ->
                LanguageInfo("go", "Go", "go")
            Regex("""^(fn main\(\)|use std::|let mut)""", RegexOption.MULTILINE).containsMatchIn(t) ->
                LanguageInfo("rust", "Rust", "rs")
            Regex("""^<\?php""", RegexOption.IGNORE_CASE).containsMatchIn(t) ->
                LanguageInfo("php", "PHP", "php")
            Regex("""^#!/bin/(bash|sh|zsh)|^(echo|ls|mkdir|rm|sudo|npm|git|curl)\s""", RegexOption.MULTILINE).containsMatchIn(t) ->
                LanguageInfo("bash", "Bash", "sh")
            Regex("""^(const|let|var|function|import|export|interface|type)\s""", RegexOption.MULTILINE).containsMatchIn(t) ||
                t.contains("=>") || t.contains("console.log") ->
                if (Regex("""interface |type [A-Z]|: (string|number|boolean)""").containsMatchIn(t)) {
                    LanguageInfo("typescript", "TypeScript", "ts")
                } else {
                    LanguageInfo("javascript", "JavaScript", "js")
                }
            Regex("""^[.#a-zA-Z0-9-]+\s*\{|@media|@import""").containsMatchIn(t) ->
                LanguageInfo("css", "CSS", "css")
            Regex("""^(#\s|##\s|-\s|\[.*\]\(.*\))""", RegexOption.MULTILINE).containsMatchIn(t) ->
                LanguageInfo("markdown", "Markdown", "md")
            Regex("""^[a-zA-Z0-9_-]+:\s+""").containsMatchIn(t) && (t.contains("\n  ") || t.contains("\n- ")) ->
                LanguageInfo("yaml", "YAML", "yaml")
            else -> SUPPORTED_LANGUAGES[0]
        }
    }

    fun getSmartAction(text: String): SmartAction? {
        val t = text.trim()
        if (t.isEmpty()) return null

        if (Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""").matches(t)) {
            return SmartAction("email", "Send Email", Icons.Rounded.Email, "mailto:$t")
        }
        if (Regex("""^\+?[\d\s-]{8,15}$""").matches(t) && !t.contains('\n')) {
            val clean = t.replace(Regex("""[^\d+]"""), "")
            return SmartAction("phone", "Call Now", Icons.Rounded.Call, "tel:$clean")
        }
        if (Regex("""^-?\d+(\.\d+)?,\s?-?\d+(\.\d+)?$""").matches(t)) {
            val q = java.net.URLEncoder.encode(t, "UTF-8")
            return SmartAction("map", "Open Maps", Icons.Rounded.LocationOn, "https://www.google.com/maps?q=$q")
        }
        if (t.length <= 9 && Regex("""^#(?:[0-9a-fA-F]{3,4}){1,2}$""").matches(t)) {
            return SmartAction("color", "Color Info", Icons.Rounded.Palette, "https://www.color-hex.com/color/${t.removePrefix("#")}")
        }
        if (Regex("""^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$""").matches(t)) {
            return SmartAction("ip", "Whois IP", Icons.Rounded.Shield, "https://who.is/whois-ip/ip-address/$t")
        }
        if (!t.any { it.isWhitespace() } &&
            (t.startsWith("http://", true) || t.startsWith("https://", true) || t.startsWith("www.", true) ||
                Regex("""^[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)+\.[a-zA-Z]{2,}(/\S*)?$""").matches(t))
        ) {
            val url = if (t.startsWith("http://", true) || t.startsWith("https://", true)) t else "https://$t"
            return SmartAction("link", "Open Link", Icons.Rounded.OpenInNew, url)
        }
        return null
    }

    private val KEYWORDS = setOf(
        "fun", "val", "var", "class", "object", "interface", "data", "sealed", "enum", "override",
        "return", "if", "else", "when", "for", "while", "do", "try", "catch", "finally", "throw",
        "import", "export", "from", "package", "const", "let", "function", "async", "await", "new",
        "def", "lambda", "yield", "with", "as", "pass", "break", "continue", "true", "false", "null",
        "nil", "None", "True", "False", "public", "private", "protected", "static", "void", "struct",
        "impl", "fn", "mut", "pub", "use", "match", "SELECT", "FROM", "WHERE", "INSERT", "INTO",
        "UPDATE", "DELETE", "CREATE", "TABLE", "JOIN", "ORDER", "BY", "GROUP", "LIMIT", "AND", "OR",
        "select", "from", "where", "insert", "into", "update", "delete", "create", "table", "join",
    )

    private val TOKEN_REGEX = Regex(
        """(//[^\n]*|#[^\n]*|--[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|`(?:\\.|[^`\\])*`|@\w+|\b\d+(?:\.\d+)?\b|\b[A-Z][A-Za-z0-9_]*\b|\b[a-zA-Z_][a-zA-Z0-9_]*(?=\s*\()|\b[a-zA-Z_][a-zA-Z0-9_]*\b)""",
    )

    /**
     * Fast Dracula syntax highlighter matching src/index.css color palette.
     */
    fun highlight(code: String, langId: String): AnnotatedString {
        if (langId == "text" || code.length > 25_000) {
            return AnnotatedString(code)
        }
        return buildAnnotatedString {
            append(code)
            TOKEN_REGEX.findAll(code).forEach { m ->
                val tok = m.value
                val color = when {
                    tok.startsWith("//") || tok.startsWith("/*") || tok.startsWith("--") ||
                        (tok.startsWith("#") && langId in setOf("python", "bash", "yaml", "ruby", "docker", "makefile")) ->
                        HefimerColors.SyntaxComment
                    tok.startsWith("\"") || tok.startsWith("'") || tok.startsWith("`") ->
                        HefimerColors.SyntaxString
                    tok.startsWith("@") ->
                        HefimerColors.SyntaxDecorator
                    tok[0].isDigit() ->
                        HefimerColors.SyntaxNumber
                    tok in KEYWORDS ->
                        HefimerColors.SyntaxKeyword
                    m.range.last + 1 < code.length && code[m.range.last + 1] == '(' ->
                        HefimerColors.SyntaxFunction
                    tok[0].isUpperCase() && tok.length > 1 ->
                        HefimerColors.SyntaxType
                    else -> null
                }
                if (color != null) {
                    addStyle(SpanStyle(color = color), m.range.first, m.range.last + 1)
                }
            }
        }
    }
}
