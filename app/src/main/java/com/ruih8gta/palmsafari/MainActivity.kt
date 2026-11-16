package com.ruih8gta.palmsafari

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.Surface as DisplaySurface
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: SafariRenderer
    private var session: Session? = null
    private var scene by mutableStateOf(SceneSettings())
    private var inAr by mutableStateOf(false)
    private var status by mutableStateOf("3Dプレビュー · カメラは使用していません")
    private var error by mutableStateOf<String?>(null)
    private var showNotice by mutableStateOf(false)
    private var busy by mutableStateOf(false)
    private var resumed = false
    private var wantsAr = false
    private var installRequested = false
    private var checking = false
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if(allowed) ensureAr()
        else { wantsAr=false;busy=false;error="カメラを許可するとARで表示できます。3Dプレビューはそのまま使えます。" }
    }

    @SuppressLint("ClickableViewAccessibility") // Rotation is also exposed through the accessible slider.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scene=SceneSettings(
            savedInstanceState?.getInt("model") ?: 0,
            Motion.entries.getOrElse(savedInstanceState?.getInt("motion") ?: 0) { Motion.IDLE },
            savedInstanceState?.getFloat("size") ?: 1f,
            savedInstanceState?.getFloat("angle") ?: -30f,
        ).normalized()
        renderer=SafariRenderer(loadCreatures(this)) { message ->
            runOnUiThread { if(!isDestroyed) status=message }
        }
        renderer.settings=scene
        glView=GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8,8,8,8,16,0)
            preserveEGLContextOnPause=true
            setRenderer(renderer)
            contentDescription="3D表示。モデルや向きは下の操作からも変更できます。"
        }
        var downX=0f;var downY=0f;var downAngle=0f
        glView.setOnTouchListener { v,e ->
            when(e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX=e.x;downY=e.y;downAngle=scene.angle }
                MotionEvent.ACTION_MOVE -> if(!inAr) updateScene(scene.copy(angle=((downAngle+(e.x-downX)*.5f+540f)%360f)-180f))
                MotionEvent.ACTION_UP -> {
                    if(inAr && abs(e.x-downX)<20 && abs(e.y-downY)<20) renderer.tap(e.x,e.y)
                    v.performClick()
                }
            }
            true
        }
        setContent {
            val scheme=if(isSystemInDarkTheme()) darkColorScheme(primary=Color(0xFFB5D2A8))
                else lightColorScheme(primary=Color(0xFF345B43),surface=Color(0xFFF8F6EF),background=Color(0xFFF8F6EF))
            MaterialTheme(colorScheme=scheme) {
                BackHandler(enabled=inAr) { leaveAr() }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                            Text("てのひらサファリ",style=MaterialTheme.typography.titleLarge)
                            Text(if(inAr) "AR" else "3Dプレビュー",style=MaterialTheme.typography.labelLarge)
                        }
                        Text(status,Modifier.padding(horizontal=20.dp).padding(bottom=8.dp),style=MaterialTheme.typography.bodySmall)
                        BoxWithConstraints(Modifier.weight(1f)) {
                            if(maxWidth > 640.dp) {
                                Row {
                                    SceneView(Modifier.weight(1f).fillMaxHeight())
                                    Controls(Modifier.width(310.dp).fillMaxHeight())
                                }
                            } else {
                                Column {
                                    SceneView(Modifier.weight(1f).fillMaxWidth())
                                    Controls(Modifier.fillMaxWidth().heightIn(max=360.dp))
                                }
                            }
                        }
                    }
                }
                if(showNotice) AlertDialog(
                    onDismissRequest={showNotice=false},
                    title={Text("カメラでARを楽しむ")},
                    text={Column {
                        Text("カメラで床や机を見つけ、モデルを置きます。周囲に気をつけて、立ち止まってお楽しみください。")
                        Spacer(Modifier.height(12.dp))
                        Text("AR機能はGoogleが提供するGoogle Play開発者サービス（AR）を使用します。このサービスのデータの取り扱いにはGoogleのプライバシーポリシーが適用されます。")
                        TextButton(onClick={openUrl("https://policies.google.com/privacy")}) { Text("Googleのプライバシーポリシー") }
                    }},
                    confirmButton={TextButton(onClick={showNotice=false;wantsAr=true;installRequested=false;checkAr()}) {Text("カメラで始める")}},
                    dismissButton={TextButton(onClick={showNotice=false}) {Text("あとで")}},
                )
            }
        }
    }
    @Composable private fun SceneView(modifier: Modifier) {
        AndroidView(factory={glView},modifier=modifier)
    }
    @Composable private fun Controls(modifier: Modifier) {
        Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("ティラノ","キツネ").forEachIndexed { i,label ->
                    FilterChip(selected=scene.model==i,onClick={updateScene(scene.copy(model=i))},label={Text(label)})
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(Motion.IDLE to "のんびり",Motion.STEP to "足ぶみ",Motion.PAUSE to "停止").forEach { (motion,label) ->
                    FilterChip(selected=scene.motion==motion,onClick={updateScene(scene.copy(motion=motion))},label={Text(label)})
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("大きさ %.1f倍".format(scene.size),style=MaterialTheme.typography.labelMedium)
                    Slider(scene.size,{updateScene(scene.copy(size=it))},valueRange=.5f..1.5f,
                        modifier=Modifier.semantics { contentDescription="モデルの大きさ" })
                }
                Column(Modifier.weight(1f)) {
                    Text("向き ${scene.angle.toInt()}°",style=MaterialTheme.typography.labelMedium)
                    Slider(scene.angle,{updateScene(scene.copy(angle=it))},valueRange=-180f..180f,
                        modifier=Modifier.semantics { contentDescription="モデルの向き" })
                }
            }
            if(inAr) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={glView.queueEvent {renderer.resetPlacement()}},modifier=Modifier.weight(1f)) {Text("置き直す")}
                Button(onClick={leaveAr()},modifier=Modifier.weight(1f)) {Text("プレビューへ")}
            } else Button(onClick={showNotice=true},enabled=!busy,modifier=Modifier.fillMaxWidth()) {
                Text(if(busy) "ARを準備しています…" else "ARをはじめる")
            }
            error?.let { message ->
                Text(message,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))}) {Text("アプリの設定を開く")}
            }
        }
    }
    private fun updateScene(value: SceneSettings) { scene=value.normalized();renderer.settings=scene }
    private fun openUrl(url: String) { runCatching { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) }.onFailure { error="リンクを開けませんでした。ブラウザの設定をご確認ください。" } }
    private fun checkAr() {
        if(checking || !resumed) return
        busy=true;checking=true;error=null
        ArCoreApk.getInstance().checkAvailabilityAsync(this) { availability ->
            runOnUiThread {
                checking=false
                if(isDestroyed || !wantsAr) return@runOnUiThread
                if(availability.isSupported) ensureAr()
                else { busy=false;wantsAr=false;error=if(availability.isTransient || availability.isUnknown)
                    "AR対応を確認できませんでした。通信状態を確認して、もう一度お試しください。"
                    else "この端末はARに対応していません。3Dプレビューでお楽しみください。" }
            }
        }
    }
    private fun ensureAr() {
        if(!resumed || !wantsAr || session != null) return
        try {
            when(ArCoreApk.getInstance().requestInstall(this,!installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {installRequested=true;return}
                ArCoreApk.InstallStatus.INSTALLED -> Unit
            }
            if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                cameraPermission.launch(Manifest.permission.CAMERA);return
            }
            val created=Session(this)
            try {
                created.configure(Config(created).apply {
                    planeFindingMode=Config.PlaneFindingMode.HORIZONTAL
                    focusMode=Config.FocusMode.AUTO
                    lightEstimationMode=Config.LightEstimationMode.DISABLED
                })
                created.resume()
            } catch(e: Exception) {created.close();throw e}
            glView.onPause()
            renderer.resetPlacement();session=created;renderer.session=created
            inAr=true;busy=false;error=null
            glView.onResume()
        } catch(e: Exception) {
            wantsAr=false;busy=false
            error=when(e) {
                is UnavailableDeviceNotCompatibleException -> "この端末はARに対応していません。"
                is UnavailableUserDeclinedInstallationException -> "ARサービスのインストールが必要です。もう一度開始すると案内を開けます。"
                is UnavailableApkTooOldException -> "Google Play開発者サービス（AR）を更新してください。"
                is UnavailableSdkTooOldException -> "このARサービスに対応したアプリの更新が必要です。"
                is CameraNotAvailableException -> "カメラを利用できません。他のカメラアプリを閉じて再試行してください。"
                else -> "ARを開始できませんでした。端末の対応とARサービスを確認して、もう一度お試しください。"
            }
        }
    }
    private fun leaveAr() {
        wantsAr=false;busy=false;error=null
        glView.onPause();renderer.resetPlacement();renderer.session=null
        session?.pause();session?.close();session=null;inAr=false
        if(resumed) glView.onResume()
        status="3Dプレビュー · カメラは使用していません"
    }
    override fun onResume() {
        super.onResume();resumed=true
        @Suppress("DEPRECATION")
        val rotation=windowManager.defaultDisplay?.rotation ?: DisplaySurface.ROTATION_0
        renderer.displayRotation=rotation
        try {session?.resume()} catch(e: CameraNotAvailableException) {leaveAr();error="カメラを再開できませんでした。ARをもう一度開始してください。"}
        glView.onResume()
        if(wantsAr && session==null && !checking) checkAr()
    }
    override fun onPause() {
        resumed=false;glView.onPause();renderer.suspendClock();session?.pause();super.onPause()
    }
    override fun onDestroy() {
        renderer.resetPlacement();renderer.session=null;session?.close();session=null;super.onDestroy()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("model",scene.model);outState.putInt("motion",scene.motion.ordinal)
        outState.putFloat("size",scene.size);outState.putFloat("angle",scene.angle)
        super.onSaveInstanceState(outState)
    }
}
