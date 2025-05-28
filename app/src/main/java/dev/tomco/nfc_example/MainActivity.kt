package dev.tomco.nfc_example

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.tomco.nfc_example.ui.theme.NFC_ExampleTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        setContent {
            MaterialTheme {
                val jsonContent = remember { mutableStateOf("Scan an NFC tag") }
                NFCContentDisplay(jsonContent.value)
                LaunchedEffect(Unit) {
                    intent?.let { processIntent(it, jsonContent) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val jsonContent = mutableStateOf("")
        processIntent(intent, jsonContent)
        setContent {
            MaterialTheme {
                NFCContentDisplay(jsonContent.value)
            }
        }
    }

    private fun processIntent(intent: Intent, jsonContent: MutableState<String>) {
        if (intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED || intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {

            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            tag?.let {
                val ndef = Ndef.get(it)
                ndef?.connect()
                val message: NdefMessage? = ndef?.ndefMessage
                val json = message?.records?.firstOrNull()?.payload?.drop(0)?.toByteArray()
                    ?.toString(Charsets.UTF_8)
                json?.let { jsonContent.value = it }
                ndef?.close()
            }
        }
    }
}

@Composable
fun NFCContentDisplay(json: String) {
    Surface {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = json,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Left
            )
        }
    }
}


