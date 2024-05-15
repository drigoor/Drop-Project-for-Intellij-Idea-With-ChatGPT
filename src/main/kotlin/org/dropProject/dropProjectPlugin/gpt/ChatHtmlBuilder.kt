package org.dropProject.dropProjectPlugin.gpt

import com.intellij.ui.JBColor
import com.intellij.util.ui.StyleSheetUtil
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import javax.swing.text.html.StyleSheet


class ChatHtmlBuilder {
    private var cssStyle = "<style>\n" +
            "    * {\n" +
            "        margin: 0;\n" +
            "        padding: 0;\n" +
            "        background-color: #787878;\n" +
            "        font-family: Consolas, 'Courier New', monospace;\n" +
            "        font-size: 10px;\n" +
            "    }\n" +
            "    table {\n" +
            "        width: 80%; /* Adjust table width as needed */\n" +
            "        border-collapse: collapse;\n" +
            "    }\n" +
            "p {\n" +
            "       word-wrap: break-word;\n" +
            "   }\n" +
            "    td, th {\n" +
            "        padding: 8px; /* Add padding for better readability */\n" +
            "        word-wrap: break-word; /* Wrap long words */\n" +
            "        max-width: 400px; /* Limit the maximum width */\n" +
            "        text-align: left;\n" +
            "        vertical-align: top;\n" +
            "        border-bottom: 1px solid #ccc;\n" +
            "    }\n" +
            "    .user-column {\n" +
            "        width: 4%;\n" +
            "        font-weight: bold;\n" +
            "        color: rgb(150, 255, 150);\n" +
            "    }\n" +
            "    .message-column {\n" +
            "        width: 96%;\n" +
            "        color: #ffffff; /* Set text color for contrast */\n" +
            "        /*background-color: #000000; Set background color */\n" +
            "    }\n" +
            "    .chatgpt-column {\n" +
            "        font-weight: bold;\n" +
            "        color: rgb(100, 255, 255);\n" +
            "    }\n" +
            "</style>"

    private var content = StringBuilder("<table>")
    fun getHtmlChat(): String {
        println("$content</table>\n")
        return "$content</table>\n"
    }
    fun append(user: String, message: String, isUser: Boolean) {
        content.append("<tr>")

        if (isUser) {
            content.append("<td class=\"user-column\">$user: </td>\n")
            content.append("<td class=\"message-column\">${converMkdownToHtml(message)}</td>\n")
        } else {
            content.append("<td class=\"chatgpt-column\">$user: </td>\n")
            content.append("<td class=\"message-column\">${converMkdownToHtml(message)}</td>\n")
        }

        content.append("</tr>")
    }
    private fun converMkdownToHtml(mkdownText: String): String {
        val parser = Parser.builder().build()
        val document = parser.parse(mkdownText)
        val htmlRenderer = HtmlRenderer.builder().build()
        val htmlResponse = htmlRenderer.render(document)

        return htmlResponse
    }

    fun getStyle(): StyleSheet = StyleSheetUtil.loadStyleSheet("""
    * {
        margin: 0;
        padding: 0;
        font-family: Consolas, 'Courier New', monospace;
        font-size: 10px;
        word-wrap: break-word;
    }
    table {
        width: 80%; /* Adjust table width as needed */
        border-collapse: collapse;
    }
    p {
        word-wrap: break-word;
    }
    
    code {
        color: ${getCodeColor()};
    }
    
    td, th {
        padding: 8px; /* Add padding for better readability */
        word-wrap: break-word; /* Wrap long words */
        max-width: 400px; /* Limit the maximum width */
        text-align: left;
        vertical-align: top;
        border-bottom: 1px solid #ccc;
    }
    .user-column {
        width: 4%;
        font-weight: bold;
        color: ${getUserColor()};
    }
    .message-column {
        width: 96%;
    }
    .chatgpt-column {
        font-weight: bold;
        color: ${getGPTColor()};
    }
    """.trimIndent())

    private fun getCodeColor(): String {
        if (isCurrentThemeDark()) {
            return "rgb(200, 200, 255)"
        }
        return "rgb(138, 43, 226)"
    }

    private fun getUserColor(): String {
        if (isCurrentThemeDark()) {
            return "rgb(50, 255, 50)"
        }
        return "rgb(0, 230, 0)"
    }

    private fun getGPTColor(): String {
        if (isCurrentThemeDark()) {
            return "rgb(0, 100, 255)"
        }
        return "rgb(0, 0, 240)"
    }

    private fun isCurrentThemeDark(): Boolean {
        val backgroundColor = JBColor.background()
        val red = backgroundColor.red
        val green = backgroundColor.green
        val blue = backgroundColor.blue


        val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
        return luminance < 128
    }
}