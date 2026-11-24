package com.pachiai.app

import java.text.Normalizer

data class TipOption(val jp: String, val ko: String, val selectedInPhoto: Boolean = false)

data class TipItem(
    val jp: String,
    val ko: String,
    val options: List<TipOption>,
    val desc: String,
    /** true: 화면에 보이는 문구를 그대로 번역 / false: 항목명 기반 추정 */
    val confirmed: Boolean
)

data class TipSection(val jp: String, val ko: String, val note: String, val items: List<TipItem>)

/** 에반게리온 17 「ゲーム性カスタマイズ」 메뉴 (사용자 제공 사진 2장 기준) */
val EVA17_SECTIONS: List<TipSection> = listOf(
    TipSection(
        jp = "▼通常時", ko = "통상 시", note = "사진 1",
        items = listOf(
            TipItem(
                jp = "先告知カスタム", ko = "선고지 커스텀",
                options = listOf(
                    TipOption("OFF", "끔", true), TipOption("アラート", "알림"),
                    TipOption("地鳴", "땅울림"), TipOption("次回予告", "다음 회 예고")
                ),
                desc = "당첨 가능성을 먼저 알려 주는 ‘선고지’ 연출의 종류를 고릅니다. 사진의 5번째 버튼은 글자가 흐려 판독하지 못했습니다. 설명 문구가 사진에 없어 항목명 기준의 해석입니다.",
                confirmed = false
            ),
            TipItem(
                jp = "先読み", ko = "선독(선읽기) 예고",
                options = listOf(
                    TipOption("なし", "없음"), TipOption("オススメ", "추천"),
                    TipOption("チャンス", "찬스", true), TipOption("熱", "열")
                ),
                desc = "보류 등을 미리 읽는 선독 예고를 어느 수준부터 보여 줄지 정하는 항목으로 보입니다. 없음 → 추천 → 찬스 → 열 순서의 단계 설정이라는 해석은 추정입니다.",
                confirmed = false
            ),
            TipItem(
                jp = "レバブルアップ", ko = "레바블 업",
                options = listOf(TipOption("OFF", "끔"), TipOption("ON", "켬", true)),
                desc = "‘アップ’가 붙은 설정입니다. 아래 ‘에어 커스텀’ 설명(출현율 상승)과 같은 방식으로 해당 연출의 출현율을 올리는 ON/OFF로 추정합니다. ‘レバブル’의 정확한 의미는 사진만으로 확인할 수 없습니다.",
                confirmed = false
            ),
            TipItem(
                jp = "インパクトフラッシュアップ", ko = "임팩트 플래시 업",
                options = listOf(TipOption("OFF", "끔"), TipOption("ON", "켬", true)),
                desc = "임팩트 플래시 연출의 출현율을 올리는 ON/OFF로 추정합니다. 설명 문구가 사진에 없습니다.",
                confirmed = false
            ),
            TipItem(
                jp = "エアーカスタム", ko = "에어 커스텀",
                options = listOf(TipOption("OFF", "끔"), TipOption("ON", "켬", true)),
                desc = "화면 하단 설명: 「エアーの出現率がアップします。」 → “에어의 출현율이 올라갑니다.” 커서가 놓인 항목의 설명으로 보입니다.",
                confirmed = true
            )
        )
    ),
    TipSection(
        jp = "▼演出カスタム", ko = "연출 커스텀", note = "사진 2",
        items = listOf(
            TipItem(
                jp = "通常時先役物落下カスタム", ko = "통상 시 선역물 낙하 커스텀",
                options = listOf(
                    TipOption("OFF", "끔", true), TipOption("40%", "40%"),
                    TipOption("90%", "90%"), TipOption("EX", "EX")
                ),
                desc = "OFF에 커서가 있을 때 하단 information에 「先役物落下発生なし。」 → “선역물 낙하 발생 없음.”이 표시됩니다. 즉 OFF는 이 연출을 아예 내지 않는 설정입니다. 40%/90%는 발생 비율 단계로 추정하며, EX의 의미는 사진만으로 알 수 없습니다. (‘役物’는 기기 위의 가동 장치를 뜻합니다.)",
                confirmed = true
            ),
            TipItem(
                jp = "先読みカスタム", ko = "선독 커스텀",
                options = listOf(TipOption("なし", "없음"), TipOption("(판독 불가)", "—"), TipOption("チャンス", "찬스")),
                desc = "통상 시 탭의 ‘先読み’와 같은 계열로 보이는 항목입니다. 가운데 버튼은 글자가 흐려 판독하지 못했고, 현재 선택값도 화면이 어두워 알 수 없습니다.",
                confirmed = false
            ),
            TipItem(
                jp = "レバブルアップ", ko = "레바블 업",
                options = listOf(TipOption("OFF", "끔"), TipOption("ON", "켬")),
                desc = "통상 시 탭에도 있는 항목입니다. 이 화면에서는 커서가 다른 줄에 있어 어둡게 표시되어 선택값을 알 수 없습니다.",
                confirmed = false
            ),
            TipItem(
                jp = "インパクトフラッシュアップ", ko = "임팩트 플래시 업",
                options = listOf(TipOption("OFF", "끔"), TipOption("ON", "켬")),
                desc = "통상 시 탭에도 있는 항목입니다. 이 화면에서는 선택값을 알 수 없습니다.",
                confirmed = false
            )
        )
    )
)

data class GlossaryEntry(val jp: String, val ko: String)

val GLOSSARY: List<GlossaryEntry> = listOf(
    GlossaryEntry("ゲーム性カスタマイズ", "게임성 커스터마이즈"),
    GlossaryEntry("通常時", "통상 시"),
    GlossaryEntry("演出カスタム", "연출 커스텀"),
    GlossaryEntry("通常時先役物落下カスタム", "통상 시 선역물 낙하 커스텀"),
    GlossaryEntry("先役物落下", "선역물 낙하"),
    GlossaryEntry("先役物", "선역물(앞서 움직이는 가동 장치, 추정)"),
    GlossaryEntry("役物", "역물(기기 위 가동 장치)"),
    GlossaryEntry("落下", "낙하"),
    GlossaryEntry("発生なし", "발생 없음"),
    GlossaryEntry("先読みカスタム", "선독 커스텀"),
    GlossaryEntry("先告知カスタム", "선고지 커스텀"),
    GlossaryEntry("先読み", "선독(선읽기)"),
    GlossaryEntry("アラート", "알림(경보)"),
    GlossaryEntry("地鳴", "땅울림"),
    GlossaryEntry("次回予告", "다음 회 예고"),
    GlossaryEntry("オススメ", "추천"),
    GlossaryEntry("チャンス", "찬스"),
    GlossaryEntry("レバブルアップ", "레바블 업(추정: 출현율 상승)"),
    GlossaryEntry("インパクトフラッシュアップ", "임팩트 플래시 업(추정)"),
    GlossaryEntry("エアーカスタム", "에어 커스텀"),
    GlossaryEntry("エアー", "에어"),
    GlossaryEntry("カスタム", "커스텀(사용자 설정)"),
    GlossaryEntry("出現率", "출현율"),
    GlossaryEntry("アップ", "업(상승)"),
    GlossaryEntry("カーソル移動", "커서 이동"),
    GlossaryEntry("決定", "결정"),
    GlossaryEntry("設定", "설정"),
    GlossaryEntry("大当り", "대당첨"),
    GlossaryEntry("大当たり", "대당첨"),
    GlossaryEntry("当り回数", "당첨 횟수"),
    GlossaryEntry("当たり回数", "당첨 횟수"),
    GlossaryEntry("確変", "확변(확률 변동)"),
    GlossaryEntry("時短", "시간 단축"),
    GlossaryEntry("保留", "보류"),
    GlossaryEntry("予告", "예고"),
    GlossaryEntry("演出", "연출"),
    GlossaryEntry("回転数", "회전수"),
    GlossaryEntry("前日", "전일")
)

private val WHITESPACE = Regex("\\s+")

/** OCR 원문에서 사전 용어를 찾습니다. 더 긴 용어에 포함된 짧은 용어는 제외합니다. */
fun matchGlossary(raw: String): List<GlossaryEntry> {
    val text = Normalizer.normalize(raw, Normalizer.Form.NFKC).replace(WHITESPACE, "")
    val hits = GLOSSARY.filter { text.contains(it.jp) }
    return hits.filter { a ->
        hits.none { b -> b !== a && b.jp.length > a.jp.length && b.jp.contains(a.jp) }
    }
}
