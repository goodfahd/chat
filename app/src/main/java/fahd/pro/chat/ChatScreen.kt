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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
    handleAction: (ChatAction) -> Unit
) {
    // 1. Create the permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            // Check if BLUETOOTH_SCAN was granted. Handle the result in the ViewModel.
            val isGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
            viewModel.onPermissionResult(isGranted)
        }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (viewModel.isLoading) "Loading..." else "Available Devices: ${viewModel.availableDevices}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = viewModel.message,
            style = MaterialTheme.typography.labelSmall
        )
        LazyColumn {
            items(viewModel.devices) {
                TextButton(onClick = {
                    handleAction(ChatAction.ConnectToDevice(it))
                }) {
                    Text(text = it.name)
                }
            }
        }
        Button(
            onClick = {
                if (viewModel.permissionGranted) {
                    viewModel.handleAction(ChatAction.ScanForDevices)
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                }
            },
            modifier = Modifier
        ) {
            Text(
                text =
                    if (viewModel.availableDevices == 0) {
                        "Scan for Devices"
                    } else {
                        "Start Chat"
                    }
            )
        }
    }
}