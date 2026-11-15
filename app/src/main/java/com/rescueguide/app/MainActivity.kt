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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
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
        setContent { RescueGuideRootV2() }
    }
}

@Composable
private fun RescueGuideRoot() {
    var flow by remember { mutableStateOf(SessionFlowState()) }
    var showImageGuide by remember { mutableStateOf(false) }
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
            if (showImageGuide) {
                ImageGuideScreen(onBack = { showImageGuide = false })
            } else when (flow.page) {
                SessionPage.HOME -> HomeScreen(onOpenImageGuide = { showImageGuide = true }, onSelectModule = { module, patient ->
                    flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ChooseModule(module))
                    if (patient != null) flow = SessionFlowReducer.reduce(flow, SessionFlowAction.ChoosePatient(patient))
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
                    onRosc = {
                        val selected = flow.selection!!
                        val currentPath = ClinicalPathwaysFa.get(selected.module, selected.patientGroup!!, selected.rescuerType!!, selected.canProvideRescueBreaths, selected.trainedRescuerCount)
                        val roscIndex = currentPath.steps.indexOfFirst { it.id == "rosc" }
                        if (roscIndex >= 0) flow = flow.copy(activeStepIndex = roscIndex)
                    },
                    onRestartCpr = {
                        val selected = flow.selection!!
                        val blsPath = ClinicalPathwaysFa.get(AppModule.BASIC_LIFE_SUPPORT, selected.patientGroup!!, selected.rescuerType!!, selected.canProvideRescueBreaths, selected.trainedRescuerCount)
                        val firstCompression = blsPath.steps.indexOfFirst { it.id == "compressions" }.coerceAtLeast(0)
                        flow = flow.copy(activeStepIndex = firstCompression)
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(onOpenImageGuide: () -> Unit, onSelectModule: (AppModule, PatientGroup?) -> Unit) {
    val context = LocalContext.current
    val welcomeAudio = remember(context) { OfflinePersianAudioPlayer(context) }
    val homeLifecycle = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        welcomeAudio.play("سلام، من رابط صوتی احیا هستم. آرامش خود را حفظ کنید. در شرایط اضطراری با ۱۱۵ تماس بگیرید.")
    }
    DisposableEffect(welcomeAudio, homeLifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) welcomeAudio.stop() }
        homeLifecycle.lifecycle.addObserver(observer)
        onDispose { homeLifecycle.lifecycle.removeObserver(observer); welcomeAudio.release() }
    }

    val navy = Color(0xFF031947)
    val blue = Color(0xFF0758C9)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF031943), Color(0xFF062B68), Color(0xFF031A43))))
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: brand identity and Iran emergency number.
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(66.dp).background(Brush.linearGradient(listOf(Color(0xFFFF3147), Color(0xFFB90024))), RoundedCornerShape(38.dp))
                        .border(2.dp, Color.White.copy(alpha = .9f), RoundedCornerShape(38.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "نشان قلب", tint = Color.White, modifier = Modifier.size(45.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rescue", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Text("Guide", color = Color(0xFFFF354E), fontSize = 27.sp, fontWeight = FontWeight.Black)
                    }
                    Text("دستیار صوتی احیای اضطراری", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(
                    Modifier.background(Color(0xFF061B4B), RoundedCornerShape(24.dp)).border(1.dp, Color(0xFF8BB7FF), RoundedCornerShape(24.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🇮🇷  ایران", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("۱۱۵ :اورژانس", color = Color(0xFFFFE33B), fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
            }

            // Voice assistant hero, using the robot artwork cropped from the approved reference.
            Row(
                Modifier.fillMaxWidth().height(242.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(
                    painterResource(R.drawable.home_robot), contentDescription = "تصویر دستیار صوتی RescueGuide",
                    modifier = Modifier.weight(.88f).fillMaxSize(), contentScale = ContentScale.Fit
                )
                Column(
                    Modifier.weight(1.25f).background(Brush.verticalGradient(listOf(Color(0xFFF7F8FF), Color(0xFFDCE7FF))), RoundedCornerShape(30.dp))
                        .border(2.dp, Color(0xFFB8D0FF), RoundedCornerShape(30.dp)).padding(horizontal = 13.dp, vertical = 17.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("سلام! من دستیار صوتی احیا هستم.", color = Color(0xFF0756C8), fontSize = 17.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Text("در شرایط اضطراری، آرامش خود را حفظ کنید.", color = Color(0xFF101D39), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 21.sp)
                    Text("برای شروع، بگویید «بله» یا دکمه شروع را بزنید.", color = Color(0xFF101D39), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 20.sp)
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFF0756C8), RoundedCornerShape(28.dp)).clickableNoRipple { onSelectModule(AppModule.BASIC_LIFE_SUPPORT, null) }.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(23.dp))
                        Text("  شروع راهنمای احیا", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Decorative voice waveform and microphone badge.
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("〰  ııı  ıııı  ııı  〰", color = Color(0xFF40C8FF), fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.padding(horizontal = 8.dp).size(62.dp).background(Color(0xFF073B91), RoundedCornerShape(40.dp)).border(2.dp, Color(0xFF36BFFF), RoundedCornerShape(40.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Mic, contentDescription = "میکروفون", tint = Color.White, modifier = Modifier.size(35.dp))
                }
                Text("〰  ııı  ıııı  ııı  〰", color = Color(0xFF40C8FF), fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }

            // Main two-column cards, matching the reference's colorful image tiles.
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReferenceHomeTile("احیای بزرگسال", "راهنمای احیای پایه بزرگسال", R.drawable.home_cpr, listOf(Color(0xFF167BFF), Color(0xFF063B9F)), Modifier.weight(1f)) {
                    onSelectModule(AppModule.BASIC_LIFE_SUPPORT, PatientGroup.ADULT)
                }
                ReferenceHomeTile("احیای کودک و شیرخوار", "انتخاب جداگانه گروه سنی و مسیر مناسب", R.drawable.home_cpr, listOf(Color(0xFF19A75A), Color(0xFF08723B)), Modifier.weight(1f)) {
                    // Open age routing so the user can explicitly choose child or infant.
                    onSelectModule(AppModule.BASIC_LIFE_SUPPORT, null)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReferenceHomeTile("آموزش تصویری", "مشاهده مراحل و روش‌های احیا", R.drawable.home_robot, listOf(Color(0xFF9D39D2), Color(0xFF5420A7)), Modifier.weight(1f), onClick = onOpenImageGuide)
                ReferenceHomeTile("اطلاعات مهم", "نکات کلیدی و یادآوری‌ها", R.drawable.home_kit, listOf(Color(0xFFFF9D16), Color(0xFFE65300)), Modifier.weight(1f)) {
                    onOpenImageGuide()
                }
            }

            // Bottom controls and emergency call.
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ReferenceUtilityButton(Icons.Filled.Settings, "تنظیمات", Modifier.weight(1f)) { }
                ReferenceUtilityButton(Icons.Filled.History, "تکرار راهنما", Modifier.weight(1f)) { welcomeAudio.play("سلام، من رابط صوتی احیا هستم. آرامش خود را حفظ کنید. در شرایط اضطراری با ۱۱۵ تماس بگیرید.") }
                ReferenceUtilityButton(Icons.Filled.Info, "راهنمای برنامه", Modifier.weight(1f)) { onOpenImageGuide() }
                Column(
                    Modifier.weight(2.25f).background(Brush.verticalGradient(listOf(Color(0xFFFF2438), Color(0xFFB9001B))), RoundedCornerShape(25.dp))
                        .border(2.dp, Color(0xFFFFA3AA), RoundedCornerShape(25.dp)).clickableNoRipple {
                            val number = EmergencyDirectory.iran.emergencyNumber
                            if (EmergencyDirectory.isDialable(number)) runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))) }
                        }.padding(horizontal = 9.dp, vertical = 11.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.Call, contentDescription = "تماس با اورژانس", tint = Color.White, modifier = Modifier.size(28.dp))
                        Text("تماس با اورژانس", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Text("۱۱۵", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("✚", color = Color(0xFF168BEA), fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("  آموزش درست  =  نجات جان انسان‌ها  ", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text("♡", color = Color(0xFFFF304C), fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
            Text("نسخه آزمایشی • راهنمای احیای پایه", Modifier.fillMaxWidth().padding(bottom = 12.dp), textAlign = TextAlign.Center, color = Color(0xFF9BB9E5), fontSize = 10.sp)
        }
    }
}

@Composable
private fun ReferenceHomeTile(
    title: String, subtitle: String, imageRes: Int, colors: List<Color>, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(190.dp).clickableNoRipple(onClick),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = colors.first()),
        elevation = CardDefaults.cardElevation(7.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Image(painterResource(imageRes), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(colors.first().copy(alpha = .15f), colors.last().copy(alpha = .9f)))))
            Column(Modifier.align(Alignment.TopEnd).padding(12.dp).fillMaxWidth(.82f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.End)
                Text(subtitle, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, lineHeight = 15.sp)
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).size(42.dp).background(colors.first(), RoundedCornerShape(30.dp)).border(1.dp, Color.White.copy(alpha = .65f), RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                Text("❯", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun ReferenceUtilityButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(88.dp).background(Color(0xFF09285E), RoundedCornerShape(20.dp)).border(1.dp, Color(0xFF4777B7), RoundedCornerShape(20.dp)).clickableNoRipple(onClick).padding(horizontal = 4.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(29.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun ImageGuideScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFEAF3FF), Color(0xFFF7F8FF))))
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("بازگشت") }
            Spacer(Modifier.weight(1f))
            Text("آموزش تصویری احیای پایه", color = Color(0xFF082C83), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text("تصاویر شماتیک روش‌های فشار قفسه سینه", color = Color(0xFF24456E), fontSize = 13.sp)
        listOf(
            Triple(PatientGroup.INFANT, "شیرخوار زیر یک سال", "دو شست روی نیمه پایینی جناغ و دست‌ها حلقه‌شده دور قفسه سینه؛ روش دو انگشتی استفاده نشود."),
            Triple(PatientGroup.CHILD, "کودک از یک سال تا بلوغ", "یک یا دو دست متناسب با جثه روی نیمه پایینی جناغ قرار گیرد."),
            Triple(PatientGroup.ADULT, "بزرگسال", "پاشنه یک دست روی نیمه پایینی جناغ و دست دیگر روی آن؛ آرنج‌ها صاف و شانه‌ها بالای دست‌ها.")
        ).forEach { (group, title, caption) ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, color = Color(0xFF073A91), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    Box(Modifier.fillMaxWidth().height(190.dp).background(Color(0xFFEAF3FF), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        CompressionIllustration(group, Modifier.fillMaxSize())
                    }
                    Text(caption, color = Color(0xFF17385F), fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2D8))) {
            Text("توجه: این‌ها طرح‌های شماتیک اولیه هستند، نه تصاویر مرجع بالینی. پیش از انتشار، دقت آن‌ها باید با استاندارد AHA 2025 و بازبینی متخصص تأیید شود.", Modifier.padding(14.dp), color = Color(0xFF694A12), fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun AgeEntryCard(
    title: String, age: String, symbol: String, colors: List<Color>, caption: String,
    modifier: Modifier = Modifier, onSelect: () -> Unit
) {
    Card(
        modifier = modifier.height(246.dp).clickableNoRipple(onSelect),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(colors)).padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text(age, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
            Box(Modifier.fillMaxWidth().weight(1f).background(Brush.verticalGradient(listOf(Color(0xFFE9F3FF), Color(0xFFC9DDF8)))), contentAlignment = Alignment.Center) {
                AgeIllustration(title)
            }
            Column(Modifier.fillMaxWidth().background(Color(0xFFF3F5FF)).padding(horizontal = 5.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(caption, color = Color(0xFF10264A), fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 3)
                Box(Modifier.size(34.dp).background(Brush.verticalGradient(colors), RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                    Text("❯", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HomeFeatureCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, modifier: Modifier, tint: Color, onClick: () -> Unit) {
    Card(modifier = modifier.height(102.dp).clickableNoRipple(onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = tint), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0755A5), modifier = Modifier.size(32.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color(0xFF082C83), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = Color(0xFF183A5C), fontSize = 10.sp, lineHeight = 13.sp)
            }
        }
    }
}

@Composable
private fun BottomNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean) {
    Column(Modifier.background(if (selected) Color(0xFF0756C8) else Color.Transparent, RoundedCornerShape(15.dp)).padding(horizontal = 9.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, contentDescription = label, tint = if (selected) Color.White else Color(0xFF24456E), modifier = Modifier.size(23.dp))
        Text(label, color = if (selected) Color.White else Color(0xFF24456E), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

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
        Text("انتخاب مسیر احیای پایه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
        Text("۳. آیا می‌توانید تنفس کمکی بدهید؟", style = MaterialTheme.typography.titleMedium)
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
                Text("بخش: احیای پایه (BLS)")
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
private fun StepVisualGuide(group: PatientGroup, stepId: String) {
    val isCompression = stepId == "compressions"
    val isBreathing = stepId == "ratio"
    val title = when {
        isCompression && group == PatientGroup.INFANT -> "راهنمای تصویری فشار شیرخوار"
        isCompression && group == PatientGroup.CHILD -> "راهنمای تصویری فشار کودک"
        isCompression -> "راهنمای تصویری فشار بزرگسال"
        isBreathing -> "راهنمای تصویری تنفس کمکی"
        stepId == "aed" -> "راهنمای تصویری دستگاه شوک"
        else -> "راهنمای دیداری این مرحله"
    }
    val visual = when {
        isCompression && group == PatientGroup.INFANT -> "دو شست روی نیمه پایینی جناغ؛ دست‌ها دور قفسه سینه حلقه شوند. روش دو انگشتی استفاده نشود."
        isCompression && group == PatientGroup.CHILD -> "یک یا دو دست متناسب با جثه، روی نیمه پایینی جناغ قرار گیرد."
        isCompression -> "پاشنه یک دست روی نیمه پایینی جناغ و دست دیگر روی آن قرار گیرد."
        isBreathing -> "راه هوایی را باز کنید؛ هر تنفس حدود یک ثانیه و فقط تا بالا آمدن قفسه سینه."
        stepId == "aed" -> "دستگاه را روشن کنید و پدها را مطابق تصویر روی خود پد نصب کنید."
        else -> "به دستور مرحله توجه کنید و در صورت نیاز راهنمای صوتی را تکرار کنید."
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3FF)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB9D2F4))
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color(0xFF073A91), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Box(Modifier.fillMaxWidth().height(168.dp).background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD8E9FF))), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                if (isCompression) CompressionIllustration(group, Modifier.fillMaxSize())
                else if (isBreathing) BreathingIllustration(Modifier.fillMaxSize())
                else if (stepId == "aed") AedIllustration(Modifier.fillMaxSize())
                else AgeIllustration(PatientRouting.label(group), Modifier.fillMaxSize())
            }
            Text(visual, color = Color(0xFF17385F), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 19.sp)
            Text("نمای شماتیک آموزشی؛ پیش از انتشار باید با مرجع بالینی و نظر متخصص تطبیق داده شود.", color = Color(0xFF6A7F96), fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AgeIllustration(title: String, modifier: Modifier = Modifier) {
    val group = when {
        title.contains("شیرخوار") -> PatientGroup.INFANT
        title.contains("کودک") -> PatientGroup.CHILD
        else -> PatientGroup.ADULT
    }
    CompressionIllustration(group, modifier)
}

@Composable
private fun CompressionIllustration(group: PatientGroup, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = minOf(size.width / 210f, size.height / 150f)
        val torsoW = if (group == PatientGroup.INFANT) 92f else 126f
        val torsoH = if (group == PatientGroup.INFANT) 116f else 126f
        // Stylized torso / chest field
        drawRoundRect(Color(0xFFF0B7A2), androidx.compose.ui.geometry.Offset(cx - torsoW*scale/2, cy - torsoH*scale/2), androidx.compose.ui.geometry.Size(torsoW*scale, torsoH*scale), androidx.compose.ui.geometry.CornerRadius(34f*scale))
        drawRoundRect(Color(0xFFFFD6C8), androidx.compose.ui.geometry.Offset(cx - torsoW*scale/2 + 8*scale, cy - torsoH*scale/2 + 8*scale), androidx.compose.ui.geometry.Size((torsoW-16)*scale, (torsoH-16)*scale), androidx.compose.ui.geometry.CornerRadius(28f*scale), style=Stroke(2f*scale))
        // Sternum guide
        drawLine(Color(0xFFB83C4B), androidx.compose.ui.geometry.Offset(cx, cy - 28*scale), androidx.compose.ui.geometry.Offset(cx, cy + 30*scale), strokeWidth=3f*scale, cap=StrokeCap.Round)
        drawCircle(Color(0xFFD32F45), 5f*scale, androidx.compose.ui.geometry.Offset(cx, cy + 8*scale))
        if (group == PatientGroup.INFANT) {
            // Encircling hands and paired thumbs, intentionally not the obsolete two-finger technique
            drawArc(Color(0xFF2869B5), 200f, 140f, false, androidx.compose.ui.geometry.Offset(cx-62*scale, cy-45*scale), androidx.compose.ui.geometry.Size(124*scale, 90*scale), style=Stroke(9f*scale, cap=StrokeCap.Round))
            drawLine(Color(0xFF2869B5), androidx.compose.ui.geometry.Offset(cx-7*scale, cy-10*scale), androidx.compose.ui.geometry.Offset(cx-7*scale, cy+18*scale), 7f*scale, StrokeCap.Round)
            drawLine(Color(0xFF2869B5), androidx.compose.ui.geometry.Offset(cx+7*scale, cy-10*scale), androidx.compose.ui.geometry.Offset(cx+7*scale, cy+18*scale), 7f*scale, StrokeCap.Round)
        } else {
            val handColor = Color(0xFF2869B5)
            drawRoundRect(handColor, androidx.compose.ui.geometry.Offset(cx-24*scale, cy-12*scale), androidx.compose.ui.geometry.Size(48*scale, 45*scale), androidx.compose.ui.geometry.CornerRadius(14f*scale))
            drawRoundRect(Color(0xFF4C8FD7), androidx.compose.ui.geometry.Offset(cx-18*scale, cy-36*scale), androidx.compose.ui.geometry.Size(36*scale, 34*scale), androidx.compose.ui.geometry.CornerRadius(12f*scale))
            if (group == PatientGroup.ADULT) {
                drawRoundRect(Color(0xFF1B4F91), androidx.compose.ui.geometry.Offset(cx-20*scale, cy-50*scale), androidx.compose.ui.geometry.Size(40*scale, 24*scale), androidx.compose.ui.geometry.CornerRadius(10f*scale))
            }
        }
        drawCircle(Color(0xFF0B5AB5), 4f*scale, androidx.compose.ui.geometry.Offset(cx, cy+47*scale))
    }
}

@Composable
private fun BreathingIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cx=size.width/2f; val cy=size.height/2f; val scale=minOf(size.width/210f,size.height/150f)
        drawCircle(Color(0xFFF2C0AC), 31f*scale, androidx.compose.ui.geometry.Offset(cx-42*scale,cy))
        drawRoundRect(Color(0xFF2B77B8), androidx.compose.ui.geometry.Offset(cx-20*scale,cy-25*scale), androidx.compose.ui.geometry.Size(92*scale,50*scale), androidx.compose.ui.geometry.CornerRadius(22f*scale))
        drawLine(Color(0xFF1E9DB5), androidx.compose.ui.geometry.Offset(cx-10*scale,cy-42*scale), androidx.compose.ui.geometry.Offset(cx+32*scale,cy-42*scale), 4f*scale, StrokeCap.Round)
        drawLine(Color(0xFF1E9DB5), androidx.compose.ui.geometry.Offset(cx+32*scale,cy-42*scale), androidx.compose.ui.geometry.Offset(cx+45*scale,cy-20*scale), 4f*scale, StrokeCap.Round)
        drawLine(Color(0xFF1E9DB5), androidx.compose.ui.geometry.Offset(cx+45*scale,cy-20*scale), androidx.compose.ui.geometry.Offset(cx+60*scale,cy-20*scale), 4f*scale, StrokeCap.Round)
        drawCircle(Color(0xFF168AAD), 7f*scale, androidx.compose.ui.geometry.Offset(cx+70*scale,cy-20*scale))
    }
}

@Composable
private fun AedIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cx=size.width/2f; val cy=size.height/2f; val scale=minOf(size.width/210f,size.height/150f)
        drawRoundRect(Color(0xFF164A83), androidx.compose.ui.geometry.Offset(cx-45*scale,cy-57*scale), androidx.compose.ui.geometry.Size(90*scale,114*scale), androidx.compose.ui.geometry.CornerRadius(15f*scale))
        drawRoundRect(Color.White, androidx.compose.ui.geometry.Offset(cx-35*scale,cy-45*scale), androidx.compose.ui.geometry.Size(70*scale,88*scale), androidx.compose.ui.geometry.CornerRadius(9f*scale))
        drawCircle(Color(0xFFE53935), 12f*scale, androidx.compose.ui.geometry.Offset(cx,cy-20*scale))
        drawLine(Color(0xFFFFC107), androidx.compose.ui.geometry.Offset(cx-4*scale,cy+7*scale), androidx.compose.ui.geometry.Offset(cx+5*scale,cy+22*scale), 4f*scale, StrokeCap.Round)
        drawLine(Color(0xFFFFC107), androidx.compose.ui.geometry.Offset(cx+5*scale,cy+22*scale), androidx.compose.ui.geometry.Offset(cx-5*scale,cy+36*scale), 4f*scale, StrokeCap.Round)
    }
}

@Composable
private fun ActivePathScreen(
    selection: SessionSelection,
    stepIndex: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
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
        Text("راهنمای احیای پایه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(step.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(step.instruction, style = MaterialTheme.typography.titleMedium)
                if (step.timeCritical) Text("اقدام فوری — برای تأیید صوتی منتظر نمانید.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
        StepVisualGuide(selection.patientGroup!!, step.id)
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

