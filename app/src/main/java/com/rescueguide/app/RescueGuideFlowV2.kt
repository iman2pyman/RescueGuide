package com.rescueguide.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFF4DA3FF), secondary = Color(0xFF36C8C8)) else lightColorScheme(primary = Color(0xFF0B4F71), secondary = Color(0xFF168AAD))) {
        when {
            guide -> LicensedGuideV2(dark) { guide = false }
            post -> PostRoscV2(dark, { post = false }, {
                post = false
                val s = flow.selection!!
                val p = ClinicalPathwaysFa.get(s.module, s.patientGroup!!, s.rescuerType!!, s.canProvideRescueBreaths, s.trainedRescuerCount)
                flow = flow.copy(page = SessionPage.ACTIVE_PATH, activeStepIndex = p.steps.indexOfFirst { it.id == "compressions" }.coerceAtLeast(0))
            })
            flow.page == SessionPage.HOME -> HomeV2(dark, { g ->
                flow = SessionFlowReducer.reduce(SessionFlowState(), SessionFlowAction.ChooseModule(AppModule.BASIC_LIFE_SUPPORT))
                flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ChoosePatient(g))
            }, { guide = true }, { settings = true })
            flow.page == SessionPage.SELECTION || flow.page == SessionPage.REVIEW_GATE -> RescuerV2(flow.selection?.patientGroup ?: PatientGroup.ADULT, dark,
                { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.Back) },
                { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ChooseRescuer(it)) })
            else -> ActiveV2(flow.selection!!, flow.activeStepIndex, dark,
                { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.PreviousStep) },
                {
                    val s = flow.selection!!
                    val p = ClinicalPathwaysFa.get(s.module, s.patientGroup!!, s.rescuerType!!, s.canProvideRescueBreaths, s.trainedRescuerCount)
                    if (flow.activeStepIndex < p.steps.indexOfLast { it.id != "rosc" }) flow = SessionFlowReducer.reduce(flow, SessionFlowAction.NextStep)
                },
                { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.Reset) },
                { post = true })
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

@Composable
private fun HomeV2(dark: Boolean, choose: (PatientGroup) -> Unit, guide: () -> Unit, settings: () -> Unit) {
    val ctx = LocalContext.current
    val audio = remember(ctx) { OfflinePersianAudioPlayer(ctx) }
    val fg = if (dark) Color.White else Color(0xFF10264A)
    val bg = if (dark) listOf(Color(0xFF031943), Color(0xFF062B68)) else listOf(Color(0xFFEAF3FF), Color(0xFFF7FAFF))
    DisposableEffect(audio) { onDispose { audio.release() } }
    LaunchedEffect(Unit) { audio.play("سلام، من رابط صوتی احیا هستم. آرامش خود را حفظ کنید. در شرایط اضطراری با ۱۱۵ تماس بگیرید.") }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(bg)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text("RescueGuide", color = fg, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("دستیار صوتی احیای اضطراری • ایران ۱۱۵", color = fg, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("گروه سنی بیمار را انتخاب کنید", color = Color(0xFF0756C8), fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("پس از انتخاب نوع امدادگر، راهنمای صوتی خودکار آغاز می‌شود.", color = Color(0xFF10264A), textAlign = TextAlign.Center)
            }
        }
        PatientTileV2("بزرگسال", "راهنمای احیای پایه بزرگسال", Color(0xFF167BFF)) { choose(PatientGroup.ADULT) }
        PatientTileV2("کودک", "از یک سال تا شروع بلوغ", Color(0xFF159B59)) { choose(PatientGroup.CHILD) }
        PatientTileV2("شیرخوار", "زیر یک سال", Color(0xFF0097A7)) { choose(PatientGroup.INFANT) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = guide, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.MenuBook, null); Text("راهنما") }
            OutlinedButton(onClick = settings, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Settings, null); Text("تنظیمات") }
        }
        Button(onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", EmergencyDirectory.iran.emergencyNumber, null))) } }, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF2036))) {
            Icon(Icons.Filled.Call, null); Text("تماس با اورژانس ۱۱۵", fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        Text("نسخه آزمایشی • راهنمای احیای پایه", color = fg, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun PatientTileV2(title: String, subtitle: String, tint: Color, click: () -> Unit) {
    Card(Modifier.fillMaxWidth().height(86.dp).clickable(onClick = click), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = tint)) {
        Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black); Text(subtitle, color = Color.White, fontSize = 12.sp) }
            Text("›", color = Color.White, fontSize = 32.sp)
        }
    }
}

@Composable
private fun RescuerV2(group: PatientGroup, dark: Boolean, back: () -> Unit, choose: (RescuerType) -> Unit) {
    val fg = if (dark) Color.White else Color(0xFF10264A)
    val bg = if (dark) listOf(Color(0xFF031943), Color(0xFF062B68)) else listOf(Color(0xFFEAF3FF), Color(0xFFF7FAFF))
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(bg)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedButton(onClick = back) { Text("بازگشت") }
        Text("نوع امدادگر را انتخاب کنید", color = fg, fontSize = 23.sp, fontWeight = FontWeight.Black)
        Text("گروه سنی: " + PatientRouting.label(group), color = fg)
        RescuerTileV2("امدادگر آموزش‌دیده", Color(0xFF0877D5), Icons.Filled.MonitorHeart) { choose(RescuerType.TRAINED_RESCUER) }
        RescuerTileV2("فرد عادی", Color(0xFF159B59), Icons.Filled.Favorite) { choose(RescuerType.LAY_RESCUER) }
    }
}

@Composable
private fun RescuerTileV2(title: String, tint: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) {
    Card(Modifier.fillMaxWidth().height(150.dp).clickable(onClick = click), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = tint)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(40.dp))
            Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ActiveV2(s: SessionSelection, stepIndex: Int, dark: Boolean, previous: () -> Unit, next: () -> Unit, exit: () -> Unit, rosc: () -> Unit) {
    val ctx = LocalContext.current
    val audio = remember(ctx) { OfflinePersianAudioPlayer(ctx) }
    val scope = rememberCoroutineScope()
    val path = remember(s) { ClinicalPathwaysFa.get(s.module, s.patientGroup!!, s.rescuerType!!, s.canProvideRescueBreaths, s.trainedRescuerCount) }
    val steps = remember(path) { path.steps.filterNot { it.id == "rosc" } }
    val index = stepIndex.coerceIn(0, steps.lastIndex)
    val step = steps[index]
    val fg = if (dark) Color.White else Color(0xFF10264A)
    val sub = if (dark) Color(0xFFD6E7FF) else Color(0xFF31577D)
    val lifecycle = LocalLifecycleOwner.current
    var autoAdvance by remember { mutableStateOf<Job?>(null) }
    fun speakCurrentStep() {
        audio.stop()
        autoAdvance?.cancel()
        val started = audio.play(step.instruction) {
            autoAdvance?.cancel()
            autoAdvance = scope.launch { delay(2000); if (index < steps.lastIndex) next() }
        }
        if (!started) autoAdvance = scope.launch { delay(9000); if (index < steps.lastIndex) next() }
    }
    LaunchedEffect(step.id) { speakCurrentStep() }
    DisposableEffect(audio, lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) audio.stop() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { autoAdvance?.cancel(); lifecycle.lifecycle.removeObserver(observer); audio.release() }
    }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(if (dark) listOf(Color(0xFF031943), Color(0xFF062B68)) else listOf(Color(0xFFEAF3FF), Color(0xFFF7FAFF))))
        .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("مرحله " + (index + 1) + " از " + steps.size, color = fg)
        }
        Text("راهنمای احیای پایه", color = fg, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(step.title, color = Color(0xFF073A91), fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(step.instruction, color = Color(0xFF142C4E), fontSize = 15.sp, lineHeight = 23.sp)
                if (step.id == "ratio" && s.patientGroup != PatientGroup.ADULT) Text("اگر قادر به تنفس کمکی نیستید، فشارهای پیوسته را ادامه دهید.", color = Color(0xFFB3261E), fontWeight = FontWeight.Bold)
                if (step.timeCritical) Text("اقدام فوری — برای تأیید منتظر نمانید.", color = Color(0xFFB3261E), fontWeight = FontWeight.Bold)
            }
        }
        StepVisualV2(s.patientGroup!!, step.id)
        if (index == steps.lastIndex) Text("مراحل صوتی پایان یافت؛ احیا را تا رسیدن کمک ادامه دهید.", color = fg)
        Button(onClick = { autoAdvance?.cancel(); audio.stop(); rosc() }, modifier = Modifier.fillMaxWidth().height(62.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF159B59))) {
            Text("علائم حیاتی برگشت", fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { autoAdvance?.cancel(); audio.stop(); previous() }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("مرحله قبل") }
            Button(onClick = { speakCurrentStep() }, modifier = Modifier.weight(1f)) { Text("تکرار توضیح فعلی") }
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
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3FF)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(data.first, color = Color(0xFF073A91), fontWeight = FontWeight.Bold)
            if (resource != 0) Image(painterResource(resource), data.first, Modifier.fillMaxWidth().height(175.dp).clickable { zoom = resource }, contentScale = ContentScale.Fit)
            else if (id == "emergency") Icon(Icons.Filled.Call, contentDescription = data.first, tint = Color(0xFF0B62C8), modifier = Modifier.size(88.dp).padding(12.dp))
            else if (id == "response") Icon(Icons.Filled.Person, contentDescription = data.first, tint = Color(0xFF0B62C8), modifier = Modifier.size(88.dp).padding(12.dp))
            else if (id == "scene") Icon(Icons.Filled.Info, contentDescription = data.first, tint = Color(0xFF0B62C8), modifier = Modifier.size(88.dp).padding(12.dp))
            else Text("تصویر آموزشی در مرحله دریافت دارایی‌ها قرار می‌گیرد.", color = Color(0xFF31577D), modifier = Modifier.padding(20.dp))
            Text(data.third, color = Color(0xFF17385F), fontSize = 12.sp, textAlign = TextAlign.Center)
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
private fun LicensedGuideV2(dark: Boolean, back: () -> Unit) {
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
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(bg)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { OutlinedButton(onClick = back) { Text("بازگشت") }; Spacer(Modifier.weight(1f)); Text("راهنمای تصویری", color = fg, fontWeight = FontWeight.Black) }
        items.forEach { item ->
            val r = remember(item.second) { ctx.resources.getIdentifier(item.second, "drawable", ctx.packageName) }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(item.first, color = Color(0xFF073A91), fontWeight = FontWeight.Bold)
                    if (r != 0) Image(painterResource(r), item.first, Modifier.fillMaxWidth().height(180.dp).clickable { zoom = r }, contentScale = ContentScale.Fit)
                    else Text("تصویر در مرحله دریافت دارایی‌ها قرار می‌گیرد.", color = Color(0xFF31577D), modifier = Modifier.padding(18.dp))
                    Text(item.third, color = Color(0xFF17385F), fontSize = 12.sp)
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
private fun PostRoscV2(dark: Boolean, back: () -> Unit, restart: () -> Unit) {
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
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(bg)).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = back) { Text("بازگشت به راهنمای احیا") }
        Text("مراقبت پس از بازگشت علائم حیاتی", color = fg, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text("این صفحه راهنمای صوتی ندارد؛ مراحل را بخوانید و بیمار را پیوسته پایش کنید.", color = fg, fontSize = 12.sp)
        steps.forEachIndexed { i, item ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text((i + 1).toString(), color = Color.White, modifier = Modifier.background(Color(0xFF0B62C8), RoundedCornerShape(22.dp)).padding(10.dp), fontWeight = FontWeight.Black)
                    Column(Modifier.weight(1f)) {
                        Text(item.first, color = Color(0xFF073A91), fontWeight = FontWeight.Bold)
                        Text(item.second, color = Color(0xFF17385F), fontSize = 12.sp)
                        if (i == 2 && recovery != 0) Image(painterResource(recovery), "وضعیت ریکاوری", Modifier.fillMaxWidth().height(130.dp), contentScale = ContentScale.Fit)
                    }
                }
            }
        }
        Button(onClick = restart, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("وضعیت بدتر شد — شروع مجدد احیا") }
    }
}
