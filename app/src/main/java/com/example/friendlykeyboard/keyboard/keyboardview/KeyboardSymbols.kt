package com.example.friendlykeyboard.keyboard.keyboardview

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Typeface
import android.inputmethodservice.Keyboard
import android.media.AudioManager
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.children
import com.example.friendlykeyboard.R
import com.example.friendlykeyboard.keyboard.KeyboardInteractionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KeyboardSymbols constructor(var context:Context, var layoutInflater: LayoutInflater, var keyboardInterationListener: KeyboardInteractionListener) {
    lateinit var symbolsLayout: LinearLayout
    var inputConnection:InputConnection? = null
        set(inputConnection){
            field = inputConnection
        }
    var isCaps:Boolean = false
    var buttons:MutableList<Button> = mutableListOf<Button>()
    lateinit var vibrator: Vibrator
    lateinit var sharedPreferences: SharedPreferences

    lateinit var numpadLine : LinearLayout
    lateinit var firstLine: LinearLayout
    lateinit var secondLine: LinearLayout
    lateinit var thirdLine: LinearLayout
    lateinit var fourthLine: LinearLayout

    val numpadText = listOf<String>("1","2","3","4","5","6","7","8","9","0")
    val firstLineText = listOf<String>("+","×","÷","=","/","￦","<",">","♡","☆")
    val secondLineText = listOf<String>("!","@","#","~","%","^","&","*","(",")")
    val thirdLineText = listOf<String>("\uD83D\uDE00","-","'","\"",":",";",",","?","DEL")
    val fourthLineText = listOf<String>("가","한/영",",","space",".","Enter")
    val myKeysText = ArrayList<List<String>>()
    val layoutLines = ArrayList<LinearLayout>()
    var downView:View? = null
    var animationMode:Int = 0
    var vibrate = 0
    var sound = 0
    var capsView:ImageView? = null

    // 키보드 속성 업데이트
    fun updateKeyboard(){
        val height = sharedPreferences.getInt("keyboardHeight", 150)
        val paddingLeft = sharedPreferences.getInt("keyboardPaddingLeft", 0)
        val paddingRight = sharedPreferences.getInt("keyboardPaddingRight", 0)
        val paddingBottom = sharedPreferences.getInt("keyboardPaddingBottom", 0)
        val fontColor = sharedPreferences.getInt("keyboardFontColor", -16777216)
        val fontStyle = sharedPreferences.getBoolean("keyboardFontStyle", false)
        val keyboardColor = sharedPreferences.getInt("keyboardColor", -1)
        val keyboardBackgroundColor = sharedPreferences.getInt("keyboardBackground", 0)

        // 키보드 padding 업데이트
        symbolsLayout.setPadding(paddingLeft, 0, paddingRight, paddingBottom)

        // 키보드 높이 업데이트
        if(context.getResources().configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            numpadLine.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (height * 0.7).toInt())
            firstLine.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (height*0.7).toInt())
            secondLine.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (height*0.7).toInt())
            thirdLine.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (height*0.7).toInt())
            fourthLine.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (height * 0.7).toInt())
        }else{
            numpadLine.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
            firstLine.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
            secondLine.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
            thirdLine.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
            fourthLine.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)
        }

        // 키보드 폰트, 자판 색, 폰트 색 업데이트
        for (button in buttons) {
            if (fontStyle) {
                button.setTypeface(null, Typeface.BOLD)
            } else {
                button.setTypeface(null, Typeface.NORMAL)
            }

            button.setTextColor(fontColor)
            button.background.setTint(keyboardColor)
        }

        // 키보드 배경색 업데이트
        symbolsLayout.setBackgroundColor(keyboardBackgroundColor)
    }

    fun init(){
        symbolsLayout = layoutInflater.inflate(R.layout.keyboard_symbols, null) as LinearLayout
        inputConnection = inputConnection
        keyboardInterationListener = keyboardInterationListener
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        context = context
        sharedPreferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE)

        val config = context.getResources().configuration
        val sharedPreferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE)
        val height = sharedPreferences.getInt("keyboardHeight", 150)
        animationMode = sharedPreferences.getInt("theme", 0)
        sound = sharedPreferences.getInt("keyboardSound", -1)
        vibrate = sharedPreferences.getInt("keyboardVibrate", -1)

        numpadLine = symbolsLayout.findViewById<LinearLayout>(
            R.id.numpad_line
        )
        firstLine = symbolsLayout.findViewById<LinearLayout>(
            R.id.first_line
        )
        secondLine = symbolsLayout.findViewById<LinearLayout>(
            R.id.second_line
        )
        thirdLine = symbolsLayout.findViewById<LinearLayout>(
            R.id.third_line
        )
        fourthLine = symbolsLayout.findViewById<LinearLayout>(
            R.id.fourth_line
        )

        updateKeyboard()

        myKeysText.clear()
        myKeysText.add(numpadText)
        myKeysText.add(firstLineText)
        myKeysText.add(secondLineText)
        myKeysText.add(thirdLineText)
        myKeysText.add(fourthLineText)

        layoutLines.clear()
        layoutLines.add(numpadLine)
        layoutLines.add(firstLine)
        layoutLines.add(secondLine)
        layoutLines.add(thirdLine)
        layoutLines.add(fourthLine)

        setLayoutComponents()
    }

    fun getLayout():LinearLayout{
        return symbolsLayout
    }

    fun modeChange(){
        if(isCaps){
            isCaps = false
            for(button in buttons){
                button.setText(button.text.toString().toLowerCase())
            }
        }
        else{
            isCaps = true
            for(button in buttons){
                button.setText(button.text.toString().toUpperCase())
            }
        }
    }

    private fun playClick(i: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        when (i) {
            32 -> am!!.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR)
            Keyboard.KEYCODE_DONE, 10 -> am!!.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN)
            Keyboard.KEYCODE_DELETE -> am!!.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE)
            else -> am!!.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, -1.toFloat())
        }
    }


    private fun playVibrate(){
        if(sharedPreferences.getBoolean("vibrate", false)){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(70, vibrate))
            }
            else{
                vibrator.vibrate(70)
            }
        }
    }

    private fun getMyClickListener(actionButton:Button):View.OnClickListener{

        val clickListener = (View.OnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                inputConnection?.requestCursorUpdates(InputConnection.CURSOR_UPDATE_IMMEDIATE)
            }
            playVibrate()
            val cursorcs:CharSequence? =  inputConnection?.getSelectedText(InputConnection.GET_TEXT_WITH_STYLES)
            if(cursorcs != null && cursorcs.length >= 2){

                val eventTime = SystemClock.uptimeMillis()
                inputConnection?.finishComposingText()
                inputConnection?.sendKeyEvent(KeyEvent(eventTime, eventTime,
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD))
                inputConnection?.sendKeyEvent(KeyEvent(SystemClock.uptimeMillis(), eventTime,
                    KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD))
            }

            when (actionButton.text.toString()) {
                "\uD83D\uDE00" -> {
                    keyboardInterationListener.modechange(3)
                }
                "한/영" -> {
                    keyboardInterationListener.modechange(1)
                }
                "가" -> {
                    keyboardInterationListener.modechange(1)
                }
                else -> {
                    playClick(
                        actionButton.text.toString().toCharArray().get(
                            0
                        ).toInt()
                    )
                    inputConnection?.commitText(actionButton.text.toString(), 1)
                    sendText()
                }

            }

        })
        actionButton.setOnClickListener(clickListener)
        return clickListener
    }

    fun getOnTouchListener(clickListener: View.OnClickListener):View.OnTouchListener{
        val handler = Handler()
        val initailInterval = 500
        val normalInterval = 100
        val handlerRunnable = object: Runnable{
            override fun run() {
                handler.postDelayed(this, normalInterval.toLong())
                clickListener.onClick(downView)
            }
        }
        val onTouchListener = object:View.OnTouchListener {
            override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
                when (motionEvent?.getAction()) {
                    MotionEvent.ACTION_DOWN -> {
                        handler.removeCallbacks(handlerRunnable)
                        handler.postDelayed(handlerRunnable, initailInterval.toLong())
                        downView = view!!
                        clickListener.onClick(view)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        handler.removeCallbacks(handlerRunnable)
                        downView = null
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(handlerRunnable)
                        downView = null
                        return true
                    }
                }
                return false
            }
        }

        return onTouchListener
    }

    private fun setLayoutComponents(){
//            inputConnection.beginBatchEdit()
        for(line in layoutLines.indices){
            val children = layoutLines[line].children.toList()
            val myText = myKeysText[line]
            for(item in children.indices){
                val actionButton = children[item].findViewById<Button>(R.id.key_button)
                val specialKey = children[item].findViewById<ImageView>(R.id.special_key)
                var myOnClickListener:View.OnClickListener? = null
                when(myText[item]){
                    "space" -> {
                        specialKey.setImageResource(R.drawable.ic_space_bar)
                        specialKey.visibility = View.VISIBLE
                        actionButton.visibility = View.GONE
                        myOnClickListener = getSpaceAction()
                        specialKey.setOnClickListener(myOnClickListener)
                        specialKey.setOnTouchListener(getOnTouchListener(myOnClickListener))
                        specialKey.setBackgroundResource(R.drawable.key_background)
                    }
                    "DEL" -> {
                        specialKey.setImageResource(R.drawable.del)
                        specialKey.visibility = View.VISIBLE
                        actionButton.visibility = View.GONE
                        myOnClickListener = getDeleteAction()
                        specialKey.setOnClickListener(myOnClickListener)
                        specialKey.setOnTouchListener(getOnTouchListener(myOnClickListener))
                    }
                    "CAPS" -> {
                        specialKey.setImageResource(R.drawable.ic_caps_unlock)
                        specialKey.visibility = View.VISIBLE
                        actionButton.visibility = View.GONE
                        capsView = specialKey
                        myOnClickListener = getCapsAction()
                        specialKey.setOnClickListener(myOnClickListener)
                        specialKey.setOnTouchListener(getOnTouchListener(myOnClickListener))
                        specialKey.setBackgroundResource(R.drawable.key_background)
                    }
                    "Enter" -> {
                        specialKey.setImageResource(R.drawable.ic_enter)
                        specialKey.visibility = View.VISIBLE
                        actionButton.visibility = View.GONE
                        myOnClickListener = getEnterAction()
                        specialKey.setOnClickListener(myOnClickListener)
                        specialKey.setOnTouchListener(getOnTouchListener(myOnClickListener))
                        specialKey.setBackgroundResource(R.drawable.key_background)
                    }
                    else -> {
                        actionButton.text = myText[item]
                        buttons.add(actionButton)
                        myOnClickListener = getMyClickListener(actionButton)
                        actionButton.setOnTouchListener(getOnTouchListener(myOnClickListener))
                    }
                }
                children[item].setOnClickListener(myOnClickListener)
            }
        }
    }
    fun getSpaceAction():View.OnClickListener{
        return View.OnClickListener{
            playClick('ㅂ'.toInt())
            playVibrate()
            inputConnection?.commitText(" ",1)
            sendText()
        }
    }

    fun getDeleteAction():View.OnClickListener{
        return View.OnClickListener{
            playVibrate()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                inputConnection?.deleteSurroundingTextInCodePoints(1, 0)
            }else{
                inputConnection?.deleteSurroundingText(1,0)
            }
            sendText()
        }
    }

    fun getCapsAction():View.OnClickListener{
        return View.OnClickListener{
            playVibrate()
            modeChange()
        }
    }

    fun getEnterAction():View.OnClickListener{
        return View.OnClickListener{
            if (inputConnection?.getExtractedText(ExtractedTextRequest(), InputConnection.GET_TEXT_WITH_STYLES)?.text.toString().length >= 1){
                playVibrate()
                val eventTime = SystemClock.uptimeMillis()

                enterText()

                //key ActionDown --> 키 눌렸을 때
                inputConnection?.sendKeyEvent(
                    KeyEvent(eventTime, eventTime,
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0, 0, 0, 0,
                        KeyEvent.FLAG_SOFT_KEYBOARD)
                )

                //key ActionUp --> 눌린 키 떼지도록
                inputConnection?.sendKeyEvent(
                    KeyEvent(
                        SystemClock.uptimeMillis(), eventTime,
                        KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0, 0, 0, 0,
                        KeyEvent.FLAG_SOFT_KEYBOARD)
                )
            }
        }
    }

    fun sendText(){
        val text = inputConnection?.getExtractedText(ExtractedTextRequest(), InputConnection.GET_TEXT_WITH_STYLES)
        keyboardInterationListener.sendText(text?.text.toString())
    }

    fun enterText(){
        val text = inputConnection?.getExtractedText(ExtractedTextRequest(), InputConnection.GET_TEXT_WITH_STYLES)
        keyboardInterationListener.checkText(text?.text.toString())
    }

}