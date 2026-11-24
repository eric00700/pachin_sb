package com.pachiai.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab { ANALYZE, SCAN, TIPS, GUIDE }

data class Machine(
    val number: String, val model: String, val spins: Int, val hitsToday: Int,
    val previousHits: Int, val investmentYen: Int, val note: String,
    val savedAt: Long = System.currentTimeMillis()
) {
    /** 내가 직접 투입한 금액을 입력한 경우에만 의미가 있는 값 */
    val spinsPer1000Yen: Double get() = if (investmentYen > 0) spins.toDouble() / investmentYen * 1000.0 else 0.0
    /** 총 회전수 1,000회전당 당첨 횟수 (과거 기록 비교용) */
    val hitsPer1000Spins: Double get() = if (spins > 0) hitsToday.toDouble() / spins * 1000.0 else 0.0
    val observedHitInterval: Double get() = if (hitsToday > 0) spins.toDouble() / hitsToday else 0.0
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PachiApp() }
    }
}

@Composable
fun PachiApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("pachiai", Context.MODE_PRIVATE) }
    val machines = remember { mutableStateListOf<Machine>().apply { addAll(loadMachines(prefs)) } }

    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = AppTab.entries[tabIndex]
    var number by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var spins by rememberSaveable { mutableStateOf("") }
    var hits by rememberSaveable { mutableStateOf("") }
    var previous by rememberSaveable { mutableStateOf("") }
    var investment by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }

    var scanText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var translatedSource by remember { mutableStateOf("") }
    var translationStatus by remember { mutableStateOf("번역 모델 준비 중") }

    fun saveAll() { saveMachines(prefs, machines.toList()) }

    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Forest, selectedTextColor = Forest, indicatorColor = Mint.copy(alpha = .45f)
    )

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Forest, onPrimary = Color.White, background = Paper,
            surface = Card, onSurface = Ink, onBackground = Ink, secondary = Mint
        )
    ) {
        Scaffold(containerColor = Paper, bottomBar = {
            NavigationBar(containerColor = Card, tonalElevation = 8.dp) {
                NavigationBarItem(selected = tab == AppTab.ANALYZE, onClick = { tabIndex = AppTab.ANALYZE.ordinal },
                    icon = { Icon(Icons.Default.Analytics, null) }, label = { Text("기계 분석", fontSize = 10.sp) }, colors = navColors)
                NavigationBarItem(selected = tab == AppTab.SCAN, onClick = { tabIndex = AppTab.SCAN.ordinal },
                    icon = { Icon(Icons.Default.CameraAlt, null) }, label = { Text("기계 스캔", fontSize = 10.sp) }, colors = navColors)
                NavigationBarItem(selected = tab == AppTab.TIPS, onClick = { tabIndex = AppTab.TIPS.ordinal },
                    icon = { Icon(Icons.Default.Translate, null) }, label = { Text("팁 번역", fontSize = 10.sp) }, colors = navColors)
                NavigationBarItem(selected = tab == AppTab.GUIDE, onClick = { tabIndex = AppTab.GUIDE.ordinal },
                    icon = { Icon(Icons.Default.Lightbulb, null) }, label = { Text("팁 사전", fontSize = 10.sp) }, colors = navColors)
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Header(tab)
                when (tab) {
                    // SCAN과 TIPS는 같은 분기라서 탭을 오가도 카메라가 다시 만들어지지 않습니다.
                    AppTab.SCAN, AppTab.TIPS -> {
                        val tips = tab == AppTab.TIPS
                        val glossaryHits = remember(scanText) {
                            if (scanText.isBlank()) emptyList() else matchGlossary(scanText)
                        }
                        val translationIsStale = translatedSource.isNotBlank() &&
                            translatedSource != scanText.trim().replace(Regex("\\s+"), " ")
                        LazyColumn(
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(if (tips) "게임 화면의 일본어를 비춰주세요" else "기계 데이터를 비춰주세요",
                                        fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                                    Text(if (tips) "게임 팁·미션·설정 메뉴·조작 설명을 한국어로 번역합니다."
                                        else "화면의 숫자와 일본어 표기를 실시간으로 읽습니다.",
                                        fontSize = 13.sp, color = Subtle)
                                }
                            }
                            item {
                                CameraScanner(
                                    onStatus = { translationStatus = it },
                                    onOcr = { raw ->
                                        scanText = raw
                                        if (tab != AppTab.TIPS) {
                                            val p = parseNumbers(raw)
                                            if (p.spins.isNotBlank()) spins = p.spins
                                            if (p.hits.isNotBlank()) hits = p.hits
                                            if (p.previous.isNotBlank()) previous = p.previous
                                        }
                                    },
                                    onTranslated = { src, out -> translatedSource = src; translatedText = out }
                                )
                            }
                            item {
                                Column(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Card).padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(9.dp).clip(CircleShape)
                                            .background(if (scanText.isBlank()) Subtle else Color(0xFF2A9D68)))
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (scanText.isBlank()) "인식 대기 중" else "문자 인식 중",
                                            fontWeight = FontWeight.SemiBold, color = Ink)
                                    }
                                    Text(
                                        if (scanText.isBlank()) "카메라를 화면에 맞추면 인식한 내용이 여기에 나타납니다." else scanText,
                                        color = Subtle, fontSize = 12.sp, lineHeight = 18.sp
                                    )
                                    HorizontalDivider(color = Line)

                                    if (tips && glossaryHits.isNotEmpty()) {
                                        Text("용어 사전 매칭 (정확)", fontWeight = FontWeight.Bold, color = Ink)
                                        glossaryHits.forEach { g ->
                                            Row {
                                                Text(g.jp, Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                                Text(g.ko, Modifier.weight(1f), fontSize = 12.sp, color = Forest)
                                            }
                                        }
                                        HorizontalDivider(color = Line)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (tips) "게임 팁 한국어 번역 (기계 번역)" else "한국어 번역",
                                            fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                                        Text(translationStatus, color = Subtle, fontSize = 10.sp)
                                    }
                                    if (translatedText.isNotBlank() && translationIsStale) {
                                        Text("이전 인식 결과의 번역입니다. 새 번역을 기다리는 중…", color = Danger, fontSize = 10.sp)
                                    }
                                    Text(
                                        if (translatedText.isBlank())
                                            "일본어 문구를 인식하면 한국어 번역이 여기에 표시됩니다. 최초 사용 시 번역 모델을 다운로드합니다."
                                        else translatedText,
                                        color = if (translatedText.isBlank()) Subtle else Forest,
                                        fontSize = 13.sp, lineHeight = 19.sp
                                    )

                                    if (!tips) {
                                        HorizontalDivider(color = Line)
                                        Text("자동 추출 결과", fontWeight = FontWeight.Bold, color = Ink)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            MiniValue("총 회전수", spins.ifBlank { "—" }, Modifier.weight(1f))
                                            MiniValue("당일 당첨", hits.ifBlank { "—" }, Modifier.weight(1f))
                                            MiniValue("전일 당첨", previous.ifBlank { "—" }, Modifier.weight(1f))
                                        }
                                        Text("인식값은 반드시 실제 화면과 대조한 뒤 저장하세요.", fontSize = 11.sp, color = Subtle)
                                        Button(
                                            onClick = { tabIndex = AppTab.ANALYZE.ordinal },
                                            modifier = Modifier.fillMaxWidth().height(50.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Forest)
                                        ) { Text("인식값 확인하고 저장하기", fontWeight = FontWeight.Bold) }
                                    } else {
                                        HorizontalDivider(color = Line)
                                        Text("번역 사용 방법", fontWeight = FontWeight.Bold, color = Ink)
                                        Text("① 게임 팁이나 설정 메뉴가 나오는 화면에 카메라를 맞춥니다.\n② 글자가 선명하게 보이도록 거리를 조절합니다.\n③ 용어 사전 매칭과 기계 번역을 함께 확인합니다.\n※ 자주 나오는 메뉴는 ‘팁 사전’ 탭에서 미리 볼 수 있습니다.",
                                            color = Subtle, fontSize = 12.sp, lineHeight = 19.sp)
                                    }
                                }
                            }
                        }
                    }

                    AppTab.GUIDE -> TipGuideScreen()

                    AppTab.ANALYZE -> {
                        val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.KOREA) }
                        LazyColumn(
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("오늘의 기계 현황", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                                    Text("수집한 데이터를 한눈에 비교해 보세요.", fontSize = 13.sp, color = Subtle)
                                }
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SummaryCard("등록 기계", "${machines.size}대", "저장된 기계", Modifier.weight(1f))
                                    SummaryCard(
                                        "최고 당첨 빈도",
                                        machines.maxOfOrNull { it.hitsPer1000Spins }?.let { "%.1f회".format(it) } ?: "—",
                                        "1,000회전당", Modifier.weight(1f), true
                                    )
                                }
                            }
                            item {
                                Column(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Card).padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(11.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("기계 정보 입력", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                                            Text("카메라 스캔 후 숫자가 자동으로 채워집니다.", fontSize = 11.sp, color = Subtle)
                                        }
                                        TextButton(onClick = { tabIndex = AppTab.SCAN.ordinal }) {
                                            Icon(Icons.Default.CameraAlt, null, Modifier.size(17.dp))
                                            Spacer(Modifier.width(4.dp)); Text("스캔")
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        EntryField("기계 번호", number, { number = it }, Modifier.weight(1f))
                                        EntryField("기종명", model, { model = it }, Modifier.weight(1f))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        EntryField("현재 총 회전수", spins, { spins = it }, Modifier.weight(1f), true)
                                        EntryField("당일 당첨 횟수", hits, { hits = it }, Modifier.weight(1f), true)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        EntryField("전일 당첨 횟수", previous, { previous = it }, Modifier.weight(1f), true)
                                        EntryField("내 투입 금액(엔, 선택)", investment, { investment = it }, Modifier.weight(1f), true)
                                    }
                                    Text("표시기의 총 회전수는 모든 손님의 합계입니다. ‘내 투입 금액’은 직접 플레이한 경우에만 입력하세요.", fontSize = 10.sp, color = Subtle, lineHeight = 15.sp)
                                    EntryField("메모 (선택)", note, { note = it }, Modifier.fillMaxWidth())
                                    if (error.isNotBlank()) Text(error, color = Danger, fontSize = 12.sp)
                                    Button(
                                        onClick = {
                                            val s = spins.toIntOrNull()
                                            val h = hits.toIntOrNull()
                                            val p = if (previous.isBlank()) 0 else previous.toIntOrNull()
                                            val yen = if (investment.isBlank()) 0 else investment.toIntOrNull()
                                            if (number.isBlank() || s == null || h == null || p == null || yen == null ||
                                                s < 0 || h < 0 || p < 0 || yen < 0
                                            ) {
                                                error = "기계 번호와 숫자를 확인해 주세요."
                                            } else {
                                                val key = number.trim()
                                                machines.removeAll { it.number == key }
                                                machines.add(Machine(key, model.trim(), s, h, p, yen, note.trim()))
                                                saveAll()
                                                number = ""; model = ""; spins = ""; hits = ""; previous = ""
                                                investment = ""; note = ""; error = ""
                                                scanText = ""; translatedText = ""; translatedSource = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Forest)
                                    ) { Text("기계 데이터 저장", fontWeight = FontWeight.Bold) }
                                }
                            }
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("기계별 비교 순위", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                                        Text("1,000회전당 당첨 빈도가 높은 순으로 정렬했습니다.", fontSize = 11.sp, color = Subtle)
                                    }
                                    Surface(color = Mint.copy(alpha = .45f), shape = RoundedCornerShape(50)) {
                                        Text("${machines.size}대", Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                            color = Forest, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            if (machines.isEmpty()) item { EmptyState(onScan = { tabIndex = AppTab.SCAN.ordinal }) }
                            items(machines.sortedByDescending { it.hitsPer1000Spins }, key = { it.number }) { m ->
                                MachineCard(m, if (m.savedAt > 0) dateFormat.format(Date(m.savedAt)) else "—") { machines.remove(m); saveAll() }
                            }
                            item {
                                Text("※ 통계는 입력된 과거 기록을 비교할 뿐, 다음 당첨이나 수익을 보장하거나 예측하지 않습니다.",
                                    color = Subtle, fontSize = 10.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(tab: AppTab) {
    Row(
        Modifier.fillMaxWidth().background(Forest).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Mint), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Smartphone, null, tint = Forest, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("PACHI AI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text("파칭코 데이터 기록 · 일본어 번역", color = Color.White.copy(alpha = .72f), fontSize = 11.sp)
        }
        Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(50)) {
            Text(
                when (tab) {
                    AppTab.ANALYZE -> "기록 대시보드"
                    AppTab.SCAN -> "기계 데이터 스캔"
                    AppTab.TIPS -> "게임 팁 번역"
                    AppTab.GUIDE -> "팁 사전"
                },
                Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                color = Mint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, caption: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(if (highlight) Forest else Card).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(title, color = if (highlight) Color.White.copy(alpha = .72f) else Subtle, fontSize = 11.sp)
        Text(value, color = if (highlight) Mint else Forest, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(caption, color = if (highlight) Color.White.copy(alpha = .65f) else Subtle, fontSize = 10.sp)
    }
}

@Composable
private fun MiniValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Paper).padding(10.dp)) {
        Text(label, color = Subtle, fontSize = 10.sp)
        Text(value, color = Forest, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyState(onScan: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Card).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Analytics, null, tint = Forest, modifier = Modifier.size(34.dp))
        Text("아직 등록된 기계가 없습니다", fontWeight = FontWeight.Bold, color = Ink)
        Text("카메라로 기계 데이터를 스캔하거나\n위 입력란에 정보를 등록해 주세요.", color = Subtle, fontSize = 12.sp, lineHeight = 18.sp)
        OutlinedButton(onClick = onScan, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.CameraAlt, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("카메라로 스캔하기")
        }
    }
}

@Composable
fun EntryField(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier, numeric: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onValue, label = { Text(label, fontSize = 11.sp) }, modifier = modifier,
        singleLine = true, shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Forest, unfocusedBorderColor = Line, focusedLabelColor = Forest, unfocusedLabelColor = Subtle,
            focusedTextColor = Ink, unfocusedTextColor = Ink, cursorColor = Forest,
            focusedContainerColor = Card, unfocusedContainerColor = Card
        )
    )
}

@Composable
fun MachineCard(m: Machine, savedLabel: String, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Card).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("기계 ${m.number}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                if (m.model.isNotBlank()) { Spacer(Modifier.width(7.dp)); Text(m.model, color = Subtle, fontSize = 11.sp) }
            }
            Text("총 ${m.spins}회전  ·  당일 당첨 ${m.hitsToday}회  ·  전일 ${m.previousHits}회", color = Subtle, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Mint.copy(alpha = .45f), shape = RoundedCornerShape(7.dp)) {
                    Text("1,000회전당 당첨 ${"%.1f".format(m.hitsPer1000Spins)}회",
                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = Forest, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(7.dp))
                Text("평균 간격 ${if (m.observedHitInterval > 0) "%.0f회전".format(m.observedHitInterval) else "—"}", color = Subtle, fontSize = 10.sp)
            }
            if (m.investmentYen > 0) {
                Text("내 투입 1,000엔당 ${"%.1f".format(m.spinsPer1000Yen)}회전", color = Subtle, fontSize = 10.sp)
            }
            if (m.note.isNotBlank()) Text(m.note, color = Subtle, fontSize = 10.sp)
            Text("저장 $savedLabel", color = Subtle, fontSize = 9.sp)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "삭제", tint = Color(0xFFB1B9B3)) }
    }
}

private fun saveMachines(prefs: SharedPreferences, list: List<Machine>) {
    val arr = JSONArray()
    list.forEach { m ->
        arr.put(JSONObject().apply {
            put("number", m.number); put("model", m.model); put("spins", m.spins); put("hits", m.hitsToday)
            put("previous", m.previousHits); put("investment", m.investmentYen); put("note", m.note)
            put("savedAt", m.savedAt)
        })
    }
    prefs.edit().putString("machines", arr.toString()).apply()
}

fun loadMachines(prefs: SharedPreferences): List<Machine> = try {
    val arr = JSONArray(prefs.getString("machines", "[]"))
    (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Machine(
            o.optString("number"), o.optString("model"), o.optInt("spins"), o.optInt("hits"),
            o.optInt("previous"), o.optInt("investment", 0), o.optString("note"),
            o.optLong("savedAt", 0L)
        )
    }
} catch (e: Exception) {
    emptyList()
}
