package com.example.cornelltwinkles

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.text.Editable
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.cornelltwinkles.helpers.Indiv
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_first.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var database : DatabaseReference
    private lateinit var indiv : Indiv
    private var userState = "HOME" // HOME, CLEARED, REJECTED, WARNING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        database = FirebaseDatabase.getInstance().getReference("test")
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)


        fab.setOnClickListener { view ->

            //open MHealth
            val webview : WebView = findViewById(R.id.wv)
            webview.webViewClient = WebViewClient()
            webview.settings.javaScriptEnabled = true
            webview.loadUrl("https://mycayugahealth.cayugamed.org/Phm-PhmHome.HomePage.WR.mthr?hcis=TOCGBL.LIVE&application=PHM")
            webview.loadUrl("javascript:document.getElementsByName('userid').value = 'maxypants2010'");
            webview.loadUrl("javascript:document.getElementsByName('password').value = '325051860'");
            //webview.loadUrl("javascript:document.forms['signonform'].submit()");

            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }


    }



    override fun onResume() {
        super.onResume()

        loadData(name_field)
        loadData(email_field)
        loadData(phone_field)

        var tagDetected : IntentFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        var ndefDetected : IntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        var techDetected : IntentFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        var nfcIntentFilter =
            arrayOf(techDetected, tagDetected, ndefDetected)

        var pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            nfcIntentFilter,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)

        val name = name_field.text.toString()
        val email = email_field.text.toString()
        val phone = phone_field.text.toString()
        saveData(name_field)
        saveData(email_field)
        saveData(phone_field)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        patchTag(tag)
        tag?.let { readFromNFC(it, intent) }
    }

    fun patchTag(oTag: Tag?): Tag? {
        if (oTag == null) return null
        val sTechList = oTag.techList
        val oParcel: Parcel
        val nParcel: Parcel
        oParcel = Parcel.obtain()
        oTag.writeToParcel(oParcel, 0)
        oParcel.setDataPosition(0)
        val len = oParcel.readInt()
        var id: ByteArray? = null
        if (len >= 0) {
            id = ByteArray(len)
            oParcel.readByteArray(id)
        }
        val oTechList = IntArray(oParcel.readInt())
        oParcel.readIntArray(oTechList)
        val oTechExtras = oParcel.createTypedArray(Bundle.CREATOR)
        val serviceHandle = oParcel.readInt()
        val isMock = oParcel.readInt()
        val tagService: IBinder?
        tagService = if (isMock == 0) {
            oParcel.readStrongBinder()
        } else {
            null
        }
        oParcel.recycle()
        var nfca_idx = -1
        var mc_idx = -1
        for (idx in sTechList.indices) {
            if (sTechList[idx] === NfcA::class.java.name) {
                nfca_idx = idx
            } else if (sTechList[idx] === MifareClassic::class.java.name) {
                mc_idx = idx
            }
        }
        if (nfca_idx >= 0 && mc_idx >= 0 && oTechExtras!![mc_idx] == null) {
            oTechExtras[mc_idx] = oTechExtras[nfca_idx]
        } else {
            return oTag
        }
        nParcel = Parcel.obtain()
        nParcel.writeInt(id!!.size)
        nParcel.writeByteArray(id)
        nParcel.writeInt(oTechList.size)
        nParcel.writeIntArray(oTechList)
        nParcel.writeTypedArray(oTechExtras, 0)
        nParcel.writeInt(serviceHandle)
        nParcel.writeInt(isMock)
        if (isMock == 0) {
            nParcel.writeStrongBinder(tagService)
        }
        nParcel.setDataPosition(0)
        val nTag = Tag.CREATOR.createFromParcel(nParcel)
        nParcel.recycle()
        return nTag
    }
    private fun readFromNFC(tag: Tag, intent: Intent) {


        /*
        if (button_first.isVisible) {
            //findNavController(R.id.nav_host_fragment).navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

         */


        indiv = Indiv()

        val name = findViewById<EditText>(R.id.name_field).text.toString()
        val email = findViewById<EditText>(R.id.email_field).text.toString()
        val phone = findViewById<EditText>(R.id.phone_field).text.toString()

        //Toast.makeText(applicationContext, name, Toast.LENGTH_LONG).show()

        if (name == "" || email == "" || phone == "") {
            Toast.makeText(applicationContext, "Name, email, or phone fields are empty", Toast.LENGTH_LONG).show()
        } else if (!indiv.matchEmail(email)) {
            Toast.makeText(applicationContext, "Enter valid Cornell email", Toast.LENGTH_LONG).show()
        } else if (!indiv.matchNumber(phone)){
            Toast.makeText(applicationContext, "Enter valid phone number", Toast.LENGTH_LONG).show()
        }
        else {


            when (userState) {

                "HOME" -> {findViewById<View>(R.id.pane).setBackgroundColor(Color.parseColor("#FFFFFF"))
                    userState = "CLEARED"}
                else -> {findViewById<View>(R.id.pane).setBackgroundColor(Color.parseColor("#FF2FFF"))
                    userState = "HOME"}
            }

            indiv.setName(name)
            indiv.setEmail(email)
            indiv.setPhoneNumber(phone)
            //indiv.setTimestamp()
            val current = LocalDateTime.now()

            val formatter = DateTimeFormatter.BASIC_ISO_DATE
            val formatted = current.format(formatter)
            database.child("party-2").child("$formatted").child(indiv.getName()).setValue(indiv)
        }
        /*
        var count : Long = 0
        var valueEventListener = object: ValueEventListener {

            override fun onDataChange(dataSnapshot:DataSnapshot){
                count = dataSnapshot.child("bathroom-2").child("Active Users").value as Long
                //Toast.makeText(applicationContext, "${count}", Toast.LENGTH_LONG*10000).show()
                database.child("bathroom-2").child("Active Users").setValue(count+1)
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }
        database.addListenerForSingleValueEvent(valueEventListener)

         */


        /*
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                val ndefMessage = ndef.ndefMessage
                if (ndefMessage != null) {
                    /*String message = new String(ndefMessage.getRecords()[0].getPayload());
                    Log.d(TAG, "NFC found.. "+"readFromNFC: "+message );
                    tvNFCMessage.setText(message);*/
                    val messages =
                        intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                    if (messages != null) {
                        val ndefMessages =
                            arrayOfNulls<NdefMessage>(messages.size)
                        for (i in messages.indices) {
                            ndefMessages[i] = messages[i] as NdefMessage
                        }
                        val record = ndefMessages[0]!!.records[0]
                        val payload = record.payload
                        val text = String(payload)
                        //tvNFCMessage.setText(text)
                        //dotloader.setVisibility(View.GONE)
                        Log.e("tag", "vahid  -->  $text")
                        ndef.close()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Not able to read from NFC, Please try again...",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    try {
                        format.connect()
                        val ndefMessage = ndef!!.ndefMessage
                        if (ndefMessage != null) {
                            val message =
                                String(ndefMessage.records[0].payload)

                            ndef.close()
                        } else {
                            Toast.makeText(
                                this,
                                "Not able to read from NFC, Please try again...",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(this, "hEy Boy i'LL makE iT SimpLe foR U", Toast.LENGTH_LONG).show()
                    findNavController(R.id.nav_host_fragment).navigate(R.id.action_FirstFragment_to_SecondFragment)
                    //writeNewUser("asd","asds","asdasd")
                    indiv = Indiv()
                    indiv.setTimestamp()
                    database.child("bathroom-2").child(indiv.getEntered().toString()).push().setValue(indiv)

                    var count : Int = 0
                    val valueEventListener = object: ValueEventListener {

                        override fun onDataChange(dataSnapshot:DataSnapshot){
                            count = (dataSnapshot.child("bathroom-2").getValue() as Int)
                        }
                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    }
                    database.child("bathroom-2").setValue(count+1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

         */
    }

    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

     */

    private fun saveData(et : EditText) {

        val insertedText = et.text.toString()

        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.apply {
            putString(et.id.toString(), insertedText)
        }.apply()
    }

    private fun loadData(et : EditText){
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val savedString = sharedPreferences.getString(et.id.toString(),null)
        et.setText(savedString)
    }

}
