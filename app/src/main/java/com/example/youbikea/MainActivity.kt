package com.example.youbikea

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {
    private var executorService: ExecutorService? = null
    var GetUbikeStationJsonStr = ""
    var btn: Button? = null
    var snaList = ArrayList<String>()
    var sareaList = ArrayList<String>()
    var txtMsg: TextView? = null
    var edtQuery: EditText? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn = findViewById(R.id.btnQuery)
        edtQuery = findViewById(R.id.edtQueryA)
        txtMsg = findViewById(R.id.txtMsg)
        txtMsg!!.setMovementMethod(ScrollingMovementMethod())
        btn!!.setOnClickListener(View.OnClickListener {
            txtMsg!!.setText("")
            StartActAsyncTask()
        })
    }
    private fun StartActAsyncTask() {
        executorService = Executors.newFixedThreadPool(1)
        executorService!!.submit(Runnable {
            try {
                GetUbikeStationJsonStr =
                    QueryUbikeStation(
          "https://tcgbusfs.blob.core.windows.net/dotapp/youbike/v2/youbike_immediate.json"
                    )
            } catch (e: Exception) {
            }
            doOnUiPost()
        })
    }
    private fun doOnUiPost() {
        var i: Int
        var sna: String
        var sarea: String
        var StationObject: JSONObject
        val StationArray: JSONArray
        var str = ""
        val n: Int
        val query = edtQuery!!.text.toString()
        if (GetUbikeStationJsonStr != "") {
            try {
                snaList.clear()
                sareaList.clear()
                StationArray = JSONArray(GetUbikeStationJsonStr)
                i = 0
                while (i < StationArray.length()) {
                    StationObject = StationArray.getJSONObject(i)
                    //snaList.add(StationObject.getString("sna"));
                    //sareaList.add(StationObject.getString("sarea"));
                    if (query.trim { it <= ' ' } == StationObject.getString("sarea")) {
                        str = """$str${StationObject.getString("sarea")}			${
                            StationObject.getString("sna")
                        }
"""
                    }
                    i++
                }
                txtMsg!!.text = str.replace("YouBike2.0_", "")
            } catch (e: Exception) {
                n = 0
            }
        }
        try {
            if (!executorService!!.isShutdown) {
                executorService!!.shutdownNow()
            }
        } catch (e: Exception) {
        }
    }
    fun QueryUbikeStation(queryString: String?): String {
        var con: HttpURLConnection? = null
        var qResult = ""
        val connectTimeout = 20000
        val readTimeout = 20000
        try {
            val url = URL(queryString)
            con = if (url.protocol.lowercase(Locale.getDefault()) == "https") {
                url.openConnection() as HttpsURLConnection
            } else {
                url.openConnection() as HttpURLConnection
            }
            con.doInput = true
            con!!.doOutput = false
            con.connectTimeout = connectTimeout
            con.readTimeout = readTimeout
            val inA: Reader = InputStreamReader(con.inputStream)
            val bufferedreader = BufferedReader(inA)
            val stringBuilder = StringBuilder()
            var stringReadLine: String? = null
            while (bufferedreader.readLine().also { stringReadLine = it } != null) {
                stringBuilder.append(stringReadLine)
            }
            qResult = stringBuilder.toString()
        } catch (e: IOException) {
            qResult = ""
            e.printStackTrace()
        } finally {
            con!!.disconnect()
        }
        return qResult
    }
}