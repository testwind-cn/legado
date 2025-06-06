package io.legado.app.ui.book.read.page.entities


import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.Keep
import io.legado.app.ui.book.read.page.entities.TextChapter.Companion.emptyTextChapter
import java.util.regex.Pattern

/**
 * 句子信息
 */
@Keep
@Suppress("unused")
data class TextSentence(
    var positionStart: Int = 0,  // 在整章节中的起止位置 ， textChapter
    var positionEnd: Int = 0
) {
    var chapterIndex: Int = 0 // 章节序号

    // val textLines: ArrayList<TextLine> = arrayListOf()
    // val firstLine: TextLine get() = textLines.first()
    // val lastLine: TextLine get() = textLines.last()
    var lineFirst: TextLine? = null
    var lineLast: TextLine? = null

    var testLineFirstIndex: Int = -1
    var testLineLastIndex: Int = -1
    var testPageFirstIndex: Int = -1
    var testPageLastIndex: Int = -1

    var charIndexFirstLine: Int = 0  // 第一行，从第几个字符
    var charIndexLastLine: Int = 0  // 最后行，到第几个字符

    val chapterIndices: IntRange get() = (lineFirst?.chapterPosition?:0) + charIndexFirstLine .. (lineLast?.chapterPosition?:0) + charIndexLastLine
    val chapterPosition: Int get() = (lineFirst?.chapterPosition?:0) + charIndexFirstLine

    var text: String = ""
    /*
    val text: String
        get() = textLines.mapIndexed { index, textLine ->
            if (index == textLines.size) {
                textLine.text.substring(0, LastLineCharIndex)
            } else if (index == 0) {
                textLine.text.substring(firstLineCharIndex)
            } else textLine.text
        }.joinToString("")
    */

    private fun fillTextLines(chapter: TextChapter) {
        var textLines: ArrayList<TextLine> = arrayListOf()

        textChapter = chapter

        chapterIndex = textChapter.chapter.index

        text = ""

        testLineFirstIndex = -1
        testLineLastIndex = -1
        testPageFirstIndex = -1
        testPageLastIndex = -1

        // 设置 pageFirstIndex 和 lineFirstIndex
        // 设置 pageLastIndex 和 lineLastIndex
        var curChapterPosition = 0
        for ( pgIndex in textChapter.pages.indices )  {
            val pg = textChapter.pages[pgIndex]

            if ( positionStart >= pg.lines.last().chapterIndices.last )
                continue

            for ( lnIndex in pg.lines.indices ) {
                val ln = pg.lines[lnIndex]
                curChapterPosition = ln.chapterPosition
                if (curChapterPosition <= positionEnd && positionStart < curChapterPosition+ ln.charSize) {
                    textLines.add(ln)
                    if ( testPageFirstIndex < 0 ) {
                        testPageFirstIndex = pgIndex
                        testLineFirstIndex = lnIndex
                    }
                    testPageLastIndex = pgIndex
                    testLineLastIndex = lnIndex
                }
                if ( curChapterPosition > positionEnd )
                    break;
            }

            if ( curChapterPosition > positionEnd )
                break;
        }

        if ( textLines.isEmpty() ) {
            System.out.println("Sdssd");
            return;
        }

        // 找到第一行和最后一行
        lineFirst = textLines.first()
        lineLast = textLines.last()

        // 获取全部文字内容到 text
        // 设置 charIndexFirstLine 和 charIndexLastLine
        for ( ln in textLines ) {
            var ss = ln.text
            if ( ln == lineLast) {
                charIndexLastLine = positionEnd - ln.chapterPosition
                if ( charIndexLastLine < ss.length )
                    ss = ss.substring(0,  charIndexLastLine)
            }
            if ( ln == lineFirst) {
                charIndexFirstLine = positionStart - ln.chapterPosition
                if ( charIndexFirstLine >=0 && charIndexFirstLine < ss.length )
                    ss = ss.substring(charIndexFirstLine)
            }
            text += ss
        }

        // 清除临时 textLines
        textLines.clear()
    }

    companion object {
        var textChapter = emptyTextChapter
            private set

        // 静态属性，用于追踪活动 TextSentence 的索引
        var activeSentenceIndex: Int = -1
            private set // 可选：如果只通过特定方法更改，则将 setter 设为私有

        /**
         * 为给定的 TextChapter 生成 TextSentence 对象列表。
         * 这只是一个占位符实现。您需要定义如何界定句子。
         * 例如，通过标点符号（'.'、'!'、'?'），或通过固定数量的单词/字符。
         */
        fun generateSentencesForChapter(
            chapter: TextChapter,
            pageIndex: Int = 0,
            startPos: Int = 0
        ): List<TextSentence> {

            textChapter = chapter

            //匹配格式化后的图片格式
            val senPattern: Pattern = Pattern.compile("([。，；？!,;?]|\\.(?=\\s)|!(?=\\s))" ) //""([。，；？!.,;?])") // ”：、
            // 或者后面不是数字的英文句点 |\\.(?!\\d)
            // 或者后面是空格的英文句点   |\\.(?=\\s)
            // 或者后面是空格的英文感叹号  |!(?=\\s)

            val sentences = arrayListOf<TextSentence>()
            // --- 您的句子分割逻辑放在这里 ---
            // 这是一个非常基础的示例：它尝试根据标点符号分割句子。
            // 您需要根据您的实际文本内容和期望的句子结构来替换和完善此逻辑。

            if (textChapter.pages.isNotEmpty()) {
                for (index in pageIndex..<textChapter.paragraphs.size) {
                    var content = textChapter.paragraphs[index].text

                    val matcher = senPattern.matcher(content)
                    var start = 0
                    while (matcher.find()) {
                        val text = content.substring(start, matcher.start() + 1)
                        // Log.d("TTS18",text )
                        if (text.startsWith("它的功能强大且")) {
                            Log.d("TTS8", "============")
                        }
                        if (text.isNotBlank()) {
                            // Wang Jun 添加图片
                            var a = TextSentence(
                                start + textChapter.paragraphs[index].chapterPosition,
                                matcher.start() + textChapter.paragraphs[index].chapterPosition
                            )
                            a.fillTextLines(chapter)
                            sentences.add(a)
                        }

                        start = matcher.end()
                    }
                    if (start < content.length) {
                        val text = content.substring(start, content.length)
                        // Log.d("TTS28",text )
                        if (text.startsWith("它的功能强大且")) {
                            Log.d("TTS8", "============")
                        }
                        var a = TextSentence(
                            start + textChapter.paragraphs[index].chapterPosition,
                            content.length - 1 + textChapter.paragraphs[index].chapterPosition
                        )
                        a.fillTextLines(chapter)
                        sentences.add(a)
                    }
                }
            }

            // 为新生成的句子列表重置活动索引
            if (sentences.isNotEmpty()) {
                activeSentenceIndex = 0 // 默认激活第一个句子，或者设为 -1 如果默认不激活
            } else {
                activeSentenceIndex = -1
            }

            textChapter.readAloudSentences = sentences

            return sentences
        }

        /**
         * 通过索引设置活动句子。
         * 如果索引有效并已设置，则返回 true，否则返回 false。
         */
        fun setActiveSentence(index: Int): Boolean {
            return if (index >= 0 && index < textChapter.readAloudSentences.size) {
                activeSentenceIndex = index
                true
            } else {
                // 可选：记录错误或处理无效索引
                // activeSentenceIndex = -1 // 如果索引越界，则重置
                false
            }
        }

        /**
         * 从列表中获取当前活动的 TextSentence。
         * 如果没有活动句子或索引越界，则返回 null。
         */
        fun getActiveSentence(): TextSentence? {
            return if (activeSentenceIndex >= 0 && activeSentenceIndex < textChapter.readAloudSentences.size) {
                textChapter.readAloudSentences[activeSentenceIndex]
            } else {
                null
            }
        }
    }
}