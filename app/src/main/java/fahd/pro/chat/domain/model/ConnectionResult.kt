package fahd.pro.chat.domain.model

sealed interface ConnectionResult {
    // Emitted when the socket is successfully opened and streams are ready
    object ConnectionEstablished : ConnectionResult

    // Emitted when the data transfer starts or a message is received
    data class TransferSucceeded(val message: BluetoothMessage) : ConnectionResult

    // Emitted when something goes wrong (e.g., device out of range, user rejected)
    data class Error(val message: String) : ConnectionResult
}