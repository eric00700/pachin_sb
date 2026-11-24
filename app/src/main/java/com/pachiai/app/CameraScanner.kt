package com.pachiai.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val WS = Regex("\\s+")

@Composable
fun CameraScanner(
    onStatus: (String) -> Unit,
    onOcr: (String) -> Unit,
    onTranslated: (source: String, translated: String) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    if (hasPermission) {
        ScannerBody(onStatus, onOcr, onTranslated)
    } else {
        Column(
            Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)).background(Card).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CameraAlt, null, tint = Forest, modifier = Modifier.size(34.dp))
            Text("카메라 권한이 필요합니다.", color = Ink, fontWeight = FontWeight.SemiBold)
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("카메라 권한 허용") }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun ScannerBody(
    onStatus: (String) -> Unit,
    onOcr: (String) -> Unit,
    onTranslated: (String, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentStatus by rememberUpdatedState(onStatus)
    val currentOcr by rememberUpdatedState(onOcr)
    val currentTranslated by rememberUpdatedState(onTranslated)
    var downloadFailed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lifecycleOwner, retry) {
        val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.JAPANESE)
                .setTargetLanguage(TranslateLanguage.KOREAN)
                .build()
        )
        val executor = Executors.newSingleThreadExecutor()
        val busy = AtomicBoolean(false)
        val closed = AtomicBoolean(false)
        val modelReady = AtomicBoolean(false)
        val lastRun = AtomicLong(0L)
        val requestId = AtomicInteger(0)
        val cache = LinkedHashMap<String, String>() // 메인 스레드에서만 접근
        var lastSeen = ""
        var stableCount = 0
        var lastTranslated = ""

        downloadFailed = false
        currentStatus("번역 모델 준비 중 (최초 1회 Wi-Fi 필요)")
        translator.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build())
            .addOnSuccessListener {
                if (!closed.get()) { modelReady.set(true); currentStatus("번역 준비 완료") }
            }
            .addOnFailureListener {
                if (!closed.get()) {
                    downloadFailed = true
                    currentStatus("번역 모델 다운로드 실패 · Wi-Fi 확인 후 다시 시도해 주세요")
                }
            }

        fun translate(source: String) {
            val cached = cache[source]
            if (cached != null) {
                currentTranslated(source, cached)
                currentStatus("한국어 번역 완료")
                return
            }
            val id = requestId.incrementAndGet()
            currentStatus("번역 중…")
            translator.translate(source)
                .addOnSuccessListener { out ->
                    if (!closed.get() && id == requestId.get()) {
                        cache[source] = out
                        if (cache.size > 40) cache.remove(cache.keys.first())
                        currentTranslated(source, out)
                        currentStatus("한국어 번역 완료")
                    }
                }
                .addOnFailureListener {
                    if (!closed.get() && id == requestId.get()) {
                        lastTranslated = "" // 같은 문구를 다시 시도할 수 있도록
                        currentStatus("번역 실패 · 화면을 다시 비춰 주세요")
                    }
                }
        }

        fun handle(rawText: String) {
            if (closed.get()) return
            val raw = rawText.trim()
            if (raw.isEmpty()) return
            currentOcr(raw)
            if (!modelReady.get() || raw.length < 2) return
            val key = raw.replace(WS, " ")
            if (key == lastSeen) stableCount++ else { lastSeen = key; stableCount = 1 }
            // 같은 인식 결과가 2회 연속 나올 때만 번역 (프레임마다 번역하지 않음)
            if (stableCount >= 2 && key != lastTranslated) {
                lastTranslated = key
                translate(key)
            }
        }

        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                    ).build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysis.setAnalyzer(executor) { proxy ->
            val media = proxy.image
            val now = SystemClock.elapsedRealtime()
            if (media == null || closed.get() || now - lastRun.get() < 450 || !busy.compareAndSet(false, true)) {
                proxy.close()
            } else {
                lastRun.set(now)
                try {
                    val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                    recognizer.process(image)
                        .addOnSuccessListener { text -> handle(text.text) }
                        .addOnCompleteListener { busy.set(false); proxy.close() }
                } catch (e: Exception) {
                    busy.set(false)
                    proxy.close()
                }
            }
        }

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            if (!closed.get()) {
                try {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (e: Exception) {
                    currentStatus("카메라를 시작하지 못했습니다")
                }
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            closed.set(true)
            try { analysis.clearAnalyzer() } catch (e: Exception) { }
            try { if (providerFuture.isDone) providerFuture.get().unbindAll() } catch (e: Exception) { }
            executor.shutdown()
            recognizer.close()
            translator.close()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(20.dp))
        )
        if (downloadFailed) {
            TextButton(onClick = { retry++ }) { Text("번역 모델 다시 받기") }
        }
    }
}
