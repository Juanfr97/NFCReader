package com.alparslanguney.example.nfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.alparslanguney.example.nfc.ui.theme.ComposeNFCTheme
import com.alparslanguney.example.nfc.util.INTENT_ACTION_NFC_READ
import com.alparslanguney.example.nfc.util.NfcBroadcastReceiver
import com.alparslanguney.example.nfc.util.getParcelableCompatibility

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            ComposeNFCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting()
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {

                    }
                }
            }
        }
    }

    private fun enableNfcForegroundDispatch() {
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                val nfcIntentFilter = arrayOf(
                    IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                    IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                    IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
                )

                val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_MUTABLE
                    )
                } else {
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
                adapter.enableForegroundDispatch(
                    this, pendingIntent, nfcIntentFilter, null
                )
            }
        }
    }

    private fun disableNfcForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        disableNfcForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val action = intent?.action
        Log.d("NFC", "Intent action: $action")

        if (action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            val ndefMessage = readNdefMessage(tag)
            if (ndefMessage != null) {
                Toast.makeText(this, "NFC NDEF Message: $ndefMessage", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "NFC tag detected, but no NDEF data", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun Greeting(modifier: Modifier = Modifier) {

    var nfcCardId by remember {
        mutableStateOf("")
    }

    Text(text = "Read Card : $nfcCardId", modifier = modifier)

    NfcBroadcastReceiver { tag ->
        nfcCardId = tag.id.toHexString()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    ComposeNFCTheme {
        Greeting()
    }
}

fun readNdefMessage(tag: Tag?): String? {
    val ndef = Ndef.get(tag)
    return if (ndef != null) {
        ndef.connect()
        val ndefMessage = ndef.ndefMessage
        val records = ndefMessage?.records
        val recordData = records?.mapNotNull { record ->
            record.payload?.let { String(it) }
        }?.joinToString()
        ndef.close()
        recordData
    } else {
        Log.d("NFC", "No NDEF records found.")
        null
    }
}