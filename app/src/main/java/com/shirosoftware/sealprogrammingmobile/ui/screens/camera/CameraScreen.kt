package com.shirosoftware.sealprogrammingmobile.ui.screens.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier,
    onCaptured: (path: String) -> Unit = {},
    onClickSettings: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    // val configuration = LocalConfiguration.current

    var lensFacing by remember {
        mutableStateOf(CameraSelector.LENS_FACING_BACK)
    }

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    val state by viewModel.state.collectAsState()

    LaunchedEffect(lensFacing, state) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()

        if (state == CameraState.Ready) {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

            preview.setSurfaceProvider(previewView.surfaceProvider)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onClickSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        when (state) {
            CameraState.Captured -> {
                
            }
            CameraState.Ready -> {
                Column(
                    modifier = modifier
                        .padding(innerPadding)
                        .background(Color.Black)
                ) {
                    // プレビュー
                    AndroidView(
                        modifier = modifier
                            .fillMaxWidth()
                            .aspectRatio(3.0f / 4.0f),
                        factory = {
                            previewView
                        },
                    )

                    ConstraintLayout(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val (shutterButton, switchButton) = createRefs()

                        ShutterButton(
                            modifier = Modifier
                                .constrainAs(shutterButton) {
                                    centerTo(parent)
                                },
                            onClick = {
                                val file = viewModel.createImageFile()
                                val outputOptions = ImageCapture.OutputFileOptions
                                    .Builder(file)
                                    .build()

                                imageCapture.takePicture(outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            Log.d("CameraScreen", "Save a picture: Success")
                                            //onCaptured.invoke(file.absolutePath)
                                            viewModel.updateState(CameraState.Captured)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e(
                                                "CameraScreen",
                                                "Save a picture: Error",
                                                exception
                                            )
                                        }
                                    })
                            }
                        )
                        SwitchCameraButton(
                            modifier = Modifier
                                .size(56.dp)
                                .constrainAs(switchButton) {
                                    centerVerticallyTo(parent)
                                    start.linkTo(shutterButton.end)
                                    end.linkTo(parent.end)
                                },
                        ) {
                            lensFacing =
                                if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                                    CameraSelector.LENS_FACING_BACK
                                else CameraSelector.LENS_FACING_FRONT
                        }
                    }
                }
            }
        }
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }
