package io.legado.app.ui.book.read.page.entities


import android.annotation.SuppressLint
import androidx.annotation.Keep
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.book.BookContent
import io.legado.app.ui.book.read.page.provider.LayoutProgressListener
import io.legado.app.ui.book.read.page.provider.TextChapterLayout
import io.legado.app.utils.fastBinarySearchBy
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import kotlin.math.min

/**
 * 句子信息
 */
@Keep
@Suppress("unused")
data class TextSentence(
    var chapterIndex: Int = 0,
    var positionStart: Int = 0,
    var positionEnd: Int = 0
) {
    // val textLines: ArrayList<TextLine> = arrayListOf()
    // val firstLine: TextLine get() = textLines.first()
    // val lastLine: TextLine get() = textLines.last()
    var lineFirst: TextLine? = null
    var lineLast: TextLine? = null
    var lineFirstIndex: Int = -1
    var lineLastIndex: Int = -1
    var pageFirstIndex: Int = -1
    var pageLastIndex: Int = -1

    var charIndexFirstLine: Int = 0
    var charIndexLastLine: Int = 0

    val chapterIndices: IntRange get() = (lineFirst?.chapterPosition?:0) + charIndexFirstLine..(lineLast?.chapterPosition?:0) + charIndexLastLine
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
    fun fillTextLines(chapter: TextChapter) {
        var textLines: ArrayList<TextLine> = arrayListOf()
        text = ""

        lineFirstIndex = -1
        lineLastIndex = -1
        pageFirstIndex = -1
        pageLastIndex = -1

        for ( pgIndex in chapter.pages.indices )  {
            val pg = chapter.pages[pgIndex]
            for ( lnIndex in pg.lines.indices ) {
                val ln = pg.lines[lnIndex]
                if (!ln.isImage && ln.chapterPosition <= positionEnd && positionStart < ln.chapterPosition + ln.charSize) {
                    textLines.add(ln)
                    if ( pageFirstIndex < 0 ) {
                        pageFirstIndex = pgIndex
                        lineFirstIndex = lnIndex
                    }
                    pageLastIndex = pgIndex
                    lineLastIndex = lnIndex
                }
            }
        }

        if ( textLines.isEmpty() ) {
            System.out.println("Sdssd");
            return;
        }

        lineFirst = textLines.first()
        lineLast = textLines.last()

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

        textLines.clear()
    }
}