package com.example.youbikea

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    lateinit var executorService: ExecutorService
    var GetUbikeStationJsonStr = ""
    var snaList: MutableList<String> = ArrayList()
    var snaListFilter: MutableList<String> = ArrayList()
    var sareaList: MutableList<String> = ArrayList()
    var sareaListFilter: MutableList<String> = ArrayList()
    lateinit var searchView: SearchView
    private var adapter: RecyclerView.Adapter<*>? = null
    lateinit var recyclerView: RecyclerView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maina)
        searchView = findViewById(R.id.search_view)
        searchView.setIconifiedByDefault(false)
        searchView.setSubmitButtonEnabled(false)
        searchView.setQueryHint("搜尋站點")
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        StartActAsyncTask()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (TextUtils.isEmpty(newText)) {
                    snaListFilter = ArrayList(snaList)
                    sareaListFilter = ArrayList(sareaList)
                } else {
                    sareaListFilter.clear()
                    snaListFilter.clear()
                    for (i in sareaList.indices) {
                        if (sareaList[i].indexOf(newText.trim()) != -1) {
                            sareaListFilter.add(sareaList[i])
                            snaListFilter.add(snaList[i])
                        }
                    }
                }
                adapter = MyAdpater()
                recyclerView.setAdapter(adapter)
                return false
            }
        })
    }

    private fun StartActAsyncTask() {
        executorService = Executors.newFixedThreadPool(1)
        executorService.submit(Runnable {
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
        var StationObject: JSONObject
        val StationArray: JSONArray
        if (GetUbikeStationJsonStr != "") {
            try {
                snaList.clear()
                sareaList.clear()
                StationArray = JSONArray(GetUbikeStationJsonStr)
                i = 0
                while (i < StationArray.length()) {
                    StationObject = StationArray.getJSONObject(i)
                    snaList.add(StationObject.getString("sna").replace("YouBike2.0_", ""))
                    sareaList.add(StationObject.getString("sarea"))
                    i++
                }
                snaListFilter = ArrayList(snaList)
                sareaListFilter = ArrayList(sareaList)
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    adapter = MyAdpater()
                    recyclerView!!.adapter = adapter
                }, 100)
            } catch (e: Exception) {
            }
        }
        try {
            if (!executorService!!.isShutdown) {
                executorService!!.shutdownNow()
            }
        } catch (e: Exception) {
        }
    }

    private inner class MyAdpater : RecyclerView.Adapter<MyAdpater.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val menuItemLayoutView = LayoutInflater.from(parent.context).inflate(
                R.layout.mylistview, parent, false
            )
            return ViewHolder(menuItemLayoutView)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position % 2 == 0) {
                holder.layRoot.setBackgroundColor(0xFFFFFFFF.toInt())
            } else {
                holder.layRoot.setBackgroundColor(0xFFF0FFF0.toInt())
            }
            holder.txtCity.text = "台北市"
            holder.txtArea.text = sareaListFilter[position]
            holder.txtTitle.text = snaListFilter[position]
        }

        override fun getItemCount(): Int {
            return snaListFilter.size
        }
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            lateinit var layRoot: LinearLayout
            var txtCity: TextView
            var txtArea: TextView
            var txtTitle: TextView

            init {
                layRoot = itemView.findViewById(R.id.layRoot)
                txtCity = itemView.findViewById(R.id.txtCity)
                txtArea = itemView.findViewById(R.id.txtSarea)
                txtTitle = itemView.findViewById(R.id.txtSna)
            }
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
            con?.doOutput = false
            con.connectTimeout = connectTimeout
            con.readTimeout = readTimeout
            val inA: Reader = InputStreamReader(con.inputStream)
            val bufferedreader = BufferedReader(inA)
            val stringBuilder = StringBuilder()
            var stringReadLine: String? = null
            stringReadLine = bufferedreader.readLine()
            while (stringReadLine != null) {
                stringBuilder.append(stringReadLine + "\n")
                stringReadLine = bufferedreader.readLine()
            }
            qResult = stringBuilder.toString()
        } catch (e: IOException) {
            qResult = ""
            e.printStackTrace()
        } finally {
            con?.disconnect()
        }
        return qResult
    }
}