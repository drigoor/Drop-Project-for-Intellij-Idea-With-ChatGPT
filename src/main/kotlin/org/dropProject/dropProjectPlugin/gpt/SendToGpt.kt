package org.dropProject.dropProjectPlugin.gpt

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.Messages
import org.dropProject.dropProjectPlugin.settings.SettingsState
import org.dropProject.dropProjectPlugin.submissionComponents.UIGpt
import org.jetbrains.annotations.NotNull

class SendToGptEditor : AnAction() {

    override fun actionPerformed(@NotNull e: AnActionEvent) {
        // Get the editor from the event
        val editor = e.dataContext.getData("editor")

        // Get the selection model to access the selected text
        val selectionModel = (editor as Editor).selectionModel
        val selectedText = selectionModel.selectedText

        // Execute your function with the selected code
        if (selectedText != null) {
            //DropProjectToolWindow.sendToGpt(selectedText) apagar a funcao
            //println(selectedText)

            val uiGPT = UIGpt.getInstance(e.project!!)
            uiGPT.addToPrompt(selectedText, "gpt-4") // só vai funcionar com gpt-4 se a opção "Send prompt automatically" is active

            val settingsState = SettingsState.getInstance()
            if (settingsState.autoSendPrompt) {
                uiGPT.sendPrompt()
            }

            //println(uiGPT.textField)
            //println(uiGPT.textField.text)
        } else {
            Messages.showInfoMessage("No text selected", "Send to ChatGPT")
        }
    }
}

class SendToGptConsole : AnAction() {

    override fun actionPerformed(@NotNull e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val caretModel: CaretModel? = editor?.caretModel
        val selectedText: String? = caretModel?.currentCaret?.selectedText

        if (selectedText != null) {
            //println(selectedText)

            val uiGPT = UIGpt.getInstance(e.project!!)
            uiGPT.addToPrompt(selectedText, "gpt-4") // só vai funcionar com gpt-4 se a opção "Send prompt automatically" is active

            val settingsState = SettingsState.getInstance()
            if (settingsState.autoSendPrompt) {
                uiGPT.sendPrompt()
            }

        } else {
            Messages.showInfoMessage("No text selected", "Send to ChatGPT")
        }
    }

}


