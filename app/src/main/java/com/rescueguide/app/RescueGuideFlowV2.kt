package com.rescueguide.app

import android.content.Intent
import android.net.Uri
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rescueguide.app.clinical.ClinicalPathwaysFa
import com.rescueguide.app.domain.*
import com.rescueguide.app.platform.OfflinePersianAudioPlayer
import com.rescueguide.app.platform.VentilationCuePlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun SessionStateHome() = SessionFlowState()

@Composable
fun RescueGuideRootV2() {
    val ctx = LocalContext.current
    val prefs = remember(ctx) { ctx.getSharedPreferences("rescueguide_settings", android.content.Context.MODE_PRIVATE) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark_theme", true)) }
    var flow by remember { mutableStateOf(SessionFlowState()) }
    var guide by remember { mutableStateOf(false) }
    var post by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFF42D6D0), secondary = Color(0xFF6C8CFF), tertiary = Color(0xFF9B7BFF), background = Color(0xFF061632), surface = Color(0xFF10264A)) else lightColorScheme(primary = Color(0xFF087F8C), secondary = Color(0xFF315FCB), tertiary = Color(0xFF7354C7), background = Color(0xFFF0F6FC), surface = Color.White)) {
        when {
            guide -> LicensedGuideV2(dark, { guide = false }, { guide = false; flow = SessionFlowState() })
            post -> PostRoscV2(dark, { post = false }, {
                post = false
                val s = flow.selection!!
                val p = ClinicalPathwaysFa.get(s.module, s.patientGroup!!, s.rescuerType!!, s.canProvideRescueBreaths, s.trainedRescuerCount)
                flow = flow.copy(page = SessionPage.ACTIVE_PATH, activeStepIndex = p.steps.indexOfFirst { it.id == "compressions" }.coerceAtLeast(0))
            }, { flow = SessionStateHome(); post = false })
            flow.page == SessionPage.HOME -> HomeV2(dark, { g ->
                flow = SessionFlowReducer.reduce(SessionFlowState(), SessionFlowAction.ChooseModule(AppModule.BASIC_LIFE_SUPPORT))
                flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ChoosePatient(g))
            }, { guide = true }, { settings = true })
            flow.page == SessionPage.SELECTION || flow.page == SessionPage.REVIEW_GATE -> RescuerV2(flow.selection?.patientGroup ?: PatientGroup.ADULT, dark,
                { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.Back) },
                { type, count -> flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ChooseRescuer(type, count)) },
                { flow = SessionStateHome() })
            else -> ActiveV2(flow.selection!!, flow.activeStepIndex, dark,
                { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.PreviousStep) },
                {
                    val s = flow.selection!!
                    val p = ClinicalPathwaysFa.get(s.module, s.patientGroup!!, s.rescuerType!!, s.canProvideRescueBreaths, s.trainedRescuerCount)
                    if (flow.activeStepIndex < p.steps.indexOfLast { it.id != "rosc" }) flow = SessionFlowReducer.reduce(flow, SessionFlowAction.NextStep)
                },
                { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.Reset) },
                { post = true },
                { flow = SessionStateHome(); post = false })
        }
        if (settings) AlertDialog(onDismissRequest = { settings = false }, title = { Text("تنظیمات برنامه") }, text = {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (dark) "تم شب" else "تم روز")
                    Switch(dark, onCheckedChange = { dark = it; prefs.edit().putBoolean("dark_theme", it).apply() })
                }
                TextButton(onClick = { settings = false; guide = true }) { Text("راهنمای برنامه") }
                TextButton(onClick = { settings = false; about = true }) { Text("درباره سازنده") }
            }
        }, confirmButton = { TextButton(onClick = { settings = false }) { Text("بستن") } })
        if (about) AlertDialog(onDismissRequest = { about = false }, title = { Text("درباره RescueGuide") },
            text = { Text("سازنده: ایمان محمدپور، کارشناس اورژانس.\n\nمبنای علمی: راهنماهای AHA/AAP 2025. تصاویر از منابع دارای مجوز هستند: BruceBlaus (CC BY-SA 4.0)، Rama (CC BY-SA 3.0 France)، Tokino (مالکیت عمومی)، Children’s Health Queensland (CC BY-NC 4.0؛ فقط استفاده غیرتجاری)، Trakotako و TenarAiuola. فهرست پیوند منابع و مجوزها در فایل مجوز تصاویر آمده است.\n\nدو تصویر روش فشار شیرخوار فقط برای استفاده غیرتجاری مجازند؛ برای انتشار تجاری باید مجوز جداگانه گرفته یا تصاویر جایگزین شوند.\n\nاین برنامه جایگزین تماس با اورژانس ۱۱۵ یا آموزش عملی نیست.") },
            dismissButton = { TextButton(onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/iman2pyman/RescueGuide/blob/main/docs/IMAGE_ASSET_LICENSES.md"))) } }) { Text("منابع و مجوز تصاویر") } },
            confirmButton = { TextButton(onClick = { about = false }) { Text("متوجه شدم") } })
    }
}

private val NavyTop = Color(0xFF03132F)
private val NavyBottom = Color(0xFF062A5A)
private val NeonCyan = Color(0xFF16D9F3)
private val NeonTeal = Color(0xFF00D8B0)
private val RoyalBlue = Color(0xFF087FE8)
private val Violet = Color(0xFF8D32D7)

private fun Modifier.medicalBackdrop(dark: Boolean = true): Modifier = this
    .background(Brush.verticalGradient(if (dark) listOf(NavyTop, Color(0xFF05234D), NavyBottom) else listOf(Color(0xFFEAF3FF), Color(0xFFD9EAF8), Color(0xFFF7FAFF))))
    .drawBehind {
        // Subtle dotted medical-tech texture, matching the approved three-phone mockup.
        val spacing = 13.dp.toPx()
        for (x in 0..(size.width / spacing).toInt()) {
            for (y in 0..(size.height / spacing).toInt()) {
                val px = x * spacing
                val py = y * spacing
                if ((x + y) % 3 != 0 && (px < size.width * .30f || py > size.height * .82f)) {
                    drawCircle((if (dark) Color(0xFF1686C8) else Color(0xFF4C91B8)).copy(alpha = .16f), radius = 1.15.dp.toPx(), center = androidx.compose.ui.geometry.Offset(px, py))
                }
            }
        }
        val path = Path().apply {
            moveTo(0f, size.height * .91f)
            lineTo(size.width * .18f, size.height * .91f)
            lineTo(size.width * .205f, size.height * .885f)
            lineTo(size.width * .22f, size.height * .94f)
            lineTo(size.width * .245f, size.height * .84f)
            lineTo(size.width * .265f, size.height * .91f)
            lineTo(size.width * .31f, size.height * .91f)
            lineTo(size.width, size.height * .91f)
        }
        drawPath(path, (if (dark) NeonCyan else Color(0xFF0B9EAC)).copy(alpha = .42f), style = Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }

@Composable
private fun BrandHomeButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.border(1.dp, NeonCyan.copy(alpha = .85f), RoundedCornerShape(18.dp)), shape = RoundedCornerShape(18.dp)) {
        Icon(Icons.Filled.Home, null, tint = Color.White)
        Spacer(Modifier.width(6.dp))
        Text("صفحه اول", color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VoiceWavePanel(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.MonitorHeart) {
    Card(Modifier.fillMaxWidth().border(1.5.dp, NeonCyan, RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF07386B))) {
        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("〰 ııııııııııııııııııııı 〰", color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun HomeV2(dark: Boolean, choose: (PatientGroup) -> Unit, guide: () -> Unit, settings: () -> Unit) {
    val ctx = LocalContext.current
    val audio = remember(ctx) { OfflinePersianAudioPlayer(ctx) }
    DisposableEffect(audio) { onDispose { audio.release() } }
    LaunchedEffect(Unit) { audio.play("سلام، من رابط صوتی احیا هستم. آرامش خود را حفظ کنید. در شرایط اضطراری با ۱۱۵ تماس بگیرید.") }
    val robot = remember { ctx.resources.getIdentifier("home_robot", "drawable", ctx.packageName) }
    Column(
        Modifier.fillMaxSize().medicalBackdrop(dark).verticalScroll(rememberScrollState())
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(58.dp).background(Brush.linearGradient(listOf(Color(0xFFFF4055), Color(0xFFB90032))), RoundedCornerShape(50))
                    .border(1.5.dp, Color.White, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.MonitorHeart, null, tint=Color.White, modifier=Modifier.size(39.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Rescue", color=Color.White, fontSize=26.sp, fontWeight=FontWeight.Black)
                    Text("Guide", color=Color(0xFFFF4055), fontSize=26.sp, fontWeight=FontWeight.Black)
                }
                Text("دستیار صوتی احیای اضطراری", color=Color.White, fontSize=13.sp, fontWeight=FontWeight.Bold)
            }
            Column(
                Modifier.background(Color(0xFF082A59), RoundedCornerShape(20.dp))
                    .border(1.dp, NeonCyan.copy(alpha=.8f), RoundedCornerShape(20.dp)).padding(horizontal=10.dp, vertical=7.dp),
                horizontalAlignment=Alignment.CenterHorizontally
            ) {
                Text("🇮🇷 ایران", color=Color.White, fontSize=12.sp, fontWeight=FontWeight.Bold)
                Text("۱۱۵", color=Color(0xFFFFE34D), fontSize=19.sp, fontWeight=FontWeight.Black)
            }
        }
        Row(Modifier.fillMaxWidth().height(184.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            if (robot != 0) Image(painterResource(robot), "دستیار صوتی RescueGuide", Modifier.weight(.9f).fillMaxSize(), contentScale=ContentScale.Fit)
            Card(
                Modifier.weight(1.1f).fillMaxHeight().border(1.5.dp, Color(0xFFB9D5FF), RoundedCornerShape(27.dp)),
                shape=RoundedCornerShape(27.dp), colors=CardDefaults.cardColors(containerColor=Color(0xFFE8EEFF))
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal=11.dp, vertical=12.dp), verticalArrangement=Arrangement.Center, horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("سلام! من دستیار صوتی احیا هستم.", color=Color(0xFF0756C8), fontSize=15.sp, fontWeight=FontWeight.Black, textAlign=TextAlign.Center)
                    Text("در شرایط اضطراری، آرامش خود را حفظ کنید.", color=Color(0xFF15213B), fontSize=13.sp, fontWeight=FontWeight.Bold, textAlign=TextAlign.Center)
                    Text("برای شروع، گروه سنی بیمار را انتخاب کنید.", color=Color(0xFF15213B), fontSize=12.sp, fontWeight=FontWeight.SemiBold, textAlign=TextAlign.Center)
                }
            }
        }
        VoiceWavePanel("سلام، من راهنمای صوتی احیا هستم")
        Text("گروه سنی بیمار را انتخاب کنید", color=Color.White, fontSize=18.sp, fontWeight=FontWeight.Black, modifier=Modifier.fillMaxWidth(), textAlign=TextAlign.Center)
        PatientTileV2("بزرگسال", "راهنمای احیای پایه بزرگسال", Color(0xFF087FE8)) { choose(PatientGroup.ADULT) }
        PatientTileV2("کودک", "از یک سال تا شروع بلوغ", Color(0xFF078FCB)) { choose(PatientGroup.CHILD) }
        PatientTileV2("شیرخوار", "زیر یک سال", Color(0xFF8437D1)) { choose(PatientGroup.INFANT) }
        Button(
            onClick={ runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", EmergencyDirectory.iran.emergencyNumber, null))) } },
            modifier=Modifier.fillMaxWidth().height(66.dp).border(1.dp, Color(0xFFFFA08A), RoundedCornerShape(23.dp)),
            shape=RoundedCornerShape(23.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFF321E))
        ) {
            Icon(Icons.Filled.Call, null, modifier=Modifier.size(27.dp)); Spacer(Modifier.width(8.dp))
            Text("تماس با اورژانس ایران ۱۱۵", fontSize=19.sp, fontWeight=FontWeight.Black)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick=settings, modifier=Modifier.weight(1f), shape=RoundedCornerShape(18.dp), border=androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)) {
                Icon(Icons.Filled.Settings, null, tint=Color.White); Text("تنظیمات", color=Color.White, fontSize=12.sp)
            }
            OutlinedButton(onClick=guide, modifier=Modifier.weight(1f), shape=RoundedCornerShape(18.dp), border=androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)) {
                Icon(Icons.Filled.MenuBook, null, tint=Color.White); Text("راهنمای برنامه", color=Color.White, fontSize=12.sp)
            }
        }
        Text("آرامش، اقدام درست، نجات جان", color=Color.White, fontWeight=FontWeight.Bold, fontSize=13.sp, modifier=Modifier.fillMaxWidth(), textAlign=TextAlign.Center)
    }
}

@Composable
private fun PatientTileV2(title: String, subtitle: String, tint: Color, click: () -> Unit) {
    val gradient = when (title) {
        "بزرگسال" -> listOf(Color(0xFF02C9B4), Color(0xFF087F9F))
        "کودک" -> listOf(Color(0xFF20B9FF), Color(0xFF0872D8))
        else -> listOf(Color(0xFFB344F4), Color(0xFF7621C8))
    }
    Card(Modifier.fillMaxWidth().height(94.dp).clickable(onClick=click).border(1.2.dp, Color.White.copy(alpha=.65f), RoundedCornerShape(25.dp)), shape=RoundedCornerShape(25.dp), colors=CardDefaults.cardColors(containerColor=tint)) {
        Row(Modifier.fillMaxSize().background(Brush.horizontalGradient(gradient)).padding(horizontal=20.dp), verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Filled.Favorite, null, tint=Color.White, modifier=Modifier.size(38.dp))
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) { Text(title, color=Color.White, fontSize=23.sp, fontWeight=FontWeight.Black); Text(subtitle, color=Color.White.copy(alpha=.92f), fontSize=12.sp) }
            Text("‹", color=Color.White, fontSize=36.sp, fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
private fun RescuerV2(group: PatientGroup, dark: Boolean, back: () -> Unit, choose: (RescuerType, Int) -> Unit, home: () -> Unit) {
    Column(Modifier.fillMaxSize().medicalBackdrop(dark).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement=Arrangement.spacedBy(18.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.Start) { BrandHomeButton(home); Spacer(Modifier.weight(1f)); TextButton(onClick=back) { Text("بازگشت", color=Color.White) } }
        Text("نوع امدادگر را انتخاب کنید", color=Color.White, fontSize=23.sp, fontWeight=FontWeight.Black, textAlign=TextAlign.Center)
        Text("گروه سنی: ${PatientRouting.label(group)}", color=Color(0xFFBCEFFF), fontSize=15.sp, fontWeight=FontWeight.Bold)
        RescuerTileV2("امدادگر آموزش‌دیده", Color(0xFF08BBAE), Icons.Filled.MonitorHeart) { choose(RescuerType.TRAINED_RESCUER, 1) }
        if (group != PatientGroup.ADULT) RescuerTileV2("دو امدادگر آموزش‌دیده", Color(0xFF087FE8), Icons.Filled.MonitorHeart) { choose(RescuerType.TRAINED_RESCUER, 2) }
        RescuerTileV2("فرد عادی", Color(0xFF7135C5), Icons.Filled.Favorite) { choose(RescuerType.LAY_RESCUER, 1) }
        Text("در صورت حضور دو امدادگر آموزش‌دیده، گزینه مربوط را انتخاب کنید.", color=Color.White, fontSize=12.sp, textAlign=TextAlign.Center)
    }
}

@Composable
private fun RescuerTileV2(title: String, tint: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) {
    Card(Modifier.fillMaxWidth().height(170.dp).clickable(onClick=click).border(1.2.dp, NeonCyan.copy(alpha=.8f), RoundedCornerShape(26.dp)), shape=RoundedCornerShape(26.dp), colors=CardDefaults.cardColors(containerColor=tint)) {
        Row(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(tint, tint.copy(alpha=.72f), Color(0xFF07517C)))).padding(22.dp), verticalAlignment=Alignment.CenterVertically) {
            Icon(icon, null, tint=Color.White, modifier=Modifier.size(54.dp))
            Spacer(Modifier.width(18.dp))
            Text(title, color=Color.White, fontSize=22.sp, fontWeight=FontWeight.Black, modifier=Modifier.weight(1f))
            Text("‹", color=Color.White, fontSize=34.sp)
        }
    }
}

@Composable
private fun ActiveV2(s: SessionSelection, stepIndex: Int, dark: Boolean, previous: () -> Unit, next: () -> Unit, exit: () -> Unit, rosc: () -> Unit, home: () -> Unit) {
    val ctx = LocalContext.current
    val audio = remember(ctx) { OfflinePersianAudioPlayer(ctx) }
    val scope = rememberCoroutineScope()
    val path = remember(s) { ClinicalPathwaysFa.get(s.module, s.patientGroup!!, s.rescuerType!!, s.canProvideRescueBreaths, s.trainedRescuerCount) }
    val steps = remember(path) { path.steps.filterNot { it.id == "rosc" } }
    val index = stepIndex.coerceIn(0, steps.lastIndex)
    val step = steps[index]
    val compressionTarget = if (s.patientGroup != PatientGroup.ADULT && s.rescuerType == RescuerType.TRAINED_RESCUER && s.trainedRescuerCount >= 2) 15 else 30
    val hasBreaths = s.canProvideRescueBreaths != false
    var compressionCount by remember(step.id, s.trainedRescuerCount) { mutableIntStateOf(0) }
    var ventilationCue by remember(step.id) { mutableIntStateOf(0) }
    val fg = if (dark) Color.White else Color(0xFF10264A)
    val sub = if (dark) Color(0xFFD6E7FF) else Color(0xFF31577D)
    val lifecycle = LocalLifecycleOwner.current
    var autoAdvance by remember { mutableStateOf<Job?>(null) }
    fun speakCurrentStep() {
        audio.stop()
        autoAdvance?.cancel()
        val started = audio.play(step.instruction) {
            autoAdvance?.cancel()
            if (step.id != "compressions") autoAdvance = scope.launch { delay(2000); if (index < steps.lastIndex) next() }
        }
        if (!started && step.id != "compressions") autoAdvance = scope.launch { delay(9000); if (index < steps.lastIndex) next() }
    }
    LaunchedEffect(step.id) { speakCurrentStep() }
    LaunchedEffect(step.id, compressionTarget, hasBreaths) {
        if (step.id == "compressions") {
            val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 72) }.getOrNull()
            val breathCue = VentilationCuePlayer()
            try {
                while (true) {
                    compressionCount = 0
                    ventilationCue = 0
                    // 110/min midpoint of the AHA-recommended 100–120/min range.
                    for (n in 1..compressionTarget) {
                        compressionCount = n
                        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 65)
                        delay(545L)
                    }
                    if (hasBreaths) {
                        ventilationCue = 1
                        breathCue.playBreathCue()
                        delay(1_000L)
                        ventilationCue = 2
                        breathCue.playBreathCue()
                        delay(1_000L)
                    }
                }
            } finally { breathCue.stop(); tone?.release() }
        } else {
            compressionCount = 0
            ventilationCue = 0
        }
    }
    DisposableEffect(audio, lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) audio.stop() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { autoAdvance?.cancel(); lifecycle.lifecycle.removeObserver(observer); audio.release() }
    }
    Column(Modifier.fillMaxSize().medicalBackdrop(dark)
        .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("مرحله " + (index + 1) + " از " + steps.size, color = Color.White, fontWeight=FontWeight.Bold)
            BrandHomeButton { autoAdvance?.cancel(); audio.stop(); exit(); home() }
        }
        Text("راهنمای احیای پایه", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Card(Modifier.fillMaxWidth().border(1.dp, NeonCyan.copy(alpha=.75f), RoundedCornerShape(22.dp)), shape=RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0B315C))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(step.title, color = Color(0xFF65E9F0), fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(step.instruction, color = Color.White, fontSize = 15.sp, lineHeight = 23.sp)
                if (step.id == "ratio" && s.patientGroup != PatientGroup.ADULT) Text("اگر قادر به تنفس کمکی نیستید، فشارهای پیوسته را ادامه دهید.", color = Color(0xFFFFB5A8), fontWeight = FontWeight.Bold)
                if (step.timeCritical) Text("اقدام فوری — برای تأیید منتظر نمانید.", color = Color(0xFFFFB5A8), fontWeight = FontWeight.Bold)
            }
        }
        StepVisualV2(s.patientGroup!!, step.id)
        if (step.id == "compressions") {
            Card(Modifier.fillMaxWidth().border(1.dp, NeonCyan, RoundedCornerShape(24.dp)), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF073A6B))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("شمارشگر خودکار ماساژ قلبی", color = Color.White, fontWeight = FontWeight.Black)
                    Text(if (ventilationCue > 0) "تنفس کمکی ${if (ventilationCue == 1) "اول" else "دوم"}" else "$compressionCount / $compressionTarget", color = NeonCyan, fontSize = 38.sp, fontWeight = FontWeight.Black)
                    Text(if (ventilationCue > 0) "اکنون تنفس را بدهید؛ هر تنفس حدود یک ثانیه و فقط تا بالا آمدن قفسه سینه." else "ریتم هدف: ۱۰۰ تا ۱۲۰ فشار در دقیقه", color = fg, textAlign = TextAlign.Center, fontSize = 12.sp)
                    Text(if (hasBreaths) "نسبت این مسیر: $compressionTarget فشار به ۲ تنفس" else "فشارهای پیوسته؛ بدون توقف برای تنفس", color = fg, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (step.id == "compressions") {
            Button(onClick = { audio.stop(); next() }, modifier = Modifier.fillMaxWidth()) { Text("دستگاه AED / مرحله بعد آماده است") }
        }
        if (index == steps.lastIndex) Text("مراحل صوتی پایان یافت؛ احیا را تا رسیدن کمک ادامه دهید.", color = fg)
        Button(onClick = { autoAdvance?.cancel(); audio.stop(); rosc() }, modifier = Modifier.fillMaxWidth().height(66.dp).border(1.dp, Color(0xFFFFB59E), RoundedCornerShape(23.dp)), shape=RoundedCornerShape(23.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF321E))) {
            Text("علائم حیاتی برگشت", fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { autoAdvance?.cancel(); audio.stop(); previous() }, enabled = index > 0, modifier = Modifier.weight(1f).height(54.dp), shape=RoundedCornerShape(18.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF075EB5))) { Text("مرحله قبل", fontWeight=FontWeight.Bold) }
            Button(onClick = { speakCurrentStep() }, modifier = Modifier.weight(1f).height(54.dp), shape=RoundedCornerShape(18.dp), colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF087BC5))) { Text("تکرار توضیح فعلی", fontWeight=FontWeight.Bold) }
        }
        Text("راهنمای صوتی خودکار پیش می‌رود؛ برای اقدامات حیاتی منتظر تأیید نمانید. با ۱۱۵ تماس بگیرید و از اپراتور پیروی کنید.", color = sub, fontSize = 11.sp)
    }
}

@Composable
private fun StepVisualV2(group: PatientGroup, id: String) {
    val ctx = LocalContext.current
    val data = when {
        id == "compressions" && group == PatientGroup.INFANT -> Triple("فشار شیرخوار", "guide_infant_compressions", "دو شست با دست‌های حلقه‌شده؛ نه روش دو انگشتی.")
        id == "compressions" && group == PatientGroup.CHILD -> Triple("فشار کودک", "guide_child_compressions", "یک یا دو دست متناسب با جثه.")
        id == "compressions" -> Triple("فشار بزرگسال", "guide_adult_compressions", "پاشنه دست‌ها روی نیمه پایینی جناغ.")
        id == "scene" -> Triple("ایمنی صحنه", "", "پیش از نزدیک شدن، از ایمن بودن محیط مطمئن شوید.")
        id == "response" -> Triple("بررسی پاسخ‌دهی", "", "بیمار را صدا بزنید و پاسخ او را بررسی کنید.")
        id == "emergency" -> Triple("تماس با اورژانس", "", "با ۱۱۵ تماس بگیرید و دستگاه شوک درخواست کنید.")
        id == "breathing" && group == PatientGroup.INFANT -> Triple("بررسی تنفس شیرخوار", "guide_infant_breathing_check", "گَس‌گَس تنفس طبیعی نیست.")
        id == "breathing" -> Triple("بررسی تنفس", "guide_checking_respiration", "تنفس طبیعی را حداکثر طی ۱۰ ثانیه بررسی کنید.")
        id == "continue" && group == PatientGroup.INFANT -> Triple("ادامه احیا", "guide_infant_compressions", "فشارها را با حداقل وقفه ادامه دهید.")
        id == "continue" && group == PatientGroup.CHILD -> Triple("ادامه احیا", "guide_child_compressions", "فشارها را با حداقل وقفه ادامه دهید.")
        id == "continue" -> Triple("ادامه احیا", "guide_adult_compressions", "فشارها را با حداقل وقفه ادامه دهید.")
        id == "ratio" && group == PatientGroup.INFANT -> Triple("تنفس شیرخوار", "guide_infant_breathing", "دهان و بینی را بپوشانید.")
        id == "ratio" && group == PatientGroup.CHILD -> Triple("تنفس کودک", "guide_child_breathing", "فقط تا بالا آمدن قفسه سینه.")
        id == "ratio" -> Triple("تنفس بزرگسال", "guide_adult_breathing", "بینی را ببندید و حدود یک ثانیه هوا بدهید.")
        id == "aed" -> Triple("پدهای AED", "guide_aed_pad_position", "طبق دستور دستگاه نصب کنید.")
        else -> Triple("بررسی تنفس", "guide_infant_breathing_check", "گَس‌گَس تنفس طبیعی نیست.")
    }
    val resource = remember(data.second) { ctx.resources.getIdentifier(data.second, "drawable", ctx.packageName) }
    var zoom by remember { mutableStateOf(0) }
    Card(Modifier.fillMaxWidth().border(1.dp, NeonCyan.copy(alpha=.7f), RoundedCornerShape(22.dp)), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2E57)), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(data.first, color = Color(0xFF65E9F0), fontWeight = FontWeight.Bold)
            if (resource != 0) Image(painterResource(resource), data.first, Modifier.fillMaxWidth().height(235.dp).clickable { zoom = resource }, contentScale = ContentScale.Fit)
            else if (id == "emergency") Icon(Icons.Filled.Call, contentDescription = data.first, tint = Color(0xFF0B62C8), modifier = Modifier.size(88.dp).padding(12.dp))
            else if (id == "response") Icon(Icons.Filled.Person, contentDescription = data.first, tint = Color(0xFF0B62C8), modifier = Modifier.size(88.dp).padding(12.dp))
            else if (id == "scene") Icon(Icons.Filled.Info, contentDescription = data.first, tint = Color(0xFF0B62C8), modifier = Modifier.size(88.dp).padding(12.dp))
            else Text("تصویر آموزشی در مرحله دریافت دارایی‌ها قرار می‌گیرد.", color = Color(0xFF31577D), modifier = Modifier.padding(20.dp))
            Text(data.third, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
    if (zoom != 0) Dialog(onDismissRequest = { zoom = 0 }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(12.dp), color = Color.White, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(zoom), data.first, Modifier.fillMaxWidth().height(520.dp), contentScale = ContentScale.Fit)
                TextButton(onClick = { zoom = 0 }) { Text("بستن") }
            }
        }
    }
}

@Composable
private fun LicensedGuideV2(dark: Boolean, back: () -> Unit, home: () -> Unit) {
    val ctx = LocalContext.current
    val fg = if (dark) Color.White else Color(0xFF10264A)
    val bg = if (dark) listOf(Color(0xFF031943), Color(0xFF062B68)) else listOf(Color(0xFFEAF3FF), Color(0xFFF7FAFF))
    val items = listOf(
        Triple("فشار بزرگسال", "guide_adult_compressions", "محل دست‌ها روی جناغ."),
        Triple("راه هوایی بزرگسال", "guide_adult_airway", "سر به عقب و چانه بالا."),
        Triple("بررسی تنفس", "guide_checking_respiration", "تنفس طبیعی را بررسی کنید."),
        Triple("تنفس بزرگسال", "guide_adult_breathing", "هر تنفس حدود یک ثانیه."),
        Triple("فشار کودک", "guide_child_compressions", "یک یا دو دست متناسب با جثه."),
        Triple("تنفس کودک", "guide_child_breathing", "فقط تا بالا آمدن قفسه سینه."),
        Triple("فشار شیرخوار — دو شست", "guide_infant_compressions", "دو شست با دست‌های حلقه‌شده؛ روش دو انگشتی استفاده نشود."),
        Triple("فشار شیرخوار — پاشنه یک دست", "guide_infant_compressions_onehand", "در صورت ناتوانی در دستیابی به عمق مناسب با روش دو شست، از پاشنه یک دست استفاده می‌شود."),
        Triple("راه هوایی شیرخوار", "guide_infant_airway", "سر در وضعیت خنثی."),
        Triple("تنفس شیرخوار", "guide_infant_breathing", "دهان و بینی را بپوشانید."),
        Triple("بررسی تنفس شیرخوار", "guide_infant_breathing_check", "گَس‌گَس طبیعی نیست."),
        Triple("پد AED", "guide_aed_pad_position", "طبق دستور دستگاه."),
        Triple("وضعیت ریکاوری", "guide_recovery_position", "فقط با تنفس طبیعی و نبود منع.")
    )
    var zoom by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().medicalBackdrop(dark).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { OutlinedButton(onClick = back, border=androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)) { Text("بازگشت", color=Color.White) }; Spacer(Modifier.weight(1f)); BrandHomeButton(home) }
        Text("راهنمای استفاده و آموزش مراحل BLS", color = fg, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text("این بخش برای مطالعه و آشنایی پیش از شرایط اضطراری است. در حادثه واقعی ابتدا با ۱۱۵ تماس بگیرید و از دستور اپراتور پیروی کنید.", color = fg, fontSize = 13.sp)
        Text("راهنمای استفاده: در صفحه اول گروه سنی را انتخاب کنید؛ سپس نوع و تعداد امدادگران را مشخص کنید. هنگام احیا، صدا و شمارشگر خودکار اجرا می‌شوند. دکمه تکرار برای شنیدن دوباره دستور و دکمه مرحله قبل برای برگشت است. در صورت بازگشت علائم حیاتی، دکمه سبز را لمس کنید. دکمه صفحه اول در تمام صفحات برای خروج سریع در دسترس است.", color = fg, fontSize = 13.sp, lineHeight = 21.sp)
        Text("""بزرگسال: ایمنی صحنه، بررسی پاسخ و تنفس، تماس با ۱۱۵، فشارهای قفسه سینه با سرعت ۱۰۰ تا ۱۲۰ در دقیقه و نسبت ۳۰:۲ در صورت امکان تنفس کمکی.

کودک: عمق فشار حدود یک‌سوم قطر قفسه سینه؛ یک امدادگر ۳۰:۲ و دو امدادگر آموزش‌دیده ۱۵:۲.

شیرخوار: فشار با دو شست و دست‌های حلقه‌شده یا پاشنه یک دست؛ روش دو انگشتی استفاده نشود. یک امدادگر ۳۰:۲ و دو امدادگر آموزش‌دیده ۱۵:۲.

در همه گروه‌ها بازگشت کامل قفسه سینه و حداقل وقفه را رعایت کنید.""", color = fg, fontSize = 13.sp, lineHeight = 21.sp)
        items.forEach { item ->
            val r = remember(item.second) { ctx.resources.getIdentifier(item.second, "drawable", ctx.packageName) }
            Card(Modifier.fillMaxWidth().border(1.dp, NeonCyan.copy(alpha=.55f), RoundedCornerShape(20.dp)), shape=RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2E57))) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(item.first, color = Color(0xFF65E9F0), fontWeight = FontWeight.Bold)
                    if (r != 0) Image(painterResource(r), item.first, Modifier.fillMaxWidth().height(180.dp).clickable { zoom = r }, contentScale = ContentScale.Fit)
                    else Text("تصویر در مرحله دریافت دارایی‌ها قرار می‌گیرد.", color = Color(0xFF31577D), modifier = Modifier.padding(18.dp))
                    Text(item.third, color = Color.White, fontSize = 12.sp)
                }
            }
        }
        Text("پدیدآورندگان و مجوزها: docs/IMAGE_ASSET_LICENSES.md", color = fg, fontSize = 11.sp)
    }
    if (zoom != 0) Dialog(onDismissRequest = { zoom = 0 }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(12.dp), color = Color.White) {
            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(zoom), "تصویر بزرگ", Modifier.fillMaxWidth().height(520.dp), contentScale = ContentScale.Fit)
                TextButton(onClick = { zoom = 0 }) { Text("بستن") }
            }
        }
    }
}

@Composable
private fun PostRoscV2(dark: Boolean, back: () -> Unit, restart: () -> Unit, home: () -> Unit) {
    val ctx = LocalContext.current
    val fg = if (dark) Color.White else Color(0xFF10264A)
    val bg = if (dark) listOf(Color(0xFF031943), Color(0xFF062B68)) else listOf(Color(0xFFEAF3FF), Color(0xFFF7FAFF))
    val recovery = remember { ctx.resources.getIdentifier("guide_recovery_position", "drawable", ctx.packageName) }
    val steps = listOf(
        "بررسی تنفس و هوشیاری" to "پاسخ بیمار و تنفس طبیعی را بررسی کنید؛ نفس‌های بریده یا گَس‌گَس طبیعی نیستند.",
        "باز نگه‌داشتن راه هوایی" to "راه هوایی را باز نگه دارید و مراقب انسداد ناشی از زبان یا ترشحات باشید.",
        "وضعیت مناسب بیمار" to "اگر بیهوش اما دارای تنفس طبیعی است، در نبود منع یا احتمال آسیب، وضعیت ریکاوری.",
        "تماس با اورژانس" to "با ۱۱۵ تماس بگیرید و ارزیابی پزشکی فوری را پیگیری کنید.",
        "پایش مداوم" to "تنفس و هوشیاری را مرتب بررسی کنید و بیمار را تنها نگذارید.",
        "بدحال‌شدن مجدد" to "اگر بیهوش شد و تنفس طبیعی نداشت، فوراً احیا را از سر بگیرید."
    )
    Column(Modifier.fillMaxSize().medicalBackdrop(dark).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = back) { Text("بازگشت به راهنمای احیا") }
            BrandHomeButton(home)
        }
        Text("مراقبت پس از بازگشت علائم حیاتی", color = fg, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text("این صفحه راهنمای صوتی ندارد؛ مراحل را بخوانید و بیمار را پیوسته پایش کنید.", color = fg, fontSize = 12.sp)
        steps.forEachIndexed { i, item ->
            Card(Modifier.fillMaxWidth().border(1.dp, NeonCyan.copy(alpha=.55f), RoundedCornerShape(20.dp)), shape=RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2E57))) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text((i + 1).toString(), color = Color.White, modifier = Modifier.background(Color(0xFF0B62C8), RoundedCornerShape(22.dp)).padding(10.dp), fontWeight = FontWeight.Black)
                    Column(Modifier.weight(1f)) {
                        Text(item.first, color = Color(0xFF65E9F0), fontWeight = FontWeight.Bold)
                        Text(item.second, color = Color.White, fontSize = 12.sp)
                        if (i == 2 && recovery != 0) Image(painterResource(recovery), "وضعیت ریکاوری", Modifier.fillMaxWidth().height(130.dp), contentScale = ContentScale.Fit)
                    }
                }
            }
        }
        Button(onClick = restart, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("وضعیت بدتر شد — شروع مجدد احیا") }
    }
}
