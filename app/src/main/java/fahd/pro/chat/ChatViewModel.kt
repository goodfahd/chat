package fahd.pro.chat

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import fahd.pro.chat.domain.model.BluetoothDeviceDomain
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    var permissionGranted by mutableStateOf(false)
        private set

    var availableDevices by mutableIntStateOf(0)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set

    var devices by mutableStateOf(emptyList<BluetoothDeviceDomain>())
        private set

    private val bluetoothManager =
        application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    fun handleAction(action: ChatAction) {
        when (action) {
            ChatAction.ScanForDevices -> {
                scanForDevices()
            }

            is ChatAction.ConnectToDevice -> {}
        }
    }

    private fun scanForDevices() {
        viewModelScope.launch {
            isLoading = true
            delay(1000)
            val bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter != null) {
                val hasBluetoothPermission = application.checkSelfPermission(
                    "android.permission.BLUETOOTH_CONNECT"
                ) == PackageManager.PERMISSION_GRANTED
                if (hasBluetoothPermission) {
                    if (bluetoothAdapter.isDiscovering) {
                        bluetoothAdapter.cancelDiscovery()
                        message = "Already Scanning for devices..."
                        availableDevices = bluetoothAdapter.bondedDevices.size
                        isLoading = false
                    } else {
                        message = "Scanning for devices..."
                        bluetoothAdapter.startDiscovery()
                        availableDevices = bluetoothAdapter.bondedDevices.size
                        isLoading = false
                    }
                    devices = bluetoothAdapter.bondedDevices.map {
                        BluetoothDeviceDomain(it.name ?: "Unknown", it.address)
                    }
                } else {
                    message = "Bluetooth permission not granted"
                    isLoading = false
                    permissionGranted = false
                }
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        permissionGranted = isGranted
        if (isGranted) {
            scanForDevices()
        }
    }
}

sealed interface ChatAction {
    data object ScanForDevices : ChatAction
    data class ConnectToDevice(val bluetoothDeviceDomain: BluetoothDeviceDomain) : ChatAction
}