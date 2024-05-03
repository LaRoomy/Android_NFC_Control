package com.example.nfccontrol

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NFCController.NfcListener {

    // UI elements
    private var constraintLayout: ConstraintLayout? = null
    private var dataInputEditText: EditText? = null
    private var notificationTextView: TextView? = null
    private var connectionStatusTextView: TextView? = null
    private var connectionStatusLayout: ConstraintLayout? = null
    private var enterDataSectionButton: Button? = null
    private var sendNowButton: Button? = null

    // Local data storage
    private val localDataStorage = LocalDataStorage(this)

    // NFC controller
    private lateinit var nfcController: NFCController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI elements
        dataInputEditText = findViewById(R.id.main_activity_name_edit_text)
        constraintLayout = findViewById(R.id.main_activity_enter_data_layout)
        notificationTextView = findViewById(R.id.main_activity_notification_text_view)
        connectionStatusTextView = findViewById(R.id.main_activity_connection_status_text_view)
        connectionStatusLayout = findViewById(R.id.main_activity_connection_status_layout)
        enterDataSectionButton = findViewById(R.id.main_activity_data_section_button)
        sendNowButton = findViewById(R.id.main_activity_send_now_button)

        // Set up the enter data section button click listener
        enterDataSectionButton?.apply {
            setOnClickListener {
                if(constraintLayout?.visibility == View.VISIBLE) {
                    constraintLayout?.visibility = View.GONE
                    enterDataSectionButton?.text = getString(R.string.main_activity_enter_data_button_text)
                }
                else {
                    constraintLayout?.visibility = View.VISIBLE
                    enterDataSectionButton?.text = getString(R.string.main_activity_enter_data_button_section_open_text)
                }
            }
        }

        // Set up the send now button click listener
        sendNowButton?.apply {
            setOnClickListener {
                nfcController.writeNFCMessage(dataInputEditText?.text.toString(), intent)
            }
        }

        // Set up the notification text view click listener
        notificationTextView?.apply {
            setOnClickListener {
                notificationTextView?.text = ""
            }
        }

        // Set up the edit text input listener
        dataInputEditText?.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    // Save the data to the local storage
                    CoroutineScope(Dispatchers.IO).launch {
                        localDataStorage.saveStringData(NFC_DATA_KEY, dataInputEditText?.text.toString())
                    }
                }
           })

        // Retrieve the data from the local storage and set it as edit text
        CoroutineScope(Dispatchers.IO).launch {
            val savedEditContent = localDataStorage.getStringData(NFC_DATA_KEY)
            if(savedEditContent.isNotEmpty()) {
                dataInputEditText?.setText(savedEditContent)
            }
        }

        // Create NFCController and Register callback
        nfcController = NFCController(this)
        nfcController.setNfcListener(this)
    }

    override fun onResume() {
        super.onResume()
        nfcController.enableNFC(this, MainActivity::class.java)
    }

    override fun onPause() {
        super.onPause()
        nfcController.disableNFC(this)
    }

    private fun setUIConnectionState(state: Boolean) {
        runOnUiThread {
            connectionStatusTextView?.text =
                if (state) getString(R.string.main_activity_connection_status_connected_text) else getString(
                    R.string.main_activity_connection_status_disconnected_text
                )
            connectionStatusLayout?.background = AppCompatResources.getDrawable(
                this,
                if (state) R.drawable.connection_state_layout_connected_background else R.drawable.connection_state_layout_disconnected_background
            )
            if(!state)
            {
                notifyUser(UserNotificationType.SUCCESS, getString(R.string.main_activity_initial_text_content))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        /*
        *   This is called when a tag is discovered by the system. Here data is directly written to the tag.
        * */
        nfcController.writeNFCMessage(dataInputEditText?.text.toString(), intent)
    }

    override fun onNfcTagConnected() {
        setUIConnectionState(true)
    }

    override fun onNfcTagDisconnected() {
        setUIConnectionState(false)
    }

    override fun onNfcOperationFail(message: String) {
        notifyUser(UserNotificationType.ERROR, message)
    }

    override fun onNfcOperationSuccess(message: String) {
        notifyUser(UserNotificationType.SUCCESS, message)
    }

    private fun notifyUser(userNotificationType: Int, message: String) {
        runOnUiThread {
            notificationTextView?.setTextColor(
                when (userNotificationType) {
                    UserNotificationType.ERROR -> getColor(R.color.notificationTextColor_Error)
                    UserNotificationType.SUCCESS -> getColor(R.color.notificationTextColor_Success)
                    UserNotificationType.INFO -> getColor(R.color.notificationTextColor_Info)
                    else -> getColor(R.color.white)
                }
            )
            notificationTextView?.text = message
        }
    }
}