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
import org.dropProject.dropProjectPlugin.submissionComponents.UIGpt
import java.io.File
import java.nio.file.FileSystems
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

import kotlin.random.Random

class GptInteraction(var project: Project) {
    var model = ""
    var gptResponseError = false

    private val separator = FileSystems.getDefault().separator
    //private val logFileDirectory = "${System.getProperty("user.home")}${separator}Documents${separator}Drop Project Plugin${separator}"
    private val logFileDirectory = project.let { FileEditorManager.getInstance(it).project.basePath.toString() }
    private val dateTime = Date()
    private val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")

//    private val logFile = File("${logFileDirectory}${separator}chat_logs${separator}chat_log_${formatter.format(dateTime)}.txt")
    private var logFileJSON = File("${logFileDirectory}${separator}chat_logs${separator}chat_log_${formatter.format(dateTime)}.json")

    private var responseLog = ArrayList<GPTResponse>()
    private var chatLog = ArrayList<Message>()
    var chatToSave = ArrayList<LogMessage>()

    // indicates if the Chat was open from the "Send to ChatGPT" button available in the DP Submission Report
    var fromDPReport = false
    var customSystemPrompt = false

    /*
    private var messages = mutableListOf(
        Message("system", "You are a helpful assistant"),
    )
    */
    val messages = ArrayList<Message>()

    init {
//        prepareLogFile(logFile)

        // Only Log when executing from the DP Report
        //prepareLogFile(logFileJSON)
    }

    fun restartLog() {
        println("restart Log")
        val dateTime_new = Date()
        logFileJSON = File("${logFileDirectory}${separator}chat_logs${separator}chat_log_${formatter.format(dateTime_new)}.json")
        prepareLogFile(logFileJSON)
    }

    fun prepareLogFile(file: File) {
        val fileParent = file.parentFile
        if (!fileParent.exists()) {
            fileParent.mkdirs() // Creating the parent directories if they don't exist
        }
        if (!file.exists()) {
            file.createNewFile() // Creating the target file if it doesn't exist
        }
    }

    private fun customSystemPrompt(): String {
        val frase0 = "És um professor de informática. "
        val frase1 = "Estás-me a ajudar a resolver problemas com o meu código do projecto de Algoritmia e Estruturas de Dados. "
        val frase2 = "Vais-me dar dicas e sugestões concretas que eu possa aplicar no meu código para resolver esses problemas. "
        val frase3 = "Dá-me respostas em Português de Portugal (PT-PT)."
        return frase0 + frase1 + frase2 + frase3
    }

    /*
    fun calcSystemPrompt(): String {
        val default = "You are a helpful assistant"
        customSystemPrompt = false
        if(!fromDPReport) {
            return default
        }
        val rnd = Random.nextBoolean()
        if(rnd) {
            customSystemPrompt = true
            return customSystemPrompt()
        }
        return default
    }
    */

    fun useCustomSystemPrompt(): Boolean {
        if (!Plafond.hasEnoughPlafond(45)) {
            return false
        }

        // there's 25% probability of using a custom system prompt
        val probability = 0.25f
        val dice = Random.nextFloat()

        return dice < probability;
    }


    fun calcSystemPrompt(): String {
        customSystemPrompt = false

        val default = "Dá-me respostas em Português de Portugal (PT-PT)." // "You are a helpful assistant"

        if (!fromDPReport) {
            return default
        }

        if (useCustomSystemPrompt()) {
            customSystemPrompt = true
            return customSystemPrompt()
        }

        return default
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

    fun clean(key: String): String {
        val r = StringBuilder()
        val aux1 = 54
        val aux2 = 86
        for (char in key) {
            when {
                char.isUpperCase() -> {
                    val d = (char.code - aux1) % 26 + 65
                    r.append(d.toChar())
                }
                char.isLowerCase() -> {
                    val d = (char.code - aux2) % 26 + 97
                    r.append(d.toChar())
                }
                else -> {
                    r.append(char)
                }
            }
        }
        return r.toString()
    }

    private fun getBaseAPIKey(): String {
        val settingsState = SettingsState.getInstance()
        val apiKey = settingsState.openAiToken
        return apiKey
    }

    private fun getAPIKey(): String {
        return getBaseAPIKey()
    }

    private fun getAPIURL(): String {
        return "https://api.openai.com/v1/chat/completions"
    }

    private fun processPrompt(): String {
        gptResponseError = false


        if (!fromDPReport && !Plafond.hasEnoughPlafond(65)) {
            gptResponseError = true
            return "Error: Not enough plafond"
        }

        val apiKey = getAPIKey()

        if (apiKey == "") {
            DefaultNotification.notify(project, "No API key set")
            gptResponseError = true
            return "Error: No API key set"
        }

        val apiUrl = getAPIURL()

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

                if (fromDPReport) {
                    logMessageGpt(myResponse.error.message)
                }

                gptResponseError = true

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

            if (fromDPReport) {
                logMessageGpt(myResponse.choices.first().message.content)
            }

            if (fromDPReport && !gptResponseError) {
                UIGpt.getInstance(project).enableUsefulnessButtons()
                UIGpt.getInstance(project).enableDPReportButton();
            }

            return myResponse.choices.first().message.content

        } catch (exception: Exception) {
            //mostrar uma notificação a dizer que o chatgpt não respondeu
            gptResponseError = true

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
        val logMessage = LogMessage("ChatGPT", message.trim(), java.time.LocalDateTime.now(), model, null, null)
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

        val logMessage = LogMessage("user", prompt.trim(), java.time.LocalDateTime.now(), null, null, customSystemPrompt)
        chatToSave.add(logMessage)

        updateLogFile()
    }

    private fun updateLogFile() {
//        logFile.delete()
//        logFile.createNewFile()

        logFileJSON.delete()
        logFileJSON.createNewFile()

        logFileJSON.appendText("{\n")
        logFileJSON.appendText("\"value\": [\n")

        var i = 0
        val nrEntries = chatToSave.size

        for (message in chatToSave) {
//            logFile.appendText(message.toString() + "\n")
            //println("ALL YOUR JSON: " + message.writeToJSON())
            var commaIfNeeded = ""
            if(i < nrEntries - 1) {
                commaIfNeeded = ", "
            }
            logFileJSON.appendText(message.writeToJSON() + commaIfNeeded + "\n")
            i++
        }

        logFileJSON.appendText("]\n")
        logFileJSON.appendText("}\n")

    }

    fun addPromptMessage(prompt: String) {
        val message = Message("user", prompt)
        messages.add(message)
        chatLog.add(message)
    }

    fun getChatLog(): String {
        var log = ""

        for (message in chatLog) {
            if (message.role == "user") {
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
            if (message.role == "user") {
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

        if (chatToSave.isEmpty()) {
            return null
        }

        if (!chatToSave.last().isFromGPT()) {
            return null
        }

        try {
            var messageContent = chatToSave.last().getContent()

            var startIndex = messageContent.indexOf(codeBlockDelimiter)

            if (startIndex >= 0) {
                messageContent = messageContent.substring(startIndex, messageContent.length)

                startIndex = messageContent.indexOf("\n")
                messageContent = messageContent.substring(startIndex, messageContent.length)

                val endIndex = messageContent.lastIndexOf(codeBlockDelimiter)

                return messageContent.substring(0, endIndex)
            }

            return null
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

    fun reset() {

    }
}