package com.accushield.sampleapp


import android.app.AlertDialog
import android.app.Dialog
import android.content.*
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.text.Editable
import android.text.Html
import android.text.method.DigitsKeyListener
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.accushield.sampleapp.UsbService
import kotlinx.android.synthetic.main.yesno_bottom_buttonlayout.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Double.parseDouble
import java.lang.ref.WeakReference
//import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment


class MainActivity : AppCompatActivity() {
    //protected var mViewBinding2: ActivityMainBinding? = null
    private var usbService: UsbService? = null
    private var mHandler: MyHandler? = null
    private var img: ImageView? = null
    var dataTotal: String = ""

    /*override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
            mViewBinding2 = ActivityMainBinding.inflate(inflater, container, false)
            mViewBinding2?.frag = this
            setFilters() // Start listening notifications from UsbService
            startService(
                    UsbService::class.java,
                    usbConnection,
                    null
            ) // Start UsbService(if it was not started before) and Bind it
            return mViewBinding2?.root
    }*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val layoutInflater:LayoutInflater = LayoutInflater.from(applicationContext)
        val view: View  = layoutInflater.inflate(R.layout.activity_main, null)
        //mViewBinding2 = ActivityMainBinding.inflate(inflater, container, false)
        //mViewBinding2?.frag = this
        setFilters() // Start listening notifications from UsbService
        startService(
                UsbService::class.java,
                usbConnection,
                null
        ) // Start UsbService(if it was not started before) and Bind it
        //return mViewBinding2?.root
        mHandler = MyHandler()
        findViewById<Button>(R.id.btn_scan).setOnClickListener {
            findViewById<TextView>(R.id.vis_signin_et_temp)?.text = ""
            findViewById<TextView>(R.id.vis_signin_et_data).text = ""
            val data = "GetT\r\n"
            dataTotal = ""
            if (usbService != null) { // if UsbService was correctly binded, Send data
                usbService!!.write(data.toByteArray())
                Log.d("Jake", "GetT should be transmitted")
            }
        }
    }
    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbService.ACTION_USB_PERMISSION_GRANTED -> Toast.makeText(
                        context,
                        "USB Ready",
                        Toast.LENGTH_SHORT
                ).show()
                UsbService.ACTION_USB_PERMISSION_NOT_GRANTED -> Toast.makeText(
                        context,
                        "USB Permission not granted",
                        Toast.LENGTH_SHORT
                ).show()
                UsbService.ACTION_NO_USB -> Toast.makeText(
                        context,
                        "No USB connected",
                        Toast.LENGTH_SHORT
                ).show()
                UsbService.ACTION_USB_DISCONNECTED -> Toast.makeText(
                        context,
                        "USB disconnected",
                        Toast.LENGTH_SHORT
                ).show()
                UsbService.ACTION_USB_NOT_SUPPORTED -> Toast.makeText(
                        context,
                        "USB device not supported",
                        Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private val usbConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            usbService = (arg1 as UsbService.UsbBinder).getService()
            usbService?.setHandler(mHandler)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            usbService = null
        }
    }

    private fun startService(
            service: Class<*>,
            serviceConnection: ServiceConnection,
            extras: Bundle?
    ) {
        if (!UsbService.SERVICE_CONNECTED) {
            val startService = Intent(applicationContext, service)
            if (extras != null && !extras.isEmpty) {
                val keys = extras.keySet()
                for (key in keys) {
                    val extra = extras.getString(key)
                    startService.putExtra(key, extra)
                }
            }
            applicationContext?.startService(startService)
        }
        val bindingIntent = Intent(applicationContext, service)
        applicationContext?.bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setFilters() {
        val filter = IntentFilter()
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(UsbService.ACTION_NO_USB)
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED)
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED)
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED)
        applicationContext?.registerReceiver(mUsbReceiver, filter)
    }

    inner class MyHandler : Handler() {
        var mActivity: WeakReference<MainActivity>? = null

        public fun MyHandler(activity: MainActivity) {
            mActivity = WeakReference<MainActivity>(activity)
        }

        fun errorHandler(data: String){
            var dialog: Dialog? = null
            if (data.contains("S-")) {
                Log.d("Jake", "Error message")
                val activity = this@MainActivity
                val inflater = activity.layoutInflater
                val v = inflater.inflate(R.layout.d_temp_error, null)
                val builder = AlertDialog.Builder(activity)

                builder.setView(v)
                    .setPositiveButton("OK", null)

                when(data) {
                    "S-3" -> {
                        (v.findViewById(R.id.txt_message) as TextView).text = getString(R.string.error3)
                        (v.findViewById(R.id.txt_title) as TextView).text = "A temperature was not detected. (3)"
                    }
                    "S-4" -> {
                        (v.findViewById(R.id.txt_message) as TextView).text =
                            getString(R.string.error4_5)
                        (v.findViewById(R.id.txt_title) as TextView).text =
                            "Move closer to the kiosk. (4)"
                        (v.findViewById(R.id.temp_image) as ImageView).visibility =
                            View.GONE
                    }
                    "S-5" -> {
                            (v.findViewById(R.id.txt_message) as TextView).text =
                                getString(R.string.error4_5)
                            (v.findViewById(R.id.txt_title) as TextView).text =
                                "Move back from the kiosk. (5)"
                            (v.findViewById(R.id.temp_image) as ImageView).visibility =
                                View.GONE
                    }
                    "S-6" -> {
                        (v.findViewById(R.id.txt_message) as TextView).text =
                            getString(R.string.error6_8_9)
                        (v.findViewById(R.id.txt_title) as TextView).text =
                            "Thermometer start up error. (6)"
                    }
                    "S-8" -> {
                        (v.findViewById(R.id.txt_message) as TextView).text =
                            getString(R.string.error6_8_9)
                        (v.findViewById(R.id.txt_title) as TextView).text =
                            "Ambient temperature too high. (8)"
                    }
                    "S-9" -> {
                        (v.findViewById(R.id.txt_message) as TextView).text =
                            getString(R.string.error6_8_9)
                        (v.findViewById(R.id.txt_title) as TextView).text =
                            "Ambient temperature too low. (9)"
                    }
                    else -> {
                        (v.findViewById(R.id.txt_message) as TextView).text =
                            getString(R.string.error6_8_9)
                        (v.findViewById(R.id.txt_title) as TextView).text =
                            "No data recieved."
                    }
                }



                dialog = builder.create()
                dialog?.setCancelable(false)
                dialog?.setCanceledOnTouchOutside(false)
                dialog?.show()
                dialog?.window?.setLayout(1250, 650)

                val btn1 = dialog?.findViewById<Button>(android.R.id.button1)
                btn1?.textSize = 25f
                btn1?.setOnClickListener {
                    dataTotal = ""
                    dialog?.dismiss()
                }
            }
        }
        override fun handleMessage(msg: Message) {
            Log.d("Jake", "Message Recieved")
            var dialog: Dialog? = null
            when (msg.what) {
                UsbService.MESSAGE_FROM_SERIAL_PORT -> {
                    val data = msg.obj as String
                    if ((dataTotal.isEmpty()) || (dataTotal.length < 4)) {
                        if (data.contains("S-")) {
                            errorHandler(data)
                        }
                        else {
                            Log.d("Jake", data)
                            dataTotal += data
                            Log.d("Jake", dataTotal)
                        }
                    } else {
                        if (dataTotal!!.contains("S")) {
                            Log.d("JakeTest", "Error message")
                            dataTotal = ""
                            errorHandler(dataTotal)
                        } else if (dataTotal.contains("T")) { // T369 X255 Y555 Z367 S0
                            Log.d("JakeTest", dataTotal)
                            findViewById<TextView>(R.id.vis_signin_et_data).text = dataTotal
                            val temp = dataTotal.split(" ").toTypedArray()[0].split("T")
                                .toTypedArray()[1]
                            dataTotal = ""
                            if (temp.isEmpty()) {
                                Log.d("Jake", "Empty String")
                            } else {
                                val temperature = temp.toFloat() / 10
                                //if (temperatureSettings?.temperatureType == "f") {
                                    dataTotal = ""
                                    updateUIIntegrated(((temperature * 1.8) + 32).toFloat())
                                    Log.d("Jake", ((temperature * 1.8) + 32).toString())
                               /* } else {
                                    dataTotal = ""
                                    updateUIIntegrated(temperature)
                                }*/
                            }
                        }
                    }
                }
            }
        }
        fun updateUIIntegrated(temp: Float) {
            try {
                Log.d("Captured Temp", ":$temp")
                this@MainActivity?.runOnUiThread {
                    findViewById<TextView>(R.id.vis_signin_et_temp)?.text = temp.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("error", ":" + e.message)
            }

        }
    }
}
