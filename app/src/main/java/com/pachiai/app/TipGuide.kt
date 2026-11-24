package com.pachiai.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TipGuideScreen() {
    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("에반게리온 17 · 게임성 커스터마이즈", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("ゲーム性カスタマイズ 설정 화면의 탭(▼通常時, ▼演出カスタム)별 일본어·한국어 대조표입니다.", fontSize = 12.sp, color = Subtle, lineHeight = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(Orange))
                    Spacer(Modifier.width(6.dp))
                    Text("주황색 = 사진에서 선택되어 있던 값", fontSize = 11.sp, color = Subtle)
                }
            }
        }
        EVA17_SECTIONS.forEach { sec ->
            item(key = "header-${sec.jp}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${sec.jp}  ${sec.ko}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Forest, modifier = Modifier.weight(1f))
                    Text(sec.note, fontSize = 10.sp, color = Subtle)
                }
            }
            items(sec.items, key = { "${sec.jp}-${it.jp}" }) { TipCard(it) }
        }
        item { GlossaryCard() }
        item {
            Text(
                "※ 하단 버튼: カーソル移動(커서 이동) / 決定(결정). 설명 문구가 화면에 없는 항목은 ‘추정’으로 표시했으니, 실제 기기 화면의 안내문을 ‘팁 번역’ 탭으로 확인해 주세요.",
                color = Subtle, fontSize = 10.sp, lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun TipCard(tip: TipItem) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Card).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tip.jp, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                Text(tip.ko, fontSize = 12.sp, color = Forest)
            }
            Surface(
                color = if (tip.confirmed) Mint.copy(alpha = .45f) else Line,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    if (tip.confirmed) "화면 문구 기준" else "추정",
                    Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = if (tip.confirmed) Forest else Subtle
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tip.options.forEach { o ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(if (o.selectedInPhoto) Orange else Paper)
                        .padding(vertical = 7.dp, horizontal = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(o.jp, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        color = if (o.selectedInPhoto) Color.White else Ink)
                    Text(o.ko, fontSize = 9.sp, maxLines = 1,
                        color = if (o.selectedInPhoto) Color.White else Subtle)
                }
            }
        }
        Text(tip.desc, fontSize = 12.sp, lineHeight = 18.sp, color = Subtle)
    }
}

@Composable
private fun GlossaryCard() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Card).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text("용어 사전", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
        Text("‘팁 번역’ 탭에서 인식된 일본어에 이 용어가 있으면 자동으로 짚어 줍니다.", fontSize = 11.sp, color = Subtle)
        HorizontalDivider(color = Line)
        GLOSSARY.forEach { g ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(g.jp, Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(g.ko, Modifier.weight(1f), fontSize = 12.sp, color = Forest)
            }
        }
    }
}
