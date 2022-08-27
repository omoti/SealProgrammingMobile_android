package com.shirosoftware.sealprogrammingmobile.ui.screens.main

import android.Manifest
import android.app.Activity.RESULT_OK
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.shirosoftware.sealprogrammingmobile.R
import com.shirosoftware.sealprogrammingmobile.camera.CameraController
import com.shirosoftware.sealprogrammingmobile.device.bluetooth.BluetoothConnection
import com.shirosoftware.sealprogrammingmobile.device.bluetooth.BluetoothConnectionState
import com.shirosoftware.sealprogrammingmobile.device.bluetooth.BluetoothController
import com.shirosoftware.sealprogrammingmobile.ml.SealDetector
import com.shirosoftware.sealprogrammingmobile.ui.components.CircleButton
import com.shirosoftware.sealprogrammingmobile.ui.theme.BackgroundDark
import com.shirosoftware.sealprogrammingmobile.ui.theme.Primary
import com.shirosoftware.sealprogrammingmobile.ui.theme.SealProgrammingMobileTheme
import com.shirosoftware.sealprogrammingmobile.ui.theme.Secondary
import com.shirosoftware.sealprogrammingmobile.ui.theme.SecondaryDisable
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val bitmap = viewModel.bitmap.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
            viewModel.loadCapturedImage()
        }
    )

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden
    )
    val bluetoothState = viewModel.bluetoothState.collectAsState()

    val device = viewModel.selectedDevice.collectAsState()
    val connectionState = viewModel.connectionState.collectAsState()
    val writing = viewModel.writing.collectAsState()

    val permissionState =
        rememberMultiplePermissionsState(
            if (Build.VERSION.SDK_INT >= 31) {
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            } else {
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            }
        )
    if (!permissionState.allPermissionsGranted) {
        SideEffect {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.disconnect()
        }
    }

    // デバイス一覧で検索状態を連動
    LaunchedEffect(sheetState.isVisible) {
        if (!permissionState.allPermissionsGranted) return@LaunchedEffect
        if (sheetState.isVisible) {
            viewModel.startSearchDevices()
        } else {
            viewModel.stopSearchDevices()
        }
    }

    LaunchedEffect(connectionState.value) {
        Log.d("MainScreen", connectionState.value.toString())
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp),
        sheetContent = {
            DeviceList(state = bluetoothState.value) { device ->
                scope.launch { sheetState.hide() }
                viewModel.connect(device)
            }
        }
    ) {
        Scaffold(topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        color = Color.White,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Primary),
            )
        }) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.White),
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(3.0f / 4.0f)
                        .fillMaxWidth()
                ) {
                    bitmap.value?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = modifier
                        .fillMaxSize()
                        .background(BackgroundDark)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircleButton(
                            Icons.Default.PhotoCamera,
                            stringResource(id = R.string.main_button_camera),
                            onClick = {
                                viewModel.dispatchTakePicture()?.let {
                                    launcher.launch(it)
                                    viewModel.startSearchDevices()
                                }
                            })
                        CircleButton(
                            if (connectionState.value == BluetoothConnectionState.Connected) {
                                Icons.Default.BluetoothConnected
                            } else {
                                Icons.Default.Bluetooth
                            },
                            stringResource(id = R.string.main_button_connect),
                            onClick = {
                                scope.launch { sheetState.show() }
                            },
                        )
                        CircleButton(
                            Icons.Default.ElectricCar,
                            stringResource(id = R.string.main_button_send),
                            onClick = { viewModel.sendCommand() },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Secondary,
                                disabledBackgroundColor = SecondaryDisable,
                            ),
                            enabled = (bitmap.value != null
                                    && connectionState.value == BluetoothConnectionState.Connected)
                                    && !writing.value
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = device.value?.name?.let {
                            "$it : ${
                                if (writing.value) "送信中..."
                                else
                                    when (connectionState.value) {
                                        BluetoothConnectionState.Connected -> "接続済"
                                        BluetoothConnectionState.Connecting -> "接続中..."
                                        BluetoothConnectionState.Disconnected -> "未接続"
                                        BluetoothConnectionState.Writing -> "送信中..."
                                        is BluetoothConnectionState.Error -> "エラー"
                                    }
                            }"
                        } ?: "未選択",
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontSize = 12.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BackgroundDark),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    val context = LocalContext.current
    SealProgrammingMobileTheme {
        MainScreen(
            MainViewModel(
                CameraController(context),
                SealDetector(context),
                BluetoothController(context, BluetoothConnection()),
            )
        )
    }
}