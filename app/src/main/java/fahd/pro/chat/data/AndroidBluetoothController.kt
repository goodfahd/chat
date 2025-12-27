package fahd.pro.chat.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import fahd.pro.chat.domain.model.BluetoothController
import fahd.pro.chat.domain.model.BluetoothDeviceDomain
import fahd.pro.chat.domain.model.BluetoothMessage
import fahd.pro.chat.domain.model.ConnectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    private val bluetoothManager by lazy { context.getSystemService(BluetoothManager::class.java) }
    private val adapter by lazy { bluetoothManager?.adapter }

    // --- State Management ---
    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> =
        _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> =
        _pairedDevices.asStateFlow()

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null

    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // --- 1. Discovery Implementation ---
    private val deviceFoundReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            device?.let {
                val domainDevice = it.toBluetoothDeviceDomain()
                _scannedDevices.update { devices ->
                    if (domainDevice in devices) devices else devices + domainDevice
                }
            }
        }
    }

    override fun startDiscovery() {
        updatePairedDevices()
        context.registerReceiver(deviceFoundReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        adapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        adapter?.cancelDiscovery()
        try {
            context.unregisterReceiver(deviceFoundReceiver)
        } catch (e: Exception) { /* Receiver not registered */
        }
    }

    private fun updatePairedDevices() {
        val devices = adapter?.bondedDevices?.map { it.toBluetoothDeviceDomain() } ?: emptyList()
        _pairedDevices.update { devices }
    }

    // --- 2. Connection Implementation (Server & Client) ---
    override fun startBluetoothServer(): Flow<ConnectionResult> = flow {
        currentServerSocket = adapter?.listenUsingRfcommWithServiceRecord("chat_service", MY_UUID)

        while (true) {
            val socket = try {
                currentServerSocket?.accept()
            } catch (e: IOException) {
                emit(ConnectionResult.Error("Server closed"))
                null
            }

            socket?.let {
                currentClientSocket = it
                emit(ConnectionResult.ConnectionEstablished)
                // Once connected, start the message-reading loop
                emitAll(readIncomingMessages(it))
            }
        }
    }.onCompletion { release() }.flowOn(Dispatchers.IO)

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> = flow {
        val bluetoothDevice = adapter?.getRemoteDevice(device.address)
        currentClientSocket = bluetoothDevice?.createRfcommSocketToServiceRecord(MY_UUID)

        stopDiscovery() // Discovery slows down connections significantly

        try {
            currentClientSocket?.connect()
            emit(ConnectionResult.ConnectionEstablished)
            emitAll(readIncomingMessages(currentClientSocket!!))
        } catch (e: IOException) {
            currentClientSocket?.close()
            emit(ConnectionResult.Error("Connection failed"))
        }
    }.onCompletion { release() }.flowOn(Dispatchers.IO)

    // --- 3. Data Transfer Implementation ---
    private fun readIncomingMessages(socket: BluetoothSocket): Flow<ConnectionResult> = flow {
        val inputStream = socket.inputStream
        val buffer = ByteArray(1024)

        while (true) {
            val byteCount = try {
                inputStream.read(buffer)
            } catch (e: IOException) {
                emit(ConnectionResult.Error("Connection lost"))
                break
            }

            val messageString = buffer.decodeToString(endIndex = byteCount)
            emit(
                ConnectionResult.TransferSucceeded(
                    BluetoothMessage(messageString, "Partner", false)
                )
            )
        }
    }

    override suspend fun trySendMessage(message: String): BluetoothMessage? {
        val socket = currentClientSocket ?: return null
        if (!socket.isConnected) return null

        return try {
            socket.outputStream.write(message.toByteArray())
            BluetoothMessage(message, "Me", true)
        } catch (e: IOException) {
            null
        }
    }

    override fun release() {
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentClientSocket = null
        currentServerSocket = null
    }
}