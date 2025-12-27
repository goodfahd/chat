package fahd.pro.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fahd.pro.chat.data.AndroidBluetoothController
import fahd.pro.chat.domain.model.BluetoothDeviceDomain
import fahd.pro.chat.domain.model.BluetoothMessage
import fahd.pro.chat.domain.model.ConnectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val btController = AndroidBluetoothController(application)
    private val _state = MutableStateFlow(ChatState())
    val state = _state.asStateFlow()

    init {
        observeDeviceChanges()
    }
    fun handleAction(action: ChatAction) {
        when (action) {
            ChatAction.StartDiscovery -> {
                btController.startDiscovery()
            }

            ChatAction.StartBluetoothServer -> {
                viewModelScope.launch {
                    btController.startBluetoothServer().collect { result ->
                        when (result) {
                            is ConnectionResult.ConnectionEstablished -> {
                                _state.value = _state.value.copy(
                                    messages = _state.value.messages + BluetoothMessage(
                                        message = "Connection established",
                                        senderName = "System",
                                        isFromLocalUser = false
                                    )
                                )
                            }

                            is ConnectionResult.Error -> {
                                _state.value = _state.value.copy(
                                    messages = _state.value.messages + BluetoothMessage(
                                        message = result.message,
                                        senderName = "System",
                                        isFromLocalUser = false
                                    )
                                )
                            }

                            is ConnectionResult.TransferSucceeded -> {
                                _state.value = _state.value.copy(
                                    messages = _state.value.messages + result.message
                                )
                            }
                        }
                    }
                }
            }

            is ChatAction.ConnectToDevice -> {
                viewModelScope.launch {
                    btController.connectToDevice(action.bluetoothDeviceDomain).collect { result ->
                        when (result) {
                            is ConnectionResult.ConnectionEstablished -> {
                                _state.value = _state.value.copy(
                                    messages = _state.value.messages + BluetoothMessage(
                                        message = "Connection established",
                                        senderName = "System",
                                        isFromLocalUser = false
                                    )
                                )
                            }

                            is ConnectionResult.Error -> {
                                _state.value = _state.value.copy(
                                    messages = _state.value.messages + BluetoothMessage(
                                        message = result.message,
                                        senderName = "System",
                                        isFromLocalUser = false
                                    )
                                )
                            }

                            is ConnectionResult.TransferSucceeded -> {
                                _state.value = _state.value.copy(
                                    messages = _state.value.messages + result.message
                                )
                            }
                        }
                    }
                }
            }

            is ChatAction.SendMessage -> {
                viewModelScope.launch {
                    val message = btController.trySendMessage(action.message)
                    if (message != null) {
                        _state.value = _state.value.copy(
                            messages = _state.value.messages + message
                        )
                    }
                }
            }

            is ChatAction.DisconnectFromDevice -> {
                btController.release()
            }

            ChatAction.StopDiscovery -> {
                btController.stopDiscovery()
            }

            is ChatAction.OnPermissionGranted -> {
                _state.value = _state.value.copy(isPermissionGranted = action.granted)
            }
        }
    }

    private fun observeDeviceChanges() {
        viewModelScope.launch {
            btController.scannedDevices
                .onEach { devices ->
                    _state.update { it.copy(scannedDevices = devices) }
                }
                .catch {
                    // Optional: Handle errors in the flow
                }
                .collect() // Terminal operator to start collection
        }
        viewModelScope.launch {
            btController.pairedDevices
                .onEach { devices ->
                    _state.update { it.copy(pairedDevices = devices) }
                }
                .catch {
                    // Optional: Handle errors in the flow
                }
                .collect() // Terminal operator to start collection
        }
    }
}

data class ChatState(
    val isLoading: Boolean = false,
    val scannedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val pairedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val messages: List<BluetoothMessage> = emptyList(),
    val isPermissionGranted: Boolean = false
)

sealed interface ChatAction {
    data object StartDiscovery : ChatAction
    data object StopDiscovery : ChatAction
    data object StartBluetoothServer : ChatAction

    data class ConnectToDevice(val bluetoothDeviceDomain: BluetoothDeviceDomain) : ChatAction
    data class DisconnectFromDevice(val bluetoothDeviceDomain: BluetoothDeviceDomain) : ChatAction
    data class SendMessage(val message: String) : ChatAction

    // Permission Granted
    data class OnPermissionGranted(val granted: Boolean) : ChatAction

}