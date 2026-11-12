package com.rescueguide.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.rescueguide.app.platform.AndroidSpeechInput
import com.rescueguide.app.platform.OfflinePersianAudioPlayer
import com.rescueguide.app.platform.CprMetronome
import com.rescueguide.app.voice.PersianVoiceCommandParser
import com.rescueguide.app.voice.VoiceIntent
import com.rescueguide.app.clinical.ClinicalPathwaysFa
import com.rescueguide.app.domain.AppModule
import com.rescueguide.app.domain.EmergencyDirectory
import com.rescueguide.app.domain.PatientGroup
import com.rescueguide.app.domain.RescuerType
import com.rescueguide.app.domain.SessionSelection
import com.rescueguide.app.domain.PatientRouting
import com.rescueguide.app.domain.SessionPage
import com.rescueguide.app.domain.SessionFlowState
import com.rescueguide.app.domain.SessionFlowAction
import com.rescueguide.app.domain.SessionFlowReducer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RescueGuideRoot() }
    }
}

@Composable
private fun RescueGuideRoot() {
    var flow by remember { mutableStateOf(SessionFlowState()) }
    val medicalColors = lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF0B4F71),
        onPrimary = androidx.compose.ui.graphics.Color.White,
        secondary = androidx.compose.ui.graphics.Color(0xFF168AAD),
        tertiary = androidx.compose.ui.graphics.Color(0xFF2A9D8F),
        background = androidx.compose.ui.graphics.Color(0xFFF3F7FA),
        surface = androidx.compose.ui.graphics.Color.White,
        error = androidx.compose.ui.graphics.Color(0xFFB3261E)
    )
    MaterialTheme(colorScheme = medicalColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (flow.page) {
                SessionPage.HOME -> HomeScreen(onSelectModule = {
                    flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ChooseModule(it))
                })
                SessionPage.SELECTION -> SelectionScreen(
                    selection = flow.selection!!,
                    onBack = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.Back) },
                    onPatient = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ChoosePatient(it)) },
                    onRescuer = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ChooseRescuer(it)) },
                    onBreaths = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ChooseRescueBreaths(it)) },
                    onTrainedCount = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ChooseTrainedRescuerCount(it)) },
                    onContinue = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ContinueToReviewGate) }
                )
                SessionPage.REVIEW_GATE -> ReviewGateScreen(
                    selection = flow.selection!!,
                    onBack = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.Back) },
                    onHome = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.Reset) },
                    onStart = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.StartPath) }
                )
                SessionPage.ACTIVE_PATH -> ActivePathScreen(
                    selection = flow.selection!!,
                    stepIndex = flow.activeStepIndex,
                    onBack = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.PreviousStep) },
                    onNext = {
                        val selected = flow.selection!!
                        val last = ClinicalPathwaysFa.get(selected.module, selected.patientGroup!!, selected.rescuerType!!, selected.canProvideRescueBreaths, selected.trainedRescuerCount).steps.lastIndex
                        if (flow.activeStepIndex < last) flow = SessionFlowReducer.reduce(flow, SessionFlowAction.NextStep)
                    },
                    onExit = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.Reset) },
                    onStartBls = {
                        val selected = flow.selection!!
                        val blsSelection = selected.copy(module = AppModule.BASIC_LIFE_SUPPORT)
                        val blsPath = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, selected.patientGroup!!, selected.rescuerType!!, selected.canProvideRescueBreaths, selected.trainedRescuerCount)
                        val firstCompression = blsPath.steps.indexOfFirst { it.id == "compressions" }.coerceAtLeast(0)
                        flow = flow.copy(selection = blsSelection, activeStepIndex = firstCompression)
                    },
                    onRosc = {
                        val selected = flow.selection!!
                        val currentPath = ClinicalPathwaysFa.get(selected.module, selected.patientGroup!!, selected.rescuerType!!, selected.canProvideRescueBreaths, selected.trainedRescuerCount)
                        val roscIndex = currentPath.steps.indexOfFirst { it.id == "rosc" }
                        if (roscIndex >= 0) flow = flow.copy(activeStepIndex = roscIndex)
                    },
                    onRestartCpr = {
                        val selected = flow.selection!!
                        val blsSelection = selected.copy(module = AppModule.BASIC_LIFE_SUPPORT)
                        val blsPath = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, selected.patientGroup!!, selected.rescuerType!!, selected.canProvideRescueBreaths, selected.trainedRescuerCount)
                        val firstCompression = blsPath.steps.indexOfFirst { it.id == "compressions" }.coerceAtLeast(0)
                        flow = flow.copy(selection = blsSelection, activeStepIndex = firstCompression)
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(onSelectModule: (AppModule) -> Unit) {
    val context = LocalContext.current
    val welcomeAudio = remember(context) { OfflinePersianAudioPlayer(context) }
    val homeLifecycle = LocalLifecycleOwner.current
    LaunchedEffect(Unit) { welcomeAudio.play("سلام، من رابط صوتی احیا هستم. آرامش خود را حفظ کنید. در شرایط اضطراری با ۱۱۵ تماس بگیرید.") }
    DisposableEffect(welcomeAudio, homeLifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) welcomeAudio.stop() }
        homeLifecycle.lifecycle.addObserver(observer)
        onDispose { homeLifecycle.lifecycle.removeObserver(observer); welcomeAudio.release() }
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(painter = painterResource(R.drawable.ic_rescueguide), contentDescription = "نشان قلب و نوار قلب RescueGuide", modifier = Modifier.size(76.dp))
        Text("راهنمای احیا", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("دستیار فارسی راهنمای احیای پایه و مدیریت خفگی", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Text("راهنمای صوتی فارسی آفلاین — بدون وابستگی به صدای فارسی گوشی", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("در وضعیت اضطراری", fontWeight = FontWeight.Bold)
                Text("برای دریافت کمک فوری با اورژانس تماس بگیرید. این برنامه جایگزین اپراتور اورژانس یا آموزش عملی نیست.")
                Button(
                    modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = "باز کردن شماره‌گیر اورژانس ۱۱۵" },
                    onClick = {
                        val number = EmergencyDirectory.iran.emergencyNumber
                        if (EmergencyDirectory.isDialable(number)) runCatching {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
                        }
                    }
                ) { Text("تماس با اورژانس ۱۱۵") }
            }
        }
        ModuleCard("احیای پایه (BLS)", "انتخاب گروه سنی و نوع امدادگر", onClick = { onSelectModule(AppModule.BASIC_LIFE_SUPPORT) })
        ModuleCard("مدیریت خفگی", "انتخاب گروه سنی و نوع امدادگر", onClick = { onSelectModule(AppModule.CHOKING) })
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("راهنمای صوتی و کنترل", fontWeight = FontWeight.Bold)
                Text("همه دستورها با فایل صوتی فارسی آفلاین پخش می‌شوند. فرمان صوتی اختیاری است و دکمه‌های لمسی همیشه در دسترس‌اند.")
            }
        }
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("راهنمای ایمنی", fontWeight = FontWeight.Bold)
                Text("در هر وضعیت تهدیدکننده حیات، فوراً با ۱۱۵ تماس بگیرید و از راهنمای اپراتور پیروی کنید. برنامه جایگزین آموزش عملی یا خدمات اورژانس نیست.")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("نسخه 0.30.0-dev", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ModuleCard(title: String, description: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = onClick) { Text("انتخاب") }
        }
    }
}

@Composable
private fun SelectionScreen(
    selection: SessionSelection,
    onBack: () -> Unit,
    onPatient: (PatientGroup) -> Unit,
    onRescuer: (RescuerType) -> Unit,
    onBreaths: (Boolean) -> Unit,
    onTrainedCount: (Int) -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val speechInput = remember(context) { AndroidSpeechInput(context) }
    var ageVoiceStatus by remember { mutableStateOf("می‌توانید بگویید: شیرخوار، کودک یا بزرگسال؛ یا گروه سنی را دستی انتخاب کنید.") }
    val listenAge: () -> Unit = {
        speechInput.start(
            onResult = { phrase ->
                val group = PersianVoiceCommandParser.parsePatientGroup(phrase)
                if (group == null) ageVoiceStatus = "سن یا وضعیت بلوغ مبهم است؛ گروه سنی را دستی انتخاب کنید."
                else { onPatient(group); ageVoiceStatus = "گروه سنی انتخاب شد: ${PatientRouting.label(group)}" }
            },
            onState = { state -> ageVoiceStatus = when (state) {
                AndroidSpeechInput.State.IDLE -> ageVoiceStatus
                AndroidSpeechInput.State.LISTENING -> "در حال شنیدن گروه سنی…"
                AndroidSpeechInput.State.PROCESSING -> "در حال تشخیص گفتار…"
                AndroidSpeechInput.State.UNAVAILABLE -> "تشخیص گفتار در این گوشی در دسترس نیست؛ انتخاب دستی را انجام دهید."
                AndroidSpeechInput.State.NO_MATCH -> "گفتار تشخیص داده نشد؛ انتخاب دستی را انجام دهید."
                AndroidSpeechInput.State.ERROR -> "خطای میکروفون؛ انتخاب دستی را انجام دهید."
            } }
        )
    }
    val agePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listenAge() else ageVoiceStatus = "مجوز میکروفون داده نشد؛ انتخاب دستی در دسترس است."
    }
    DisposableEffect(speechInput) { onDispose { speechInput.stop() } }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("بازگشت") }
        Text(if (selection.module == AppModule.BASIC_LIFE_SUPPORT) "انتخاب مسیر احیای پایه" else "انتخاب مسیر مدیریت خفگی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("۱. گروه سنی فرد را انتخاب کنید", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listenAge()
            else agePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }, modifier = Modifier.fillMaxWidth()) { Text("تعیین گروه سنی با گفتار") }
        Text(ageVoiceStatus, style = MaterialTheme.typography.bodySmall)
        PatientGroup.values().forEach { group ->
            SelectionButton(PatientRouting.label(group), selection.patientGroup == group) { onPatient(group) }
        }
        Text("۲. نوع امدادگر را انتخاب کنید", style = MaterialTheme.typography.titleMedium)
        SelectionButton("امدادگر عادی", selection.rescuerType == RescuerType.LAY_RESCUER) { onRescuer(RescuerType.LAY_RESCUER) }
        SelectionButton("امدادگر آموزش‌دیده / کادر درمان", selection.rescuerType == RescuerType.TRAINED_RESCUER) { onRescuer(RescuerType.TRAINED_RESCUER) }
        Text(if (selection.module == AppModule.BASIC_LIFE_SUPPORT) "۳. آیا می‌توانید تنفس کمکی بدهید؟" else "۳. اگر فرد بیهوش شد، آیا می‌توانید تنفس کمکی بدهید؟", style = MaterialTheme.typography.titleMedium)
        SelectionButton("بله، می‌توانم تنفس کمکی بدهم", selection.canProvideRescueBreaths == true) { onBreaths(true) }
        SelectionButton("خیر، فقط فشار قفسه سینه", selection.canProvideRescueBreaths == false) { onBreaths(false) }
        if (selection.rescuerType == RescuerType.TRAINED_RESCUER) {
            Text("تعداد امدادگران آموزش‌دیده حاضر", style = MaterialTheme.typography.titleMedium)
            SelectionButton("یک امدادگر آموزش‌دیده", selection.trainedRescuerCount == 1) { onTrainedCount(1) }
            SelectionButton("دو امدادگر آموزش‌دیده", selection.trainedRescuerCount == 2) { onTrainedCount(2) }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("راهنمای انتخاب", fontWeight = FontWeight.Bold)
                Text("شیرخوار: زیر یک سال. کودک: از یک سال تا شروع بلوغ. با نشانه‌های بلوغ، مسیر بزرگسال انتخاب می‌شود. احیای نوزاد هنگام تولد در محدوده این نسخه نیست.")
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("انتخاب مسیر مناسب", fontWeight = FontWeight.Bold)
                Text("اگر درباره گروه سنی یا وضعیت بلوغ مطمئن نیستید، از اپراتور ۱۱۵ راهنمایی بگیرید.")
                Text("انتخاب فعلی: ${selection.patientGroup?.let(PatientRouting::label) ?: "تعیین نشده"} — ${when (selection.rescuerType) { RescuerType.LAY_RESCUER -> "امدادگر عادی"; RescuerType.TRAINED_RESCUER -> "امدادگر آموزش‌دیده"; null -> "نوع امدادگر تعیین نشده" }}")
            }
        }
        Button(
            onClick = onContinue,
            enabled = selection.isComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("ادامه و بررسی انتخاب‌ها") }
    }
}

@Composable
private fun SelectionButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(label) }
    else OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(label) }
}

@Composable
private fun ReviewGateScreen(
    selection: SessionSelection,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("بازگشت و اصلاح انتخاب‌ها") }
        Text("بررسی انتخاب‌ها", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("بخش: ${if (selection.module == AppModule.BASIC_LIFE_SUPPORT) "احیای پایه (BLS)" else "مدیریت خفگی"}")
                Text("گروه سنی: ${selection.patientGroup?.let(PatientRouting::label) ?: "تعیین نشده"}")
                Text("امدادگر: ${if (selection.rescuerType == RescuerType.LAY_RESCUER) "امدادگر عادی" else "امدادگر آموزش‌دیده / کادر درمان"}")
                Text("تنفس کمکی: ${if (selection.canProvideRescueBreaths == true) "قادر به انجام" else "فقط فشار قفسه سینه"}")
                if (selection.rescuerType == RescuerType.TRAINED_RESCUER) Text("امدادگران آموزش‌دیده: ${selection.trainedRescuerCount}")
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("یادآوری مهم", fontWeight = FontWeight.Bold)
                Text("با تأیید شما، مسیر آموزشی انتخاب‌شده نمایش داده می‌شود. در شرایط واقعی فوراً با ۱۱۵ تماس بگیرید و از اپراتور پیروی کنید.")
            }
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("شروع راهنمای مرحله‌به‌مرحله")
        }
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("بازگشت به صفحه اصلی") }
    }
}


@Composable
private fun ActivePathScreen(
    selection: SessionSelection,
    stepIndex: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    onStartBls: () -> Unit,
    onRosc: () -> Unit,
    onRestartCpr: () -> Unit
) {
    val context = LocalContext.current
    val audioPlayer = remember(context) { OfflinePersianAudioPlayer(context) }
    val scope = rememberCoroutineScope()
    val metronome = remember(context) { CprMetronome(scope) }
    var metronomeBpm by remember { mutableStateOf(110f) }
    var metronomeRunning by remember { mutableStateOf(false) }
    val speechInput = remember(context) { AndroidSpeechInput(context) }
    var voiceStatus by remember { mutableStateOf("فرمان صوتی اختیاری است؛ دکمه‌های لمسی همیشه فعال‌اند.") }
    val pathway = remember(selection) {
        ClinicalPathwaysFa.get(selection.module, selection.patientGroup!!, selection.rescuerType!!, selection.canProvideRescueBreaths, selection.trainedRescuerCount)
    }
    val safeIndex = stepIndex.coerceIn(0, pathway.steps.lastIndex)
    val step = pathway.steps[safeIndex]
    val latestInstruction by rememberUpdatedState(step.instruction)
    val reportRosc: () -> Unit = { metronome.stop(); metronomeRunning = false; onRosc() }
    val listenOnce: () -> Unit = {
        speechInput.start(
            onResult = { phrase ->
                when (PersianVoiceCommandParser.parse(phrase)) {
                    VoiceIntent.CONFIRM -> if (safeIndex < pathway.steps.lastIndex) { voiceStatus = "فرمان تأیید دریافت شد"; onNext() } else { voiceStatus = "این آخرین مرحله مسیر است." }
                    VoiceIntent.BACK -> { voiceStatus = "فرمان بازگشت دریافت شد"; if (safeIndex > 0) onBack() }
                    VoiceIntent.REPEAT -> { voiceStatus = "دستور تکرار شد"; audioPlayer.play(latestInstruction) }
                    VoiceIntent.ROSC -> { voiceStatus = "بازگشت تنفس طبیعی گزارش شد"; reportRosc() }
                    VoiceIntent.DECLINE, VoiceIntent.UNKNOWN -> voiceStatus = "فرمان واضح نبود؛ از دکمه‌های لمسی استفاده کنید."
                }
            },
            onState = { state -> voiceStatus = when (state) {
                AndroidSpeechInput.State.IDLE -> voiceStatus
                AndroidSpeechInput.State.LISTENING -> "در حال شنیدن فرمان فارسی…"
                AndroidSpeechInput.State.PROCESSING -> "در حال تشخیص گفتار…"
                AndroidSpeechInput.State.UNAVAILABLE -> "سرویس تشخیص گفتار در گوشی موجود نیست."
                AndroidSpeechInput.State.NO_MATCH -> "گفتار تشخیص داده نشد؛ از دکمه‌های لمسی استفاده کنید."
                AndroidSpeechInput.State.ERROR -> "تشخیص گفتار ناموفق بود؛ از دکمه‌های لمسی استفاده کنید."
            } }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listenOnce() else voiceStatus = "مجوز میکروفون داده نشد؛ کنترل لمسی در دسترس است."
    }
    val activeLifecycle = LocalLifecycleOwner.current
    LaunchedEffect(step.instruction) { speechInput.stop(); audioPlayer.play(step.instruction) }
    DisposableEffect(audioPlayer, speechInput, metronome, activeLifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                speechInput.stop()
                audioPlayer.stop()
                metronome.stop()
                metronomeRunning = false
            }
        }
        activeLifecycle.lifecycle.addObserver(observer)
        onDispose {
            activeLifecycle.lifecycle.removeObserver(observer)
            speechInput.stop(); audioPlayer.release(); metronome.release()
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = onExit) { Text("پایان راهنما") }
            Text("مرحله ${safeIndex + 1} از ${pathway.steps.size}", style = MaterialTheme.typography.labelLarge)
        }
        Text(if (selection.module == AppModule.BASIC_LIFE_SUPPORT) "راهنمای احیای پایه" else "راهنمای رفع خفگی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(step.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(step.instruction, style = MaterialTheme.typography.titleMedium)
                if (step.timeCritical) Text("اقدام فوری — برای تأیید صوتی منتظر نمانید.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
        Text("گروه سنی: ${PatientRouting.label(selection.patientGroup!!)}")
        Text("نوع امدادگر: ${if (selection.rescuerType == RescuerType.LAY_RESCUER) "عادی" else "آموزش‌دیده"}")
        OutlinedButton(onClick = { audioPlayer.play(step.instruction) }, modifier = Modifier.fillMaxWidth()) { Text("پخش دوباره راهنمای صوتی") }
        if (selection.module == AppModule.BASIC_LIFE_SUPPORT) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مترونوم فشار قفسه سینه", fontWeight = FontWeight.Bold)
                    Text("سرعت: ${metronomeBpm.toInt()} فشار در دقیقه")
                    Slider(value = metronomeBpm, onValueChange = { metronomeBpm = it }, valueRange = 100f..120f, steps = 19)
                    Button(onClick = {
                        if (metronomeRunning) { metronome.stop(); metronomeRunning = false }
                        else { metronomeRunning = metronome.start(metronomeBpm.toInt()) }
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (metronomeRunning) "توقف مترونوم" else "شروع مترونوم") }
                    Text("مترونوم فقط ریتم را یادآوری می‌کند و عمق یا کیفیت فشار را اندازه‌گیری نمی‌کند.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        OutlinedButton(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listenOnce()
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }, modifier = Modifier.fillMaxWidth()) { Text("گفتن فرمان: بله، تکرار، بازگشت یا تنفس طبیعی برگشت") }
        Text(voiceStatus, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, enabled = safeIndex > 0, modifier = Modifier.weight(1f)) { Text("مرحله قبل") }
            Button(onClick = onNext, enabled = safeIndex < pathway.steps.lastIndex, modifier = Modifier.weight(1f)) { Text("مرحله بعد") }
        }
        if (safeIndex == pathway.steps.lastIndex) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Text("به پایان مسیر آموزشی رسیدید. وضعیت فرد را پیوسته ارزیابی کنید و تا رسیدن کمک تخصصی از راهنمای اپراتور ۱۱۵ پیروی کنید.", Modifier.padding(16.dp))
            }
        }
        if (selection.module == AppModule.CHOKING && step.id == "unconscious") {
            Button(onClick = { metronome.stop(); metronomeRunning = false; onStartBls() }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("ورود مستقیم به مراحل CPR") }
        }
        OutlinedButton(onClick = reportRosc, modifier = Modifier.fillMaxWidth()) { Text("گزارش بازگشت پاسخ‌دهی و تنفس طبیعی") }
        if (step.id == "rosc") {
            Button(onClick = { metronome.stop(); metronomeRunning = false; onRestartCpr() }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("وضعیت دوباره بدتر شد — شروع CPR") }
        }
        Button(onClick = {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", EmergencyDirectory.iran.emergencyNumber, null)))
        }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("تماس با اورژانس ۱۱۵") }
        Text("این راهنما ابزار کمکی آموزشی است و جایگزین تماس با اورژانس، دستور اپراتور یا آموزش عملی معتبر نیست.", style = MaterialTheme.typography.bodySmall)
    }
}

