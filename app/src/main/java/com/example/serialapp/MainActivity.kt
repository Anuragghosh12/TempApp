package com.example.serialapp

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.serialapp.ui.theme.SerialAppTheme
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : ComponentActivity() {

    private var usbManager: UsbManager? = null
    private var serialPort: UsbSerialPort? = null
    private var connection: UsbDeviceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        usbManager = getSystemService(UsbManager::class.java)
        val device = usbManager?.deviceList?.values?.firstOrNull() // Assumes the first device is the target

        if (device != null) {
            setupSerialConnection(device)
        }

        setContent {
            SerialAppTheme {
                MainScreen(serialPort)
            }
        }
    }

    private fun setupSerialConnection(device: UsbDevice) {
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device)
        driver?.let {
            serialPort = it.ports[0]
            connection = usbManager?.openDevice(device)
            serialPort?.open(connection)
            serialPort?.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(serialPort: UsbSerialPort?) {
    var receivedData by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Coroutine for reading data continuously
    LaunchedEffect(serialPort) {
        if (serialPort != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val buffer = ByteArray(1024)
                while (true) {
                    try {
                        val bytesRead = serialPort.read(buffer, 1000)
                        if (bytesRead > 0) {
                            val receivedText = String(buffer, 0, bytesRead)
                            receivedData += receivedText
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Serial Communication") }) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Received Data:",
                    style = MaterialTheme.typography.bodyLarge
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(receivedData)
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Type your message") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                serialPort?.write(message.toByteArray(), 1000)
                                message = "" // Clear input after sending
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send")
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SerialAppTheme {
        MainScreen(null)
    }
}
