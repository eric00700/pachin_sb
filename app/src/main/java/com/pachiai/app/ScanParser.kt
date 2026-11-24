package com.pachiai.app

import java.text.Normalizer

data class ParsedNumbers(val spins: String = "", val hits: String = "", val previous: String = "")

private val PREVIOUS_LINE = Regex("前日|昨日")
private val COMMA_BETWEEN_DIGITS = Regex("(?<=\\d),(?=\\d)")

private val SPIN_PATTERNS = listOf(
    Regex("(?:総回転|総スタート|累計回転|回転数|スタート)\\s*[:：]?\\s*([0-9]+)"),
    Regex("(?:total\\s*spins|spins)\\s*[:：]?\\s*([0-9]+)", RegexOption.IGNORE_CASE)
)
private val HIT_PATTERNS = listOf(
    Regex("(?:大当り|大当たり|当り|当たり)\\s*(?:回数)?\\s*[:：]?\\s*([0-9]+)"),
    Regex("(?:hits|jackpot)\\s*[:：]?\\s*([0-9]+)", RegexOption.IGNORE_CASE)
)
private val PREVIOUS_PATTERNS = listOf(
    Regex("(?:前日|昨日)\\s*(?:大当り|大当たり|当り|当たり)?\\s*(?:回数)?\\s*[:：]?\\s*([0-9]+)")
)

/** 전각 숫자·반각 가나를 정규화하고 숫자 사이의 쉼표를 제거합니다. */
fun normalizeForParse(raw: String): String =
    Normalizer.normalize(raw, Normalizer.Form.NFKC).replace(COMMA_BETWEEN_DIGITS, "")

private fun firstMatch(patterns: List<Regex>, text: String): String {
    for (p in patterns) {
        val m = p.find(text)
        if (m != null) return m.groupValues[1]
    }
    return ""
}

/** 전일 줄은 따로 분리해, 당일 당첨 횟수와 섞이지 않도록 합니다. */
fun parseNumbers(raw: String): ParsedNumbers {
    val lines = normalizeForParse(raw).lines()
    val previousText = lines.filter { PREVIOUS_LINE.containsMatchIn(it) }.joinToString("\n")
    val currentText = lines.filterNot { PREVIOUS_LINE.containsMatchIn(it) }.joinToString("\n")
    return ParsedNumbers(
        spins = firstMatch(SPIN_PATTERNS, currentText),
        hits = firstMatch(HIT_PATTERNS, currentText),
        previous = firstMatch(PREVIOUS_PATTERNS, previousText)
    )
}
