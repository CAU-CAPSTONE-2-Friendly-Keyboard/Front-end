package com.example.friendlykeyboard.activities

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.friendlykeyboard.BuildConfig
import com.example.friendlykeyboard.databinding.ActivityChattingBinding
import com.example.friendlykeyboard.retrofit_util.Account
import com.example.friendlykeyboard.retrofit_util.Chat
import com.example.friendlykeyboard.retrofit_util.RetrofitClient
import com.example.friendlykeyboard.utils.ChattingRVAdapter
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class ChattingActivity : AppCompatActivity() {
    private lateinit var binding : ActivityChattingBinding
    private lateinit var chattingRVAdapter: ChattingRVAdapter
    private lateinit var spf : SharedPreferences
    private val service = RetrofitClient.getApiService()
    private var chattingList = arrayListOf<Array<Any>>()
    private var missionText : String = ""
    private val missionCount = 3
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChattingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        spf = getSharedPreferences("setting", 0)
        // 채팅중이라면 비속어 test x
        spf.edit().putBoolean("chatting", true).apply()

        // chatting 내역 view로 가져오기
        runBlocking {
            initChatListData()
        }
        // editText 엔터 시 처리
        initListener()
        // 본 activity를 오도록 했던 욕설
        val curse = intent.getStringExtra("curse")!!
        // chatGPT prompt
        val ask = "이라는 문장을 비속어 및 욕설 없이 50토큰 이내로 완화해서 대체해줘"

        runBlocking {
            addAndNotifyAdapter(2, "미션 생성 중...")
        }
        // 영문 모드일 시 고려
        if (spf.getInt("stageNumber", 0) == 3) {
            loadMissionText("Sorry for swearing")
        }
        else{
            callChatGPTAPI(curse, ask)
        }

    }

    private fun initRecyclerView(chatList : ArrayList<Array<Any>>){
        chattingRVAdapter = ChattingRVAdapter(chatList)
        binding.rvChatting.adapter = chattingRVAdapter
        val manager = LinearLayoutManager(applicationContext)
        manager.stackFromEnd = true
        binding.rvChatting.setHasFixedSize(true)    // view changing size 고정하여 리소스 save
        binding.rvChatting.layoutManager = manager
        binding.rvChatting.layoutManager!!.scrollToPosition(chattingRVAdapter.itemCount - 1)
    }

    private suspend fun initChatListData(){
        val id = getSharedPreferences("cbAuto", 0).getString("id", "")!!

        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            try {
                val response = service.getChatList(Account(id, "?"))

                if (response.isSuccessful) {
                    val result = response.body()!!
                    val idList = result.idList
                    val textList = result.textList
                    val dateList = result.dateList

                    chattingList = arrayListOf<Array<Any>>().apply {
                        for (i in idList.indices) {
                            add(arrayOf(idList[i], textList[i], dateList[i]))
                        }
                    }

                    //Recyclerview 초기화
                    initRecyclerView(chattingList)

                } else {
                    // 통신이 실패한 경우
                    Log.d("ChattingActivity", response.message())
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            applicationContext,
                            "조금 뒤 다시 시도해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.d("ChattingActivity", "Connection Error")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        applicationContext,
                        "서버와의 통신이 실패하였습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        initRecyclerView(chattingList)
    }

    private fun loadMissionText(text : String){
        missionText = text
        val text = "[" + missionText + "]를 " + missionCount + "번 입력하세요 !"

        runBlocking {
            addAndNotifyAdapter(2, text)
        }
    }

    private fun initListener(){
        binding.textInputEditText.setOnEditorActionListener { v, actionId, event ->

            //엔터시 actionId = 0
            //EditorInfo.IME_ACTION_SEND = 4
            // 엔터키를 눌렀을 때 실행할 동작을 여기에 작성합니다.
            if (actionId == 0 && binding.textInputEditText.text.toString().isNotEmpty()) {

                val enteredText = binding.textInputEditText.text.toString()
                binding.textInputEditText.setText("")

                //입력된 text 서버에 저장 및 recyclerview에 notify하여 view 변경
                runBlocking {
                    addAndNotifyAdapter(1, enteredText)
                }

                // 과제 성공했는지 검사
                checkMissionAccomplished(enteredText)

                true
            } else false
        }
    }

    private fun checkMissionAccomplished(enteredText : String){
        if (enteredText.equals(missionText)){
            count++
            if (count == missionCount - 1){
                runBlocking {
                    addAndNotifyAdapter(2, "마지막 한 번!")
                }
            }
            if (count == missionCount){
                count = 0
                initStage()
                CoroutineScope(Dispatchers.Main).launch{
                    delay(500)
                    Toast.makeText(applicationContext, "미션 성공", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun addAndNotifyAdapter(id : Int, text : String) {
        val accountID = getSharedPreferences("cbAuto", 0).getString("id", "")!!
        val currentTime = currentTime()
        val chat = Chat(accountID, id, text, currentTime)

        withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
            try {
                val response = service.saveChat(chat)
                if (response.isSuccessful) {
                    Log.d("ChattingActivity", "Saved successfully.")
                } else {
                    // 통신이 실패한 경우
                    Log.d("ChattingActivity", response.message())
                }
            } catch (e: Exception) {
                // 통신 실패 (인터넷 끊김, 예외 발생 등 시스템적인 이유)
                e.printStackTrace()
            }
        }

        chattingList.add(arrayOf(id, text, currentTime))

        //recyclerview에 notify하여 view 변경
        binding.rvChatting.adapter!!.notifyDataSetChanged()
        binding.rvChatting.layoutManager!!.scrollToPosition(chattingRVAdapter.itemCount - 1)
    }

    private fun currentTime() : String {
        /*
        val calendar: Calendar = Calendar.getInstance() // 캘린더 객체 인스턴스 calendar
        val dateFormat = SimpleDateFormat("HH:mm") // SimpleDataFormat 이라는 날짜와 시간을 출력하는 객체 생성, hh을 HH로 변경했더니 24시각제로 바뀜
        return dateFormat.format(calendar.time) // 캘린더 날짜시간 값을 가져와서 문자열인 datatime 으로 변환함
        */
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN).format(Date())
    }

    private fun initStage(){
        spf.edit().putInt("stageNumber", 0).apply()

        //기능설정 색깔 복구
        spf.edit().putInt("settingAlarmColor", Color.parseColor("#000000")).apply()
        spf.edit().putInt("settingInvisibleColor", Color.parseColor("#000000")).apply()
        spf.edit().putInt("settingEnglishColor", Color.parseColor("#000000")).apply()
        spf.edit().putInt("settingRandomColor", Color.parseColor("#000000")).apply()
        spf.edit().putInt("settingCorrectColor", Color.parseColor("#000000")).apply()

        // 키보드 폰트색 복구
        spf.edit().putInt("keyboardFontColor", spf.getInt("tempKeyboardFontColor", 0)).apply()
    }

    private fun callChatGPTAPI(curse: String, ask: String){
        val client = OkHttpClient.Builder()
            .connectTimeout(100, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS)
            .writeTimeout(100, TimeUnit.SECONDS)
            .build()

        Log.d("curse", curse)
        // 교정 2단계 이상일 때만
        if (spf.getInt("stageNumber", 0) >= 2){
            //okhttp

            val arr = JSONArray()
            val userMsg = JSONObject()
            try {
                //유저 메세지
                userMsg.put("role", "user")
                userMsg.put("content", curse + ask)
                //array에 담아서 한번에
                arr.put(userMsg)
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
            val obj = JSONObject()
            try {
                //모델명 변경
                obj.put("model", "gpt-3.5-turbo")
                obj.put("messages", arr)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = obj.toString().toRequestBody(JSON)
            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + BuildConfig.MY_KEY)
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        var jsonObject: JSONObject? = null
                        try {
                            jsonObject = JSONObject(response.body!!.string())
                            val jsonArray = jsonObject.getJSONArray("choices")
                            val result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content")

                            CoroutineScope(Dispatchers.Main).launch {
                                // chatGPT로 표현 결과 가져오기
                                loadMissionText(result)
                            }

                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }

                    } else {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(applicationContext,"api 오류가 발생하였습니다.",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(applicationContext,"api 통신이 실패하였습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            })

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        spf.edit().putBoolean("chatting", false).apply()
    }


}