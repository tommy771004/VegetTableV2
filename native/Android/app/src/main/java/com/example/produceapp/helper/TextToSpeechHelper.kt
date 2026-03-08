package com.example.produceapp.helper

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class TextToSpeechHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.TRADITIONAL_CHINESE
            tts.setSpeechRate(0.85f) // 稍慢語速，方便長輩聆聽
            isReady = true
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "produce_tts")
        }
    }

    fun speakPrice(cropName: String, avgPrice: Double, unit: String) {
        speak("${cropName}，目前平均價格為每${unit}${String.format("%.1f", avgPrice)}元")
    }

    fun setLanguage(locale: Locale) {
        tts.language = locale
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
