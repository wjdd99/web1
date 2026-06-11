package com.beolddeok.alarm.ui.ring.missions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.beolddeok.alarm.data.Alarm
import com.beolddeok.alarm.data.AlarmStore
import com.beolddeok.alarm.util.ImageHash

/**
 * 침대 탈출 사진 인증 미션.
 * - 기준 사진이 없으면(첫 알람): 집안 장소 사진 strength 장을 찍어 기준으로 등록.
 * - 기준 사진이 있으면: 그 장소들로 가서 같은 사진을 찍어 모두 일치시켜야 꺼짐.
 */
@Composable
fun PhotoMission(alarm: Alarm, onSolved: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = remember { AlarmStore.get(context) }

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) permLauncher.launch(Manifest.permission.CAMERA) }

    val registering = alarm.photoHashes.isEmpty()
    val target = alarm.missionStrength
    val captured = remember { mutableStateListOf<String>() }      // 등록 모드에서 모은 해시
    val matched = remember { mutableStateListOf<Int>() }          // 검증 모드에서 일치한 기준 index
    var status by remember { mutableStateOf("") }

    val imageCapture = remember { ImageCapture.Builder().build() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (registering) "📷 기준 장소 등록 (${captured.size}/$target)"
            else "📷 등록한 장소로 가서 찍으세요 (${matched.size}/${alarm.photoHashes.size})",
            fontSize = 18.sp, fontWeight = FontWeight.Bold,
        )
        Text(
            if (registering) "침대에서 떨어진 곳(세면대·냉장고·현관 등)을 찍으세요."
            else "처음 등록했던 장소를 찾아 같은 구도로 찍으세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 6.dp),
        )

        if (hasCamera) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture,
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )

            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.error)

            Button(
                onClick = {
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bmp = image.toBitmap()
                                image.close()
                                val hash = ImageHash.compute(bmp)

                                if (registering) {
                                    captured.add(hash)
                                    if (captured.size >= target) {
                                        store.upsert(alarm.copy(photoHashes = captured.toList()))
                                        onSolved()
                                    }
                                } else {
                                    val foundIdx = alarm.photoHashes.indices.firstOrNull { i ->
                                        i !in matched && ImageHash.matches(alarm.photoHashes[i], hash)
                                    }
                                    if (foundIdx != null) {
                                        matched.add(foundIdx); status = "✅ 일치!"
                                        if (matched.size >= alarm.photoHashes.size) onSolved()
                                    } else {
                                        status = "❌ 등록한 장소가 아니에요. 다시!"
                                    }
                                }
                            }

                            override fun onError(exc: ImageCaptureException) {
                                status = "촬영 실패: ${exc.message}"
                            }
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (registering) "이 장소 등록하기" else "찍어서 인증하기") }
        } else {
            Text("카메라 권한이 필요합니다.", color = MaterialTheme.colorScheme.error)
        }
    }
}
