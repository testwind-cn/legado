package io.legado.app.ui.book.read.page.entities


import android.annotation.SuppressLint
import androidx.annotation.Keep

/**
 * 句子信息
 */
@Keep
@Suppress("unused")
data class TextSentence(
    var charIndexFirstLine: Int = 0,
    var charIndexLastLine: Int = 0
) {
    // val textLines: ArrayList<TextLine> = arrayListOf()
    // val firstLine: TextLine get() = textLines.first()
    // val lastLine: TextLine get() = textLines.last()
    var lineFirst: TextLine? = null
    var lineLast: TextLine? = null

    var testLineFirstIndex: Int = -1
    var testLineLastIndex: Int = -1
    var testPageFirstIndex: Int = -1
    var testPageLastIndex: Int = -1

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

    @SuppressLint("SuspiciousIndentation")
    fun fillTextLines(chapter: TextChapter, positionStart: Int = 0,
                      positionEnd: Int = 0) {
        var textLines: ArrayList<TextLine> = arrayListOf()
        text = ""

        testLineFirstIndex = -1
        testLineLastIndex = -1
        testPageFirstIndex = -1
        testPageLastIndex = -1

        // 设置 pageFirstIndex 和 lineFirstIndex
        // 设置 pageLastIndex 和 lineLastIndex
        for ( pgIndex in chapter.pages.indices )  {
            val pg = chapter.pages[pgIndex]
            for ( lnIndex in pg.lines.indices ) {
                val ln = pg.lines[lnIndex]
                if (!ln.isImage && ln.chapterPosition <= positionEnd && positionStart < ln.chapterPosition + ln.charSize) {
                    textLines.add(ln)
                    if ( testPageFirstIndex < 0 ) {
                        testPageFirstIndex = pgIndex
                        testLineFirstIndex = lnIndex
                    }
                    testPageLastIndex = pgIndex
                    testLineLastIndex = lnIndex
                }
            }
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
                    ss = ss.substring(0,  charIndexLastLine +1)
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
}