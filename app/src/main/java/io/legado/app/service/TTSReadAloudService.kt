package io.legado.app.service

import android.app.PendingIntent
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID
import android.speech.tts.UtteranceProgressListener
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.constant.EventBus
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.MediaHelp
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.model.ReadAloud
import io.legado.app.model.ReadBook
import io.legado.app.utils.GSON
import io.legado.app.utils.LogUtils
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.postEvent
import io.legado.app.utils.servicePendingIntent
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

/**
 * 本地朗读
 */
class TTSReadAloudService : BaseReadAloudService(), TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var ttsInitFinish = false
    private val ttsUtteranceListener = TTSUtteranceListener()
    private var speakJob: Coroutine<*>? = null
    private val TAG = "TTSReadAloudService"

    override fun onCreate() {
        super.onCreate()
        val msg = "===== 02. 启动朗读服务 TTSReadAloudService.onCreate "
        AppLog.put(msg)
        initTts()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearTTS()
    }

    @Synchronized
    private fun initTts() {
        val msg = "===== 03. 启动朗读服务 TTSReadAloudService.initTts "
        AppLog.put(msg)
        ttsInitFinish = false
        val engine = GSON.fromJsonObject<SelectItem<String>>(ReadAloud.ttsEngine).getOrNull()?.value
        LogUtils.d(TAG, "initTts engine:$engine")
        textToSpeech = if (engine.isNullOrBlank()) {
            TextToSpeech(this, this)
        } else {
            TextToSpeech(this, this, engine)
        }
        upSpeechRate()
    }

    @Synchronized
    fun clearTTS() {
        textToSpeech?.runCatching {
            stop()
            shutdown()
        }
        textToSpeech = null
        ttsInitFinish = false
    }

    override fun onInit(status: Int) {
        val msg = "===== 07. 启动朗读服务 TTSReadAloudService.onInit "
        AppLog.put(msg)
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let {
                it.setOnUtteranceProgressListener(ttsUtteranceListener)
                ttsInitFinish = true

                val msg = "===== 08. 启动朗读服务 TTSReadAloudService.play "
                AppLog.put(msg)
                play()
            }
        } else {
            toastOnUi(R.string.tts_init_failed)
        }
    }

    @Synchronized
    override fun play() {
        var msg = "===== 09. 启动朗读服务 TTSReadAloudService.play ${ttsInitFinish} ${contentList.size}"
        AppLog.put(msg)

        if (!ttsInitFinish) return
        if (!requestFocus()) return
        if (contentList.isEmpty() && ! readAloudBySentence ||
            sentenceList.isEmpty() && readAloudBySentence) {
            AppLog.putDebug("朗读列表为空")
            ReadBook.readAloud()
            return
        }
        msg = "===== 10. 启动朗读服务 TTSReadAloudService.play "
        AppLog.put(msg)

        super.play()
        MediaHelp.playSilentSound(this@TTSReadAloudService)
        speakJob?.cancel()
        speakJob = execute {
            LogUtils.d(TAG, "朗读列表大小 ${contentList.size}")
            LogUtils.d(TAG, "朗读句子列表大小 ${sentenceList.size}")
            LogUtils.d(TAG, "朗读页数 ${textChapter?.pageSize}")
            val tts = textToSpeech ?: throw NoStackTraceException("tts is null")
            val contentListSize =  if ( readAloudBySentence ) sentenceList.size else contentList.size
            var isAddedText = false
            for (i in nowSpeak until contentListSize) {
                ensureActive()
                var text = if ( readAloudBySentence ) sentenceList[i].text else contentList[i]
                if (paragraphStartPos > 0 && i == nowSpeak) {
                    text = text.substring(paragraphStartPos)
                }
                if (text.matches(AppPattern.notReadAloudRegex)) {
                    continue
                }
                if (!isAddedText) {
                    val result = tts.runCatching {
                        AppLog.put("tts QUEUE_FLUSH " + i)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            speak(text, TextToSpeech.QUEUE_FLUSH, null, AppConst.APP_TAG + i)
                        } else {
                            var params: HashMap<String, String> = HashMap()
                            params[KEY_PARAM_UTTERANCE_ID] = AppConst.APP_TAG + "," + i
                            speak(text, TextToSpeech.QUEUE_FLUSH, params)
                        }
                    }.getOrElse {
                        AppLog.put("tts出错\n${it.localizedMessage}", it, true)
                        TextToSpeech.ERROR
                    }
                    if (result == TextToSpeech.ERROR) {
                        AppLog.put("tts出错 尝试重新初始化")
                        clearTTS()
                        initTts()
                        return@execute
                    }
                } else {
                    val result = tts.runCatching {
                        // speak(text, TextToSpeech.QUEUE_ADD, null, AppConst.APP_TAG + i)
                        var tag = if ( readAloudBySentence ) {
                            AppConst.APP_TAG +
                                    "," + i +
                                    "," + sentenceList[i].chapterIndex +
                                    "," + sentenceList[i].pageIndexFirst +
                                    "," + sentenceList[i].pageIndexLast +
                                    "," + sentenceList[i].lineIndexFirst +
                                    "," + sentenceList[i].lineIndexLast +
                                    ", top1 " + ((sentenceList[i].lineFirst?.lineTop) ?: 0f) +
                                    " top2 " + ((sentenceList[i].lineLast?.lineTop) ?: 0f) +
                                    " First " + sentenceList[i].charIndexLineFirst +
                                    " Last " + sentenceList[i].charIndexLineLast + " text " + text
                        } else {
                            AppConst.APP_TAG + i
                        }

                        AppLog.put("TTS6 : " + "QUEUE_ADD utteranceId " + tag)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            speak(text, TextToSpeech.QUEUE_ADD, null, tag)
                        } else {
                            var params: HashMap<String, String> = HashMap()
                            params[KEY_PARAM_UTTERANCE_ID] = tag
                            speak(text, TextToSpeech.QUEUE_ADD, params)
                        }
                    }.getOrElse {
                        AppLog.put("tts出错\n${it.localizedMessage}", it, true)
                        TextToSpeech.ERROR
                    }
                    if (result == TextToSpeech.ERROR) {
                        AppLog.put("tts朗读出错:$text")
                    }
                }
                isAddedText = true
            }
            LogUtils.d(TAG, "朗读内容添加完成")
            if (!isAddedText) {
                playStop()
                delay(1000)
                nextChapter()
            }
        }.onError {
            AppLog.put("tts朗读出错\n${it.localizedMessage}", it, true)
        }
    }

    override fun playStop() {
        textToSpeech?.runCatching {
            stop()
        }
    }

    /**
     * 更新朗读速度
     */
    override fun upSpeechRate(reset: Boolean) {
        if (AppConfig.ttsFlowSys) {
            if (reset) {
                clearTTS()
                initTts()
            }
        } else {
            val speechRate = (AppConfig.ttsSpeechRate + 5) / 10f
            textToSpeech?.setSpeechRate(speechRate)
        }
    }

    /**
     * 暂停朗读
     */
    override fun pauseReadAloud(abandonFocus: Boolean) {
        super.pauseReadAloud(abandonFocus)
        speakJob?.cancel()
        textToSpeech?.runCatching {
            stop()
        }
    }

    /**
     * 恢复朗读
     */
    override fun resumeReadAloud() {
        super.resumeReadAloud()
        play()
    }

    /**
     * 朗读监听
     */
    private inner class TTSUtteranceListener : UtteranceProgressListener() {

        private val TAG = "TTSUtteranceListener"

        override fun onStart(s: String) {
            LogUtils.d(TAG, "onStart nowSpeak:$nowSpeak pageIndex:$pageIndex utteranceId:$s")
            textChapter?.let {
                if ( readAloudBySentence ) {
                    val readAloudNumberOld = readAloudNumber
                    var nowSpeak2 = s.split(",").getOrNull(1)?.toInt()?:nowSpeak
                    nowSpeak = nowSpeak2
                    readAloudNumber = sentenceList[nowSpeak].chapterPosition

                    // 只有原来朗读位置，在当前下。新一句位置大于等于下页，才翻页
                    // 如果下页是图片，下句是在下下页，这时只翻了一页，图片也能被显示一句
                    if (readAloudNumberOld >= it.getReadLength(pageIndex)
                        && readAloudNumberOld < it.getReadLength(pageIndex + 1)
                        && readAloudNumber >= it.getReadLength(pageIndex + 1)
                        ) {
                        pageIndex++
                        ReadBook.moveToNextPage()
                    }

                    // TTS_PROGRESS_PAGE 向上翻页
                    val bundle = Bundle().apply {
                        putString("state", "onDone")
                        putInt("chapterIndex0", sentenceList[nowSpeak-1].chapterIndex,)
                        putInt("pageIndexFirst0", sentenceList[nowSpeak-1].pageIndexFirst)
                        putInt("lineIndexFirst0",sentenceList[nowSpeak-1].lineIndexFirst)
                        putInt("pageIndexLast0",sentenceList[nowSpeak-1].pageIndexLast)
                        putInt("lineIndexLast0",sentenceList[nowSpeak-1].lineIndexLast)
                        putInt("chapterIndex1",sentenceList[nowSpeak].chapterIndex)
                        putInt("pageIndexFirst1",sentenceList[nowSpeak].pageIndexFirst)
                        putInt("lineIndexFirst1",sentenceList[nowSpeak].lineIndexFirst)
                        putInt("pageIndexLast1",sentenceList[nowSpeak].pageIndexLast)
                        putInt("lineIndexLast1",sentenceList[nowSpeak].lineIndexLast)
                    }
                    postEvent(EventBus.TTS_PROGRESS_PAGE, bundle)   // 滚动翻页


                    LogUtils.d(TAG, "TTS6 : " +  "onStart utteranceId " + s + " nowSpeak " + nowSpeak + " nowSpeak2 " + nowSpeak2)
                    upTtsProgressSentence(
                        textChapter?.chapter?.index?:0,
                        (sentenceList[nowSpeak2].lineFirst?.chapterPosition?:0) + sentenceList[nowSpeak2].charIndexLineFirst ,
                        (sentenceList[nowSpeak2].lineLast?.chapterPosition?:0) + sentenceList[nowSpeak2].charIndexLineLast )
                } else {
                    if (contentList[nowSpeak].matches(AppPattern.notReadAloudRegex)) {
                        nextParagraph()
                    }
                    if (readAloudNumber >= it.getReadLength(pageIndex + 1)) {
                        pageIndex++
                        ReadBook.moveToNextPage()
                    }
                    upTtsProgress(readAloudNumber + 1)
                }
            }
        }

        override fun onDone(s: String) {
            LogUtils.d(TAG, "onDone utteranceId:$s")
            if ( readAloudBySentence ) {
                nextSentence(s)
                return
            }
            nextParagraph()
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            super.onRangeStart(utteranceId, start, end, frame)
            val msg =
                "onRangeStart nowSpeak:$nowSpeak pageIndex:$pageIndex utteranceId:$utteranceId start:$start end:$end frame:$frame"
            LogUtils.d(TAG, msg)
            textChapter?.let {
                if (readAloudNumber + start > it.getReadLength(pageIndex + 1)) {
                    pageIndex++
                    ReadBook.moveToNextPage()
                    upTtsProgress(readAloudNumber + start)
                }
            }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            LogUtils.d(
                TAG,
                "onError nowSpeak:$nowSpeak pageIndex:$pageIndex utteranceId:$utteranceId errorCode:$errorCode"
            )
            if ( readAloudBySentence ) {
                nextSentence(utteranceId ?: "")
                return
            }
            nextParagraph()
        }

        private fun nextParagraph() {
            //跳过全标点段落
            do {
                readAloudNumber += contentList[nowSpeak].length + 1 - paragraphStartPos
                paragraphStartPos = 0
                nowSpeak++
                if (nowSpeak >= contentList.size) {
                    nextChapter()
                    return
                }
            } while (contentList[nowSpeak].matches(AppPattern.notReadAloudRegex))
        }

        private fun nextSentence(s: String) {
            // 以前执行读序号加1，大于列表总数后，会翻章节
            var nowSpeak2 = s.split(",").getOrNull(1)?.toInt()?:nowSpeak
            // nowSpeak = nowSpeak2 + 1
            paragraphStartPos = 0
            LogUtils.d(
                TAG, "TTS6 : + onDone utteranceId " + s + " nowSpeak " + nowSpeak+ " nowSpeak2 " + nowSpeak2
            )
            if ( nowSpeak2 + 1 >= sentenceList.size ) {
                nextChapter()
            } else {
                // readAloudNumber = sentenceList[nowSpeak].chapterPosition
                /*
                nextSentence(
                    contentList2[nowSpeak2].chapterIndex,
                    contentList2[nowSpeak2].pageFirstIndex,
                    contentList2[nowSpeak2].lineFirstIndex,
                    contentList2[nowSpeak2].pageLastIndex,
                    contentList2[nowSpeak2].lineLastIndex,
                    contentList2[nowSpeak2+1].chapterIndex,
                    contentList2[nowSpeak2+1].pageFirstIndex,
                    contentList2[nowSpeak2+1].lineFirstIndex
                )
                 */
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(s: String) {
            LogUtils.d(TAG, "onError nowSpeak:$nowSpeak pageIndex:$pageIndex s:$s")
            if ( readAloudBySentence ) {
                nextSentence(s)
                return
            }
            nextParagraph()
        }

    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return servicePendingIntent<TTSReadAloudService>(actionStr)
    }

}