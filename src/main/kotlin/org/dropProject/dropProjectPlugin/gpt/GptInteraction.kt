package org.dropProject.dropProjectPlugin.gpt

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.dropProject.dropProjectPlugin.DefaultNotification
import org.dropProject.dropProjectPlugin.plafond.Plafond
import org.dropProject.dropProjectPlugin.settings.SettingsState
import java.io.File
import java.nio.file.FileSystems
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class GptInteraction(var project: Project) {
    var model = ""

    private val separator = FileSystems.getDefault().separator
    //private val logFileDirectory = "${System.getProperty("user.home")}${separator}Documents${separator}Drop Project Plugin${separator}"
    private val logFileDirectory = project.let { FileEditorManager.getInstance(it).project.basePath.toString() }
    private val dateTime = Date()
    private val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
    private val logFile = File("${logFileDirectory}${separator}chat_logs${separator}chat_log_${formatter.format(dateTime)}.txt")
    private var responseLog = ArrayList<GPTResponse>()
    private var chatLog = ArrayList<Message>()
    private var chatToSave = ArrayList<LogMessage>()
    private var messages = mutableListOf(
        Message("system", "You are a helpful assistant"),
    )

    init {
        val logFileParent = logFile.parentFile
        if (!logFileParent.exists()) {
            logFileParent.mkdirs() // Creating the parent directories if they don't exist
        }
        if (!logFile.exists()) {
            logFile.createNewFile() // Creating the target file if it doesn't exist
        }
    }

    fun executePrompt(prompt: String): String {

        val chatGptResponse = processPrompt()

        //add prompt and response to chatLog
        chatLog.add(Message("system", chatGptResponse))

        if (chatGptResponse.contains("Error")) {
            return chatGptResponse
        }

        return responseLog.last().choices.first().message.content
    }

    private fun processPrompt(): String {
        if (model != "gpt-4" && Plafond.isBellow70("https://raw.githubusercontent.com/drigoor/Drop-Project-for-Intellij-Idea-With-ChatGPT/master/plafond.json")) {
                return "Error: Not enough plafond"
        }

        val settingsState = SettingsState.getInstance()
        val apiKey = settingsState.openAiToken

        if (apiKey == "") {
            DefaultNotification.notify(project, "No API key set")
            return "Error: No API key set"
        }


        val apiUrl = "https://api.openai.com/v1/chat/completions"

        val messagesJson = messages.joinToString(",") {
            """
            {
                "role": "${it.role}",
                "content": "${it.content}"
            }
            """
        }

        val requestBody =
            """
            {
                "model": "$model",
                "messages": [$messagesJson]
            }
            """.trimIndent()

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        try {
            val response = client.newCall(request).execute()

            //println("res0: $response")

            if (!response.isSuccessful) {
                val json = response.body?.string()
                val moshi = Moshi.Builder().build()
                val adapter = moshi.adapter(ErrorResponse::class.java)
                //println(json)
                val myResponse = adapter.fromJson(json!!) ?: return "didnt work"

                DefaultNotification.notify(project, "Response unsuccseessful, no tokens")

                logMessageGpt(myResponse.error.message)

                return "Error code: {${myResponse.error.code}}"
            }

            //println("res1: $response")

            val json = response.body?.string()
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(GPTResponse::class.java)
            //println(json)
            val myResponse = adapter.fromJson(json!!) ?: return ""

            client.connectionPool.evictAll()

            responseLog.add(myResponse)

            logMessageGpt(myResponse.choices.first().message.content)

            return myResponse.choices.first().message.content

        } catch (exception : Exception) {
            //mostrar uma notificação a dizer que o chatgpt não respondeu
            return "Erro desconhecido"
        }
    }

    private fun logMessageGpt(message: String) {
        //println(logFile.absolutePath)
        /*
        try{
            logFile.appendText(
                "Author: ChatGPT" + "\n" +
                        "Model: $model\n" +
                        "DateTime: ${java.time.LocalDateTime.now()}\n" +
                        "Message: $message\n\n"
            )
        } catch (exception : Exception){
            println("Couldn't write file")
        }
        */
        val logMessage = LogMessage("ChatGPT", message.trim(), java.time.LocalDateTime.now(), model, null)
        chatToSave.add(logMessage)

        updateLogFile()
    }

    public fun logMessageUser(prompt: String) {
        //println(logFile.absolutePath)
        /*
        try {
            logFile.appendText(
                "Author: User" + "\n" +
                        "DateTime: ${java.time.LocalDateTime.now()}\n" +
                        "Message: $prompt\n\n"
            )
        } catch (exception : Exception){
            println("Couldn't write file")
        }
        */

        val logMessage = LogMessage("user", prompt.trim(), java.time.LocalDateTime.now(), null, null)
        chatToSave.add(logMessage)

        updateLogFile()
    }

    private fun updateLogFile() {
        logFile.delete()
        logFile.createNewFile()
        for (message in chatToSave) {
            logFile.appendText(message.toString() + "\n")
        }
    }

    fun addPromptMessage(prompt: String) {
        val message = Message("user", prompt)
        messages.add(message)
        chatLog.add(message)
    }

    fun getChatLog(): String {
        var log = ""

        for (message in chatLog) {
            if (message.role == "user")
            {
                log += "User: " + message.content + "\n"
            } else {
                log += "ChatGPT: " + message.content + "\n"
            }
        }

        return log
    }

    fun getChatLogHtml(): String {
        var log = ""

        for (message in chatLog) {
            if (message.role == "user")
            {
                log += "User: " + message.content + "<br><br>"
            } else {
                log += "ChatGPT: " + message.content + "<br><br>"
            }
        }

        log.removeSuffix("<br><br>")

        return log
    }

    fun getLastBlockOfCode(): String? {
        val codeBlockDelimiter = "```"

        if (!chatToSave.last().isFromGPT()) {
            return null
        }

        try {
            var messageContent = chatToSave.last().getContent()

            var startIndex = messageContent.indexOf(codeBlockDelimiter)
            messageContent = messageContent.substring(startIndex, messageContent.length)

            startIndex = messageContent.indexOf("\n")
            messageContent = messageContent.substring(startIndex, messageContent.length)

            val endIndex = messageContent.lastIndexOf(codeBlockDelimiter)

            return messageContent.substring(0, endIndex)

        } catch (e: Exception) {
            println("IDK some error")
            return null
        }
    }

    fun markLastResponseAs(useful: Boolean) {
        for (message in chatToSave.reversed()) {
            if (!message.isFromGPT()) {
                break
            }
            message.markAs(useful)
        }
        updateLogFile()
    }
}