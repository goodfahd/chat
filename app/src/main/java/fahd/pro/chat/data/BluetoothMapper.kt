package fahd.pro.chat.data

import android.bluetooth.BluetoothDevice
import fahd.pro.chat.domain.model.BluetoothDeviceDomain

fun BluetoothDevice.toBluetoothDeviceDomain() = BluetoothDeviceDomain(
    name = name,
    address = address
)