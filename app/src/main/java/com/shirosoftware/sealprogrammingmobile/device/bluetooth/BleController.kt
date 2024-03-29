package com.shirosoftware.sealprogrammingmobile.device.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.shirosoftware.sealprogrammingmobile.device.SerialCommand
import com.shirosoftware.sealprogrammingmobile.domain.Device
import com.shirosoftware.sealprogrammingmobile.domain.DeviceConnectionState
import com.shirosoftware.sealprogrammingmobile.domain.DeviceDiscoveryState
import java.nio.charset.Charset
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class BleController(private val context: Context) : BluetoothController {
    private val _discoveryState =
        MutableStateFlow<DeviceDiscoveryState>(DeviceDiscoveryState.NotSearching)
    override val discoveryState: Flow<DeviceDiscoveryState> = _discoveryState

    private val _connectionState =
        MutableStateFlow<DeviceConnectionState>(DeviceConnectionState.Disconnected)
    override val connectionState: Flow<DeviceConnectionState> = _connectionState

    private var currentGatt: BluetoothGatt? = null

    private val _foundBluetoothDevices = mutableListOf<BluetoothDevice>()
    private val _foundDevices: MutableStateFlow<List<Device>> = MutableStateFlow(listOf())
    override val foundDevices: Flow<List<Device>> = _foundDevices

    private val scanner: BluetoothLeScanner? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter.bluetoothLeScanner
    }

    private var scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            if (result.device.name != null &&
                _foundBluetoothDevices.find { it.address == result.device.address } == null
            ) {
                Log.d("BleController", "Found: ${result.device.name}")
                _foundBluetoothDevices.add(result.device)
                _discoveryState.value = DeviceDiscoveryState.Searching
                _foundDevices.value = _foundBluetoothDevices.map { device ->
                    Device(
                        device.name,
                        device.address,
                        device.bondState == BluetoothDevice.BOND_BONDED,
                    )
                }
            }
        }

        override fun onBatchScanResults(results: List<ScanResult?>?) {
            super.onBatchScanResults(results)
        }
    }

    private val connectCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = DeviceConnectionState.Connecting
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    currentGatt = gatt
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    currentGatt = null
                    _connectionState.value = DeviceConnectionState.Disconnected
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    _connectionState.value = DeviceConnectionState.Connected
                }
            }
        }
    }

    override fun startDiscovery() {
        // 必要に応じてフィルタ
//        var scanFilter: ScanFilter = ScanFilter.Builder()
//            .build()

        _discoveryState.value = DeviceDiscoveryState.Searching
        _foundBluetoothDevices.clear()
        _foundDevices.value = listOf()
        scanner?.startScan(scanCallback)
    }

    override fun cancelDiscovery() {
        scanner?.stopScan(scanCallback)
        _discoveryState.value = DeviceDiscoveryState.NotSearching
    }

    override suspend fun connect(address: String) {
        _connectionState.value = DeviceConnectionState.Connecting

        _foundBluetoothDevices.find {
            it.address == address
        }?.connectGatt(context, false, connectCallback)
    }

    override suspend fun disconnect() {
        currentGatt?.disconnect()
    }

    override suspend fun write(command: String) {
        val characteristic =
            currentGatt?.getService(
                SERVICE_UUID
            )?.getCharacteristic(CHARACTERISTIC_UUID)

        characteristic ?: return

        withContext(Dispatchers.IO) {
            currentGatt?.let { gatt ->
                _connectionState.value = DeviceConnectionState.Writing

                // 転送中LED点灯
                writeCommand(gatt, characteristic, SerialCommand.ledRed100)

                // クリア
                writeCommand(gatt, characteristic, SerialCommand.clear)

                // 点滅
                writeCommand(gatt, characteristic, SerialCommand.blink)

                // 1文字ずつ送る
                command.map {
                    writeCommand(gatt, characteristic, it.toString())
                }

                // ゴール
                writeCommand(gatt, characteristic, SerialCommand.goal)

                // 転送中LED消灯
                writeCommand(gatt, characteristic, SerialCommand.ledRed0)

                _connectionState.value = DeviceConnectionState.Connected
            }
        }
    }

    private suspend fun writeCommand(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        command: String
    ) {
        gatt.writeCharacteristic(
            characteristic,
            command.toByteArray(Charset.forName("UTF-8")),
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
        )
        delay(10)
    }

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    }
}
