package org.dropProject.dropProjectPlugin.submissionComponents

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.CheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dropProject.dropProjectPlugin.gpt.ChatHtmlBuilder
import org.dropProject.dropProjectPlugin.gpt.GptInteraction
import org.dropProject.dropProjectPlugin.gpt.Message
import org.dropProject.dropProjectPlugin.gpt3_5Model
import org.dropProject.dropProjectPlugin.settings.SettingsState
import java.awt.*
import java.awt.datatransfer.StringSelection
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
    //private var phraseComboBox = JComboBox(phrases.toTypedArray())
    private var phraseComboBox = JComboBox(phrases.toArray())
    private var phraseComboPanel = JPanel()
    private val responseArea = JEditorPane()
    private var inputAndSubmitPanel = JPanel(GridBagLayout())
    private var uI: JBScrollPane = JBScrollPane()
    private var chatHtml = ChatHtmlBuilder()
    private var usefulButton = JButton("Useful", AllIcons.Ide.LikeSelected)
    private var notUsefulButton = JButton("Not Useful", AllIcons.Ide.DislikeSelected)
    private var copyCodeButton = JButton("Copy Code")
    private var resetButton = JButton("Clear Chat", AllIcons.Actions.Refresh)
    var askTwiceCheckBox = CheckBox("Ask for 2 Solutions")
    var askTwice = false

    public var dpReportButton: JButton? = null

    init {

        dpReportButton = null

        disableUsefulnessButtons()

        //textField.emptyText.text = "Send a message"
        textField.emptyText.text = "Write your message here..."
        //textField.preferredSize = Dimension(400, 30)  // Set a preferred size for the textField


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

        //phraseComboBox = JComboBox(phrases.toTypedArray())
        //phraseComboBox.preferredSize = Dimension(200, 30)

        phraseComboPanel = createComboBoxPanel()
        //phraseComboBox.preferredSize = Dimension(200, 30)

        val scope = CoroutineScope(Dispatchers.Default)

        usefulButton.addActionListener {
            gptInteraction.markLastResponseAs(true)

            disableUsefulnessButtons()
        }

        notUsefulButton.addActionListener {
            gptInteraction.markLastResponseAs(false)

            disableUsefulnessButtons()
        }

        copyCodeButton.addActionListener {
            val codeBlock = gptInteraction.getLastBlockOfCode()
            if (codeBlock != null) {
                val stringSelection = StringSelection(codeBlock)
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(stringSelection, null)
            }
        }



        sendButton = JButton("Send Message")
        sendButton.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                gptInteraction.model = gpt3_5Model
                gptInteraction.fromDPReport = false
                sendPrompt()
            }
        })

        inputAndSubmitPanel = JPanel(GridBagLayout())


        //val askTwiceCheckBox = JCheckBox("Ask for 2 solutions")


        val gbc = GridBagConstraints()
        //gbc.weightx = 0.0
        //gbc.weighty = 0.0
        gbc.insets = JBUI.insets(3) // Uniform padding around components

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.BOTH
        inputAndSubmitPanel.add(copyCodeButton, gbc)

        // Configuration for the second row with two components
        gbc.gridwidth = 1 // Reset to default gridwidth

        // Useful Button
        gbc.gridx = 0
        gbc.gridy = 1
        //gbc.gridwidth = 1
        //gbc.weightx = 0.5
        //gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 0.5 // Each button gets half the space
        gbc.anchor = GridBagConstraints.CENTER // Center the button in its cell
        inputAndSubmitPanel.add(usefulButton, gbc)

        // Not Useful Button
        gbc.gridx = 1
        gbc.gridy = 1
        //gbc.gridwidth = 1
        //gbc.weightx = 0.5
        //gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 0.5 // Ensuring even split of horizontal space
        gbc.anchor = GridBagConstraints.CENTER // Center the button in its cell
        inputAndSubmitPanel.add(notUsefulButton, gbc)

        // TextField row
        gbc.gridx = 0
        gbc.gridy = 2
        //gbc.gridwidth = 2
        //gbc.weightx = 1.0
        gbc.gridwidth = 2 // TextField spans two columns
        gbc.weightx = 1.0 // Takes up remaining space
        gbc.fill = GridBagConstraints.BOTH
        inputAndSubmitPanel.add(textField, gbc)

        // Phrase Combo Panel and CheckBox
        gbc.gridwidth = 1 // Reset to default gridwidth
        gbc.weightx = 0.5 // Split the row evenly

        // Phrase Combo Panel
        gbc.gridx = 0
        gbc.gridy = 3
        //gbc.gridwidth = 1
        //gbc.weightx = 0.5
        //gbc.fill = GridBagConstraints.BOTH
        //inputAndSubmitPanel.add(phraseComboBox, gbc)
        gbc.fill = GridBagConstraints.CENTER
        inputAndSubmitPanel.add(phraseComboPanel, gbc)

        phraseComboBox.toolTipText = "The selected text will be suffixed to your prompt..."

        // CheckBox
        gbc.gridx = 1
        gbc.gridy = 3
        //gbc.gridwidth = 1
        //gbc.weightx = 0.5
        //gbc.fill = GridBagConstraints.BOTH
        gbc.fill = GridBagConstraints.CENTER
        inputAndSubmitPanel.add(askTwiceCheckBox, gbc)

        // Send Button spans two columns
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.BOTH
        inputAndSubmitPanel.add(sendButton, gbc)


        askTwiceCheckBox.addActionListener {
            askTwice = askTwiceCheckBox.isSelected
        }

        resetButton.addActionListener {
            resetChat()
        }


        inputAndSubmitPanel.preferredSize = Dimension(600, 180) // Increased height to accommodate the taller button

        responseArea.size = Dimension(200, -1)


        val panel = JPanel()
        panel.layout = BorderLayout()
        panel.add(resetButton, BorderLayout.NORTH)
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

    private fun createComboBoxPanel(): JPanel {
        phraseComboBox = JComboBox(phrases.toTypedArray())

        val label = JLabel("Suffix with:")

        // Create a JPanel and set BoxLayout to align items horizontally
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)

        // Add the label and combo box to the panel
        panel.add(label)
        panel.add(Box.createHorizontalStrut(5)) // Adds a fixed space between label and combo box
        panel.add(phraseComboBox)

        // Optional: Adjust spacing and alignment further if needed
        label.alignmentY = Component.CENTER_ALIGNMENT
        phraseComboBox.alignmentY = Component.CENTER_ALIGNMENT

        // Ensuring the panel does not stretch vertically more than necessary
        panel.maximumSize = panel.preferredSize

        return panel
    }

    private fun escapeKotlinSpecialCharacters(input: String): String {
        return input.replace("[\\\\$\"\\n\\r\\t\b\\u000c']".toRegex(), "")
    }

    fun addToPrompt(text: String, model: String = gpt3_5Model, fromDPReport: Boolean = false) {
        gptInteraction.model = model
        gptInteraction.fromDPReport = fromDPReport
        textField.text += text

        if(fromDPReport) {
            gptInteraction.chatToSave.clear()
            gptInteraction.messages.clear()

            val systemPrompt = gptInteraction.calcSystemPrompt()
            if (systemPrompt.isNotEmpty()) {
                gptInteraction.messages.add(Message("system", systemPrompt))
            }

            gptInteraction.restartLog()
        }
    }

    fun disableUsefulnessButtons() {
        usefulButton.isEnabled = false
        notUsefulButton.isEnabled = false

        sendButton.isEnabled = true
    }

    fun enableUsefulnessButtons() {
        usefulButton.isEnabled = true
        notUsefulButton.isEnabled = true

        sendButton.isEnabled = false
    }

    fun enableDPReportButton() {
        if(dpReportButton != null) {
            dpReportButton!!.isEnabled = true
        }
    }

    fun sendPrompt() {
        val scope = CoroutineScope(Dispatchers.Default)

        if (textField.text != null && textField.text != "") {
            disableUsefulnessButtons()

            sendButton.isEnabled = false // block until the response arrives

            val selectedPhrase = phraseComboBox.selectedItem as String
            val message = "${textField.text} $selectedPhrase"

            val escapedMessage = escapeKotlinSpecialCharacters(message)

            textField.text = ""

            // Start a coroutine to perform the GPT interaction
            scope.launch(Dispatchers.Default) {

                //Adding the prompt that is being sent
                gptInteraction.addPromptMessage(escapedMessage)
                chatHtml.append("User", escapedMessage, true)

                if (gptInteraction.fromDPReport) {
                    gptInteraction.logMessageUser(escapedMessage)
                }

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

                if (!gptInteraction.fromDPReport) {
                    SwingUtilities.invokeLater {
                        sendButton.isEnabled = true // enable after a response arrives
                    }
                }

            }
        }

//        if (gptInteraction.fromDPReport && !gptInteraction.gptResponseError) {
//            enableUsefulnessButtons()
//        }

        //gptInteraction.fromDPReport = false
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
        //println(chatHtml.getHtmlChat())

        SwingUtilities.invokeLater {
            uI.verticalScrollBar.value = uI.verticalScrollBar.maximum
        }
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

    private fun resetChat() {
        chatHtml.reset()
        gptInteraction.reset()
        updateChatScreen()
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
