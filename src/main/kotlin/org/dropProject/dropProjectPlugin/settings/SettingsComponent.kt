package org.dropProject.dropProjectPlugin.settings

import com.intellij.ui.JBColor
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.*
import com.intellij.util.ui.FormBuilder
import org.jetbrains.annotations.NotNull
import java.awt.*
import javax.swing.*


class SettingsComponent {
    private val mainPanel: JPanel
    private val serverURL = JBTextField()
    private var nameField = JBTextField()

    private val numberField = JBTextField()
    private val tokenField = JBPasswordField()
    private val showToken = JBCheckBox("Show")

    private val openAiTokenField = JBPasswordField()
    private val showOpenAiToken = JBCheckBox("Show")
    private val openAiTokenPanel = JPanel(BorderLayout())
    private val autoSendPrompt = JBCheckBox("<html>Send prompt automatically.<br>If checked, GPT will be prompted as soon as the \"Ask GPT\" button is clicked.<br>Otherwise, the plugin will wait for further input to be added before sending the request.</html>") // New checkbox
    private val tokenpanel = JPanel(BorderLayout())

    private val addEditRemovePanel = JPanel()
    private val sentenceListModel: DefaultListModel<String> = DefaultListModel()
    private var sentenceList: JBList<String> = JBList(sentenceListModel)
    private val sentenceTextField: JBTextField = JBTextField()
    private val addButton = JButton("Add")
    private val editButton = JButton("Edit")
    private val removeButton = JButton("Remove")

    private val createOpenAiAccountButton = JButton("Create a new OpenAI account")

    init {
        // TOKEN FIELD AND SHOW CHECKBOX COMBINED
        tokenpanel.add(tokenField, BorderLayout.CENTER)
        tokenpanel.add(showToken, BorderLayout.EAST)

        openAiTokenPanel.add(openAiTokenField, BorderLayout.CENTER)
        openAiTokenPanel.add(showOpenAiToken, BorderLayout.EAST)

        sentenceTextField.emptyText.text = "Text to be added at the end of the prompt"

        if (sentenceListModel.isEmpty || sentenceListModel.size == 1) {
            populateDefaultSentences()
        }

        // BUILD SETTINGS FORM
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(createGeneralSettingsPanel(), 1)
            .addComponent(createOpenAiSettingsPanel(), 1)
            .addComponentFillVertically(createAddEditRemovePanel(), 1) // Add add/edit/remove panel
            .panel

        showToken.addActionListener {
            val checkbox = it.source as JCheckBox
            tokenField.echoChar = if (checkbox.isSelected) 0.toChar() else '\u2022'
        }

        showOpenAiToken.addActionListener {
            val checkbox = it.source as JCheckBox
            openAiTokenField.echoChar = if (checkbox.isSelected) 0.toChar() else '\u2022'
        }

        addButton.addActionListener {
            addSentence()
        }

        editButton.addActionListener {
            editSentence()
        }

        removeButton.addActionListener {
            removeSentence()
        }
    }

    private fun createGeneralSettingsPanel(): JPanel {
        val panel = JPanel(BorderLayout())

        panel.add(createSettingsLine("General Settings"), BorderLayout.NORTH)
        panel.add(
            FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel("Server URL: "), serverURL, 1, false)
                .addLabeledComponent(JBLabel("Name: "), nameField, 1, false)
                .addLabeledComponent(JBLabel("Number: "), numberField, 1, false)
                .addLabeledComponent(JBLabel("Token: "), tokenpanel, 1, false)
                .panel.apply {
                    border = BorderFactory.createEmptyBorder(0, 20, 0, 0) // Add left indentation
                }, BorderLayout.CENTER
        )

        return panel
    }

    private fun createOpenAiSettingsPanel(): JPanel {
        val panel = JPanel(BorderLayout())

        panel.add(createSettingsLine("OpenAI Settings"), BorderLayout.NORTH)
        panel.add(
            FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel("OpenAI API Key: "), openAiTokenPanel, 1, false)
                .addLabeledComponent(JBLabel("Send to ChatGPT: "), autoSendPrompt, 1, false)
                .addComponent(createOpenAiAccountButton)
                .panel.apply {
                    border = BorderFactory.createEmptyBorder(0, 20, 0, 0) // Add left indentation
                }, BorderLayout.CENTER
        )

        return panel
    }

    private fun populateDefaultSentences() {
        val defaultSentences = listOf("Find the bug", "Improve the performance", "Explain this code", "Write tests for this function")
        defaultSentences.forEach {
            sentenceListModel.addElement(it)
        }
    }

    private fun createSettingsLine(title: String): JPanel {
        val panel = JPanel()
        panel.layout = BorderLayout()

        val sep = TitledSeparator(title)

        panel.add(sep, BorderLayout.CENTER)

        return panel
    }

    private fun createAddEditRemovePanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        val scrollPane = JBScrollPane(sentenceList)
        scrollPane.preferredSize = Dimension(0, 100)

        val emptyBorder = BorderFactory.createEmptyBorder(3, 3, 3, 3)
        val lineBorder = BorderFactory.createLineBorder(JBColor(0xB5B5B5, 0x5C5C5C))
        val compoundBorder = BorderFactory.createCompoundBorder(emptyBorder, lineBorder)
        scrollPane.border = compoundBorder

        // Adjust the preferred size to limit the height of the list
        sentenceList.preferredSize = Dimension(300, 100)

        // Create a sub-panel for buttons with GridLayout (equal-sized cells)
        val buttonPanel = JPanel(GridLayout(1, 0))
        buttonPanel.add(addButton)
        buttonPanel.add(editButton)
        buttonPanel.add(removeButton)

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        panel.add(scrollPane, gbc)

        gbc.gridy = 1
        gbc.weighty = 0.0
        panel.add(sentenceTextField, gbc)

        gbc.gridy = 2
        panel.add(buttonPanel, gbc)

        panel.border = BorderFactory.createEmptyBorder(0, 20, 0, 0) // Add left indentation

        return panel
    }

    private fun addSentence() {
        val sentence = sentenceTextField.text
        if (sentence.isNotEmpty()) {
            sentenceListModel.addElement(sentence)
            sentenceTextField.text = ""
        }

        //needs to update the interface
    }

    private fun editSentence() {
        val selectedIndex = sentenceList.selectedIndex
        if (selectedIndex != -1) {
            val editedSentence = JOptionPane.showInputDialog(
                mainPanel,
                "Edit Sentence:",
                sentenceListModel.getElementAt(selectedIndex)
            )
            if (editedSentence != null) {
                sentenceListModel.setElementAt(editedSentence, selectedIndex)
            }
        }

        //needs to update the interface
    }

    private fun removeSentence() {
        val selectedIndex = sentenceList.selectedIndex
        if (selectedIndex != -1) {
            sentenceListModel.remove(selectedIndex)
        }

        //needs to update the interface
    }

    fun getPanel(): JPanel {
        return mainPanel
    }

    fun getServerURL(): String {
        return serverURL.text
    }

    fun setServerURL(@NotNull text: String) {
        serverURL.text = text
    }

    fun getNameField(): String {
        return nameField.text
    }

    fun setNameField(@NotNull text: String) {
        nameField.text = text
    }

    fun getNumberField(): String {
        return numberField.text
    }

    fun setNumberField(@NotNull text: String) {
        numberField.text = text
    }

    fun getTokenField(): String {
        return String(tokenField.password)
    }

    fun setTokenField(token: String) {
        tokenField.text = token
    }

    fun isAutoSendPromptSelected(): Boolean {
        return autoSendPrompt.isSelected
    }

    fun setAutoSendPrompt(autoSend: Boolean) {
        autoSendPrompt.isSelected = autoSend
    }

    fun getPreferredFocusedComponent(): JComponent {
        return nameField
    }

    fun getOpenAiTokenField(): String {
        return String(openAiTokenField.password)
    }

    fun setOpenAiTokenField(token: String) {
        openAiTokenField.text = token
    }

    fun getSentenceList(): List<String> {
        return sentenceListModel.elements().toList()
    }

    fun setSentenceList(sentences: List<String>) {
        sentenceListModel.clear()
        sentences.forEach { sentenceListModel.addElement(it) }
    }
}
