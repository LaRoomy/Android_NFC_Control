package com.example.nfccontrol

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcV
import java.util.Timer
import kotlin.experimental.and

class NFCController(context: Context) {

    private var nfcListener: NfcListener? = null
    private var ftmPollTimer: Timer? = null
    private var isConnected = false
    private var isPolling = false

    private var nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    private var nfcTag: NfcV? = null

    fun <T>enableNFC(activity: Activity, classType: Class<T>) {
        nfcAdapter?.let {
            this.enableNFCInForeground(it, activity, classType)
        }
    }

    fun disableNFC(activity: Activity) {
        nfcAdapter?.let {
            this.disableNFCInForeground(it, activity)
        }
        nfcTag?.let {
            it.close()
            nfcTag = null
        }
    }

    fun setNfcListener(nfcListener: NfcListener) {
        this.nfcListener = nfcListener
    }

    fun writeNFCMessage(payload: String, intent: Intent?): Boolean {

        var outputData: ByteArray = byteArrayOf()
        outputData += (0x02).toByte()
        outputData += (0xAA).toByte()
        outputData += (0x02).toByte()

        if(payload.isNotEmpty()) {
            outputData += (payload.length).toByte()
            outputData += payload.toByteArray()
        }
        else {
            outputData += (0x00).toByte()
        }
        outputData += (0x00).toByte()

        intent?.let {
            val tag = it.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            return writeMessageToTag(outputData, tag)
        }
        return false
    }

    private fun writeMessageToTag(data: ByteArray, tag: Tag?): Boolean {

        try {
            if(nfcTag == null) {
                nfcTag = NfcV.get(tag)
            }
            nfcTag?.let {
                if(!it.isConnected) {
                    it.connect()
                }
                if(it.maxTransceiveLength < data.size) {
                    nfcListener?.onNfcOperationFail("Message to large to write to NFC tag")
                    return false
                }
                if(it.isConnected) {
                    setConnectionState(true)
                    val response = it.transceive(data)
                    nfcListener?.onNfcOperationSuccess("Message is written to tag successfully. Response: ${formatResult(response)}")
                    pollForMailboxWritten(it)
                    return true
                } else {
                    nfcListener?.onNfcOperationFail("NFC tag is read-only")
                    return false
                }
            }
            nfcListener?.onNfcOperationFail("NFC operation failed.")
            return false

        } catch (tle: TagLostException) {
            setConnectionState(false)
            clearNfcTag()
            nfcListener?.onNfcOperationFail("Tag connection lost")
            return false
        } catch (e: Exception) {
            clearNfcTag()
            nfcListener?.onNfcOperationFail(e.message ?: "Writing to NFC tag failed")
            return false
        }
    }

    private fun <T>enableNFCInForeground(nfcAdapter: NfcAdapter, activity: Activity, classType : Class<T>) {

        val pendingIntent = PendingIntent.getActivity(activity, 0, Intent(activity,classType).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE)
        val nfcIntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        val filters = arrayOf(nfcIntentFilter)
        val techLists = arrayOf(arrayOf(Ndef::class.java.name), arrayOf(NdefFormatable::class.java.name))

        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
    }

    private fun disableNFCInForeground(nfcAdapter: NfcAdapter, activity: Activity) {
        nfcAdapter.disableForegroundDispatch(activity)
    }

    private fun setConnectionState(state: Boolean) {
        this.isConnected = state
        when(state) {
            true -> nfcListener?.onNfcTagConnected()
            false -> nfcListener?.onNfcTagDisconnected()
        }
    }

    private fun pollForMailboxWritten(nTag: NfcV?)
    {
        if(isPolling) {
            return
        }
        isPolling = true
        ftmPollTimer = Timer()
        ftmPollTimer?.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                try {
                    nTag?.let {
                        if(it.isConnected) {
                            // read MB_CTRL_Dyn register
                            val mbCtrlDynRegisterRequest =
                                byteArrayOf((0x02).toByte(), (0xAD).toByte(), (0x02).toByte(), (0x0D).toByte())

                            val result =
                                it.transceive(mbCtrlDynRegisterRequest)

                            // Check if flag is set: HOST_PUT_MSG
                            if((result[1] and (0x02).toByte()) != (0).toByte())
                            {
                                stopFtmPollTimer()
                                readFtmMailbox(it)
                                pollForMailboxWritten(it)
                            }
                        }
                        else
                        {
                            stopFtmPollTimer()
                            it.close()
                        }
                    }

                } catch(tle: TagLostException) {
                    stopFtmPollTimer()
                    clearNfcTag()
                    nfcListener?.onNfcTagDisconnected()
                } catch (e: Exception) {
                    stopFtmPollTimer()
                    clearNfcTag()
                    nfcListener?.onNfcOperationFail(e.message ?: "Polling for mailbox written failed")
                }
            }
        }, 50, 100)
    }

    private fun readFtmMailbox(nTag: NfcV?)
    {
        // read MB_LEN_Dyn register
        val mbLenDynRegisterRequest =
            byteArrayOf((0x02).toByte(), (0xAB).toByte(), (0x02).toByte())

        val result =
            nTag?.transceive(mbLenDynRegisterRequest)

        if(result != null) {
            if (result[1] > (0).toByte()) {

                // read ftm mailbox message
                val ftmMessageRequest =
                    byteArrayOf((0x02).toByte(), (0xAC).toByte(), (0x02).toByte(), (0x00).toByte(), result[1])

                val ftmMessageResult =
                    nTag.transceive(ftmMessageRequest)

                val resultString = ftmMessageResult?.decodeToString() ?: ""
                if(resultString.isNotEmpty()) {
                    nfcListener?.onNfcOperationSuccess("Message read from tag: $resultString")
                }
            }
        }
    }

    private fun stopFtmPollTimer() {
        ftmPollTimer?.cancel()
        ftmPollTimer = null
        isPolling = false
    }

    private fun clearNfcTag() {
        try {
            nfcTag?.let {
                if(it.isConnected)
                {
                    it.close()
                }
            }
            nfcTag = null
        } catch (e: Exception) {
            return
        }
    }

    private fun formatResult(result: ByteArray): String {
        var resultString = ""
        for (byte in result) {
            resultString += byte.toString()
        }
        return resultString
    }

    interface NfcListener {
        fun onNfcTagConnected()
        fun onNfcTagDisconnected()
        fun onNfcOperationFail(message: String)
        fun onNfcOperationSuccess(message: String)
    }

    init {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    }
}