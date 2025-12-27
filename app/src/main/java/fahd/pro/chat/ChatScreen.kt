package fahd.pro.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // 1. Create the permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            // Check if BLUETOOTH_SCAN was granted. Handle the result in the ViewModel.
            val isGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
            viewModel.handleAction(ChatAction.OnPermissionGranted(isGranted))

        }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Permission: ${state.isPermissionGranted}")
        LazyColumn {
            item {
                Text(
                    text = "Messages",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(state.messages) {
                Text(text = it.message)
            }
            item {
                Text(
                    text = "Paired Devices",
                    style = MaterialTheme.typography.titleLarge
                )

            }
            items(state.pairedDevices) {
                TextButton(onClick = {
                    viewModel.handleAction(ChatAction.ConnectToDevice(it))
                }) {
                    it.name?.let { text -> Text(text = text) }
                }
            }
            item {
                Text(
                    text = "Scanned Devices",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(state.scannedDevices) {
                TextButton(onClick = {
                    viewModel.handleAction(ChatAction.ConnectToDevice(it))
                }) {
                    it.name?.let { text -> Text(text = text) }
                }
            }
        }
        Button(
            onClick = {
                if (state.isPermissionGranted) {
                    viewModel.handleAction(ChatAction.StartDiscovery)
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
            }
        ) {
            Text(
                text = "Start Discovery"
            )
        }
        Button(
            onClick = {
                if (state.isPermissionGranted) {
                    viewModel.handleAction(ChatAction.StartBluetoothServer)
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
            }
        ) {
            Text(
                text = "Start Server"
            )
        }
    }
}