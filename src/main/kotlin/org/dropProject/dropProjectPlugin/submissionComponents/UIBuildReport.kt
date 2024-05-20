package org.dropProject.dropProjectPlugin.submissionComponents

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import data.FullBuildReport
import org.dropProject.dropProjectPlugin.DefaultNotification
import org.dropProject.dropProjectPlugin.gpt4Model
import org.dropProject.dropProjectPlugin.settings.SettingsState
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.JButton
import javax.swing.JViewport


internal class UIBuildReport(private val project: Project) {
    fun buildComponents(buildReport: FullBuildReport, submissionNumber: Int?): JBScrollPane {
        val panel = panel {
            row {
                label("Build Report").bold()
                submissionNumber?.let {
                    comment("Submission: $submissionNumber")
                }
            }
            row {
                comment("Assignment: ${buildReport.assignment!!.name}")
            }
            row {
                val fullDate = buildReport.submission!!.submissionDate.split(".")[0].split("T")
                comment("Submitted: ${fullDate[0]} | ${fullDate[1]}")
            }
            buildReport.summary?.let { self ->
                self.forEach { summary ->
                    row {
                        when (summary.reportKey) {
                            "PS" -> {
                                label("Project Structure")
                                if (summary.reportValue == "OK") {
                                    comment("<icon src='AllIcons.Actions.Commit'>&nbsp;")
                                } else {
                                    comment("<icon src='AllIcons.Actions.Suspend'>&nbsp;")
                                }
                            }

                            "C" -> {
                                label("Compilation")
                                if (summary.reportValue == "OK") {
                                    comment("<icon src='AllIcons.Actions.Commit'>&nbsp;")
                                } else {
                                    comment("<icon src='AllIcons.Actions.Suspend'>&nbsp;")
                                }
                            }

                            "TT" -> {
                                label("Teacher Unit Tests")
                                if (summary.reportValue == "OK") {
                                    comment("<icon src='AllIcons.Actions.Commit'>&nbsp;<b>${summary.reportProgress}/${summary.reportGoal}</b>")
                                } else { //OK NOK ..?.. NULL??...CHECK THIS
                                    comment("<icon src='AllIcons.Actions.Suspend'>&nbsp;<b>${summary.reportProgress}/${summary.reportGoal}</b>")
                                }
                            }

                            "CS" -> {
                                label("Code Quality")
                                if (summary.reportValue == "OK") {
                                    comment("<icon src='AllIcons.Actions.Commit'>&nbsp;")
                                } else {
                                    comment("<icon src='AllIcons.Actions.Suspend'>&nbsp;")
                                }
                            }
                        }
                    }.layout(RowLayout.PARENT_GRID)
                }
            }
            //PROJECT STRUCTURE
            buildReport.structureErrors?.let { psErr ->
                collapsibleGroup("Project Structure Errors (${psErr.size})") {
                    psErr.forEachIndexed { index, error ->
                        group("Error $index") {
                            row {
                                text(error)
                            }
                            row {
                                button("Send to ChatGPT") {
                                    sendToChatGPTAction(error, it)
                                }
                            }
                        }
                    }
                }
            }
            //COMPILATION
            buildReport.buildReport?.compilationErrors?.let { cErr ->
                collapsibleGroup(
                    "Compilation Errors (${
                        cErr.filter { it.contains("[TEST]") }.size
                    })"
                ) {
                    cErr.joinToString("<br>").trim().split("[TEST]").forEach { error ->
                        if (error.isNotBlank()) {
                            group {
                                row {
                                    text(error)
                                }
                                row {
                                    button("Send to ChatGPT") {
                                        sendToChatGPTAction(error, it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            //CHECKSTYLE
            buildReport.buildReport?.checkstyleErrors?.let { csErr ->
                collapsibleGroup("Code Quality Errors (${csErr.size})") {
                    csErr.forEach { error ->
                        group {
                            row {
                                text(error)
                            }
                            row {
                                button("Send to ChatGPT") {
                                    sendToChatGPTAction(error, it)
                                }
                            }
                        }
                    }
                }
            }
            //TEACHER UNIT TESTS
            buildReport.buildReport?.junitErrorsTeacher?.let { ttErr ->
                collapsibleGroup("Teacher Units Tests Errors") {
                    ttErr.split("ERROR: |FAILURE:".toRegex()).forEach { error ->
                        if (error.isNotBlank()) {
                            group {
                                row {
                                    text(
                                        error.replace("<", "&lt;").replace("\n", "<br>")
                                    )
                                }
                                row {
                                    button("Send to ChatGPT") {
                                        sendToChatGPTAction(error, it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            //SUMMARY
            buildReport.buildReport?.junitSummaryTeacher?.let { summary ->
                group {
                    row {
                        text("$summary<br>")
                    }
                }
            }


        }
        val scrollPane = JBScrollPane(panel)
        val viewport: JViewport = scrollPane.viewport
        viewport.scrollMode = JViewport.SIMPLE_SCROLL_MODE
        scrollPane.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        viewport.extentSize = Dimension(0, 0)

        return scrollPane
    }


    private fun sendToChatGPTAction(error : String) {
        val uiGPT = UIGpt.getInstance(project)
        uiGPT.addToPrompt(error, gpt4Model, true)

        val message = "The error has been sent to GPT. Check the ChatGPT tab for more information."
        DefaultNotification.notify(project, message)

        //val settingsState = SettingsState.getInstance()
        //if (settingsState.autoSendPrompt) {
            uiGPT.sendPrompt()
        //}
    }

    private fun sendToChatGPTAction(error: String, it: ActionEvent) {
        // disable the button that originated this event (i.e. arg `it')
        val button :JButton = it.source as JButton
        button.isEnabled = false

        val uiGPT = UIGpt.getInstance(project)
        // store the pressed button in order to enable it again after
        // the response arrives
        uiGPT.dpReportButton = button

        // ignore previously selected options since we don't want them to mess with the experiment
        uiGPT.askTwiceCheckBox.isSelected = false
        uiGPT.addToPrompt(error, gpt4Model, true)

        val message = "The error has been sent to GPT. Check the ChatGPT tab for more information."
        DefaultNotification.notify(project, message)

        //val settingsState = SettingsState.getInstance()
        //if (settingsState.autoSendPrompt) {
            uiGPT.sendPrompt()
        //}

    }
}