package org.dropProject.dropProjectPlugin.submissionComponents

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dropProject.dropProjectPlugin.gpt.GptInteraction
import org.dropProject.dropProjectPlugin.settings.SettingsState
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*


/*
class UIGpt {

    var gptInteraction = GptInteraction()

    fun buildComponents(): JBScrollPane {

        val panel = JPanel()

        val textField = JBTextField()
        textField.emptyText.text = "placeholder"

        val responseArea = JBTextArea("Response")
        responseArea.foreground = JBColor.WHITE
        responseArea.background = JBColor.BLACK

        //exprimentar a cena do editor n sei q que o gpt recomenda

        val button = JButton("Send Prompt")
        button.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {

                if (textField.text != null && textField.text != "") {
                    gptInteraction.executePrompt(textField.text)
                    responseArea.text = gptInteraction.getChatLog()
                }

            }
        })

        panel.add(button)
        panel.add(textField)
        panel.add(responseArea)


        val scrollPane = JBScrollPane(panel)
        val viewport: JViewport = scrollPane.viewport
        viewport.scrollMode = JViewport.SIMPLE_SCROLL_MODE
        scrollPane.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        viewport.extentSize = Dimension(0, 0)

        return scrollPane
    }
}
*/

/*
class UIGpt {

    var gptInteraction = GptInteraction()

    fun buildComponents(): JBScrollPane {

        val panel = JPanel()

        val textField = JBTextField()
        textField.emptyText.text = "placeholder"

        val responseArea = JBTextArea("Response")
        responseArea.foreground = JBColor.WHITE
        responseArea.background = JBColor.BLACK



        val button = JButton("Send Prompt")
        button.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {

                if (textField.text != null && textField.text != "") {
                    gptInteraction.executePrompt(textField.text)
                    responseArea.text = gptInteraction.getChatLog()
                }

            }
        })

        panel.add(button)
        panel.add(textField)
        panel.add(responseArea)


        val scrollPane = JBScrollPane(panel)
        val viewport: JViewport = scrollPane.viewport
        viewport.scrollMode = JViewport.SIMPLE_SCROLL_MODE
        scrollPane.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        viewport.extentSize = Dimension(0, 0)

        return scrollPane
    }
}
*/


class UIGpt(var project: Project) {


    private var gptInteraction = GptInteraction(project)
    private var textField = JBTextField()
    private var phrases = ArrayList<String>()
    private var sendButton = JButton()
    private var phraseComboBox = JComboBox(phrases.toTypedArray())
    private val responseArea = JEditorPane()
    private var inputAndSubmitPanel = JPanel(GridBagLayout())
    private var uI: JBScrollPane = JBScrollPane()
    private var chatHtml = ChatHtmlBuilder()
    private var usefulButton = JButton("Useful")
    private var notUsefulButton = JButton("Not Useful")
    private var askTwice = false

    init {
        textField.emptyText.text = "Send a message"
        textField.preferredSize = Dimension(400, 30)  // Set a preferred size for the textField


        responseArea.apply {
            contentType = "text/html"

            editorKit = HTMLEditorKitBuilder().build().also {
                it.styleSheet.addStyleSheet(chatHtml.getStyle())
            }

            isEditable = false
            foreground = JBColor.foreground()
            background = JBColor.background()
            isOpaque = true
            text = chatHtml.getHtmlChat()

            UIUtil.doNotScrollToCaret(this)
            UIUtil.invokeLaterIfNeeded {
                revalidate()
                setCaretPosition(document.length)
            }

        }

        val settingsState = SettingsState.getInstance()
        phrases = ArrayList(settingsState.sentenceList)

        phrases.add(0, "") //Option that doesn't add anything to the prompt

        phraseComboBox = JComboBox(phrases.toTypedArray())
        phraseComboBox.preferredSize = Dimension(200, 30)


        val scope = CoroutineScope(Dispatchers.Default)

        usefulButton.addActionListener {
            gptInteraction.markLastResponseAs(true)

        }

        notUsefulButton.addActionListener {
            gptInteraction.markLastResponseAs(false)
        }


        sendButton = JButton("Send Message")
        sendButton.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                gptInteraction.model = "gpt-3.5-turbo"
                sendPrompt()
            }
        })

        val usefulButtonConstraints = GridBagConstraints()
        usefulButtonConstraints.fill = GridBagConstraints.HORIZONTAL
        usefulButtonConstraints.weightx = 0.5
        usefulButtonConstraints.insets = JBUI.insets(3) // Custom padding
        usefulButtonConstraints.gridx = 0 // Place at column 0
        usefulButtonConstraints.gridy = 0 // Place in the first row

        val notUsefulButtonConstraints = GridBagConstraints()
        notUsefulButtonConstraints.fill = GridBagConstraints.HORIZONTAL
        notUsefulButtonConstraints.weightx = 0.5
        notUsefulButtonConstraints.insets = JBUI.insets(3) // Custom padding
        notUsefulButtonConstraints.gridx = 1 // Place at column 1
        notUsefulButtonConstraints.gridy = 0 // Place in the first row

        val textFieldConstraints = GridBagConstraints()
        textFieldConstraints.fill = GridBagConstraints.HORIZONTAL
        textFieldConstraints.weightx = 1.0
        textFieldConstraints.gridwidth = 3 // Span three columns
        textFieldConstraints.insets = JBUI.insets(3) // Custom padding
        textFieldConstraints.gridx = 0 // Place at column 0
        textFieldConstraints.gridy = 1 // Place in the second row

        val phraseComboBoxConstraints = GridBagConstraints()
        phraseComboBoxConstraints.fill = GridBagConstraints.HORIZONTAL
        phraseComboBoxConstraints.weightx = 0.5
        phraseComboBoxConstraints.insets = JBUI.insets(3) // Custom padding
        phraseComboBoxConstraints.gridx = 0 // Place at column 0
        phraseComboBoxConstraints.gridy = 2 // Place in the third row

        val checkBoxConstraints = GridBagConstraints()
        checkBoxConstraints.fill = GridBagConstraints.HORIZONTAL
        checkBoxConstraints.weightx = 0.1 // Increased weightx value
        checkBoxConstraints.insets = JBUI.insets(3, 30, 3, 3) // Add more padding to the right
        checkBoxConstraints.gridx = 1 // Place at column 1
        checkBoxConstraints.gridy = 2 // Place in the third row

        val sendButtonConstraints = GridBagConstraints()
        sendButtonConstraints.fill = GridBagConstraints.HORIZONTAL
        sendButtonConstraints.weightx = 1.0
        sendButtonConstraints.gridwidth = 3 // Span three columns
        sendButtonConstraints.insets = JBUI.insets(3) // Custom padding
        sendButtonConstraints.gridx = 0 // Place at column 0
        sendButtonConstraints.gridy = 3 // Place in the fourth row

        val askTwiceCheckBox = JCheckBox("Ask for 2 solutions")



        askTwiceCheckBox.addActionListener { _ ->
            askTwice = askTwiceCheckBox.isSelected
        }

        inputAndSubmitPanel = JPanel(GridBagLayout())
        inputAndSubmitPanel.add(usefulButton, usefulButtonConstraints)
        inputAndSubmitPanel.add(notUsefulButton, notUsefulButtonConstraints)
        inputAndSubmitPanel.add(textField, textFieldConstraints)
        inputAndSubmitPanel.add(phraseComboBox, phraseComboBoxConstraints)
        inputAndSubmitPanel.add(askTwiceCheckBox, checkBoxConstraints) // Add checkbox in the second column
        inputAndSubmitPanel.add(sendButton, sendButtonConstraints) // Add send button in the third column


        inputAndSubmitPanel.preferredSize = Dimension(600, 140) // Increased height to accommodate the taller button


        responseArea.size = Dimension(200, -1)

        val panel = JPanel()
        panel.layout = BorderLayout()
        panel.add(responseArea, BorderLayout.CENTER)
        panel.add(inputAndSubmitPanel, BorderLayout.SOUTH)
        panel.size = Dimension(800, -1)


        val scrollPane = JBScrollPane(panel)
        val viewport: JViewport = scrollPane.viewport
        viewport.scrollMode = JViewport.SIMPLE_SCROLL_MODE
        scrollPane.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        viewport.extentSize = Dimension(0, 0)

        uI = scrollPane

    }


    fun buildComponents(): JBScrollPane {

        //val editor: Editor = DataManager.getInstance().getDataContext().getData(DataConstants.EDITOR) as Editor
        //val editor2: Editor = DataManager.getInstance().dataContextFromFocusAsync.getData(DataConstants.EDITOR) as Editor
        //DataManager.getInstance().saveInDataContext(DataManager.getInstance().getDataContext(), Key.create("ObjectGPTUI"), this)
        //editor.putUserData(Key.create("ObjectGPTUI"), this)

        return uI
    }


    /*
    private fun escapeKotlinSpecialCharacters(input: String): String {
        // Define the characters to be escaped
        val specialCharacters = setOf("\\", "$", "\"", "\n", "\r", "\t", "\b", "\u000c")

        // Escape each special character
        var escaped = input
        for (character in specialCharacters) {
            //escaped = escaped.replace(character, "\\$character")
            escaped = escaped.replace(character, "") // TEST if it woorks well now
        }

        escaped = escaped.replace("'", "") //for some reason this character breaks everything

        return escaped
    }
    */

    private fun escapeKotlinSpecialCharacters(input: String): String {
        return input.replace("[\\\\$\"\\n\\r\\t\b\\u000c']".toRegex(), "")
    }

    fun addToPrompt(text: String, model: String = "gpt-3.5-turbo") {
        gptInteraction.model = model
        textField.text += text
    }

    fun sendPrompt() {
        val scope = CoroutineScope(Dispatchers.Default)

        if (textField.text != null && textField.text != "") {
            sendButton.isEnabled = false

            val selectedPhrase = phraseComboBox.selectedItem as String
            val message = "${textField.text} $selectedPhrase"

            val escapedMessage = escapeKotlinSpecialCharacters(message)

            textField.text = ""

            // Start a coroutine to perform the GPT interaction
            scope.launch(Dispatchers.Default) {

                //Adding the prompt that is being sent
                gptInteraction.addPromptMessage(escapedMessage)
                chatHtml.append("User", escapedMessage, true)
                gptInteraction.logMessageUser(escapedMessage)
                updateChatScreen()

                val response = gptInteraction.executePrompt(escapedMessage)
                //Adding the response

                chatHtml.append("ChatGPT", response, false)
                updateChatScreen()

                if (askTwice) {
                    val altResponse = gptInteraction.executePrompt(escapedMessage)

                    chatHtml.append("ChatGPT", altResponse, false)
                    updateChatScreen()

                    SwingUtilities.invokeLater {
                        openDiffViewer(response, altResponse)
                    }
                }

                SwingUtilities.invokeLater {
                    sendButton.isEnabled = true
                }

            }
        }
    }

    private fun openDiffViewer(response1: String, response2: String) {
        val project = project

        val content1 = DiffContentFactory.getInstance().create(project, response1)
        val content2 = DiffContentFactory.getInstance().create(project, response2)

        val request = SimpleDiffRequest("Response Comparison", content1, content2, "Original Response", "Alternative Response")
        DiffManager.getInstance().showDiff(project, request)
    }

    private fun updateChatScreen() {
        responseArea.text = chatHtml.getHtmlChat()
        println(chatHtml.getHtmlChat())
    }

    fun updatePhrases() {
        val settingsState = SettingsState.getInstance()
        phrases = ArrayList(settingsState.sentenceList)

        phrases.add("") //Option that doesn't add anything to the prompt


        phraseComboBox.removeAll()
    }

    fun addPhrase(phrase : String) {
        phraseComboBox.addItem(phrase)
    }

    fun removePhrase(index : Int) {
        phraseComboBox.removeItemAt(index)
    }

    fun editPhrase(selectedIndex : Int, editedSentence : String) {
        phraseComboBox.removeItemAt(selectedIndex)
        phraseComboBox.insertItemAt(editedSentence, selectedIndex)
        phraseComboBox.selectedIndex = selectedIndex
    }

    companion object {
        var instance1 : UIGpt? = null

        fun getInstance(project: Project) : UIGpt {

            if(instance1 == null) {
                instance1 = UIGpt(project)
            }
            return instance1!!
        }
    }
}
