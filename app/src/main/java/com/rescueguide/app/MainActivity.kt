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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.rescueguide.app.platform.AndroidPersianSpeechOutput
import com.rescueguide.app.platform.AndroidSpeechInput
import com.rescueguide.app.voice.PersianVoiceCommandParser
import com.rescueguide.app.voice.VoiceIntent
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
    MaterialTheme {
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
                    onContinue = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ContinueToReviewGate) }
                )
                SessionPage.REVIEW_GATE -> ReviewGateScreen(
                    selection = flow.selection!!,
                    onBack = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.Back) },
                    onHome = { flow = SessionFlowReducer.reduce(flow, SessionFlowAction.Reset) }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(onSelectModule: (AppModule) -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("راهنمای احیا", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("دستیار فارسی راهنمای احیای پایه و مدیریت خفگی", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
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
        VoiceHardwarePanel()
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("نسخه آزمایشی", fontWeight = FontWeight.Bold)
                Text("محتوای بالینی هنوز در انتظار بازبینی و تأیید تخصصی است؛ انتخاب‌ها صرفاً نمایشی‌اند و راهنمای اجرایی فعال نمی‌شود.")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("نسخه 0.28.0-dev", style = MaterialTheme.typography.labelMedium)
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
    onContinue: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedButton(onClick = onBack) { Text("بازگشت") }
        Text(if (selection.module == AppModule.BASIC_LIFE_SUPPORT) "انتخاب مسیر احیای پایه" else "انتخاب مسیر مدیریت خفگی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("۱. گروه سنی فرد را انتخاب کنید", style = MaterialTheme.typography.titleMedium)
        PatientGroup.values().forEach { group ->
            SelectionButton(PatientRouting.label(group), selection.patientGroup == group) { onPatient(group) }
        }
        Text("۲. نوع امدادگر را انتخاب کنید", style = MaterialTheme.typography.titleMedium)
        SelectionButton("امدادگر عادی", selection.rescuerType == RescuerType.LAY_RESCUER) { onRescuer(RescuerType.LAY_RESCUER) }
        SelectionButton("امدادگر آموزش‌دیده / کادر درمان", selection.rescuerType == RescuerType.TRAINED_RESCUER) { onRescuer(RescuerType.TRAINED_RESCUER) }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("راهنمای انتخاب", fontWeight = FontWeight.Bold)
                Text("شیرخوار: زیر یک سال. کودک: از یک سال تا شروع بلوغ. با نشانه‌های بلوغ، مسیر بزرگسال انتخاب می‌شود. احیای نوزاد هنگام تولد در محدوده این نسخه نیست.")
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("مسیر اجرایی هنوز غیرفعال است", fontWeight = FontWeight.Bold)
                Text("این صفحه فقط انتخاب‌ها را ثبت می‌کند. تا زمان بازبینی و تأیید محتوای پزشکی، دستورهای احیا یا رفع خفگی پخش نمی‌شوند.")
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
    onHome: () -> Unit
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
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("راهنمای پزشکی هنوز فعال نیست", fontWeight = FontWeight.Bold)
                Text("این مرحله فقط انتخاب‌های شما را بازبینی می‌کند. هیچ دستور بالینی، شمارش احیا یا فایل صوتی اجرایی در این نسخه پخش نمی‌شود؛ فعال‌سازی منوط به بازبینی و تأیید تخصصی محتوای پزشکی است.")
            }
        }
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("بازگشت به صفحه اصلی")
        }
    }
}


@Composable
private fun VoiceHardwarePanel() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val speechInput = remember(context) { AndroidSpeechInput(context) }
    val speechOutput = remember(context) { AndroidPersianSpeechOutput(context) }
    var micState by remember { mutableStateOf(AndroidSpeechInput.State.IDLE) }
    var ttsState by remember { mutableStateOf(AndroidPersianSpeechOutput.State.INITIALIZING) }
    var recognized by remember { mutableStateOf("هنوز فرمانی دریافت نشده است") }
    var parsed by remember { mutableStateOf(VoiceIntent.UNKNOWN) }

    DisposableEffect(speechInput, speechOutput, lifecycleOwner) {
        speechOutput.observeReady { ttsState = it }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                speechInput.stop()
                speechOutput.stop()
                micState = AndroidSpeechInput.State.IDLE
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speechInput.stop()
            speechOutput.shutdown()
        }
    }

    val startListening: () -> Unit = {
        speechInput.start(
            onResult = { phrase ->
                recognized = phrase
                parsed = PersianVoiceCommandParser.parse(phrase)
            },
            onState = { micState = it }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else micState = AndroidSpeechInput.State.ERROR
    }
    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("آزمایش زیرساخت صوتی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("میکروفون فقط پس از فشردن دکمه و اجازه شما فعال می‌شود. نتیجه گفتار فقط برای آزمایش فرمان‌ها نمایش داده می‌شود و هیچ اقدام پزشکی انجام نمی‌دهد.")
            Text("وضعیت تشخیص گفتار: ${when (micState) {
                AndroidSpeechInput.State.IDLE -> "آماده"
                AndroidSpeechInput.State.LISTENING -> "در حال شنیدن"
                AndroidSpeechInput.State.PROCESSING -> "در حال پردازش"
                AndroidSpeechInput.State.UNAVAILABLE -> "سرویس تشخیص گفتار در دستگاه موجود نیست"
                AndroidSpeechInput.State.NO_MATCH -> "گفتار واضحی تشخیص داده نشد؛ دوباره تلاش کنید"
                AndroidSpeechInput.State.ERROR -> "خطا یا مجوز رد شده است"
            }}")
            Button(onClick = {
                if (hasPermission) startListening() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("شروع آزمایش فرمان صوتی") }
            OutlinedButton(onClick = { speechInput.stop(); micState = AndroidSpeechInput.State.IDLE }, modifier = Modifier.fillMaxWidth()) { Text("توقف میکروفون") }
            Text("گفتار تشخیص‌داده‌شده: $recognized")
            Text("برداشت آزمایشی: ${when (parsed) {
                VoiceIntent.CONFIRM -> "تأیید"
                VoiceIntent.DECLINE -> "رد"
                VoiceIntent.REPEAT -> "تکرار"
                VoiceIntent.BACK -> "بازگشت"
                VoiceIntent.UNKNOWN -> "نامشخص / بدون اقدام"
            }}")
            Text("وضعیت صدای فارسی: ${when (ttsState) {
                AndroidPersianSpeechOutput.State.INITIALIZING -> "در حال آماده‌سازی"
                AndroidPersianSpeechOutput.State.READY -> "آماده"
                AndroidPersianSpeechOutput.State.LANGUAGE_DATA_MISSING -> "داده صدای فارسی نصب نیست"
                AndroidPersianSpeechOutput.State.LANGUAGE_UNSUPPORTED -> "صدای فارسی پشتیبانی نمی‌شود"
                AndroidPersianSpeechOutput.State.INIT_FAILED -> "راه‌اندازی موتور گفتار ناموفق بود"
            }}")
            Button(
                onClick = { speechOutput.speakTestPhrase("این فقط آزمایش صدای فارسی برنامه راهنمای احیا است.") },
                enabled = ttsState == AndroidPersianSpeechOutput.State.READY,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("پخش آزمایشی صدای فارسی") }
            OutlinedButton(
                onClick = { speechOutput.repeatLastTestPhrase() },
                enabled = ttsState == AndroidPersianSpeechOutput.State.READY,
                modifier = Modifier.fillMaxWidth()
            ) { Text("تکرار آخرین صدای آزمایشی") }
            OutlinedButton(
                onClick = { speechOutput.stop() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("توقف پخش صدا") }
            Text("توجه: تشخیص گفتار ممکن است به سرویس نصب‌شده روی گوشی و اتصال شبکه وابسته باشد. هیچ صدای ضبط‌شده‌ای در برنامه ذخیره نمی‌شود.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
