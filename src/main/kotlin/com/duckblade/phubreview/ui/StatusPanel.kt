package com.duckblade.phubreview.ui

import com.duckblade.phubreview.WorkflowRun
import com.duckblade.phubreview.humanFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.time.Clock

private val COLOR_SUCCESS = Color(0x39753B)
private val COLOR_FAILURE = Color(0x9F2929)
private val COLOR_RUNNING = Color(0xA97B0B)
private val COLOR_NO_DATA = Color(0x565656)

class StatusPanel : JPanel() {

    val label = JLabel().apply {
        foreground = Color.WHITE
        alignmentX = CENTER_ALIGNMENT
        alignmentY = CENTER_ALIGNMENT
        verticalAlignment = SwingConstants.CENTER
        horizontalAlignment = SwingConstants.CENTER
    }

    val timerLabel = JLabel().apply {
        foreground = Color.WHITE
        alignmentX = CENTER_ALIGNMENT
        alignmentY = CENTER_ALIGNMENT
        verticalAlignment = SwingConstants.CENTER
        horizontalAlignment = SwingConstants.CENTER
    }

    init {
        layout = BorderLayout()
        add(label, BorderLayout.CENTER)
        add(timerLabel, BorderLayout.SOUTH)

        runBlocking {
            updateActionsLabel(null)
            updateTimer("0s")
        }
    }

    suspend fun updateActionsLabel(lastRun: WorkflowRun?) = withContext(Dispatchers.Swing) {
        if (lastRun == null) {
            label.text = "no data"
            background = COLOR_NO_DATA
            return@withContext
        }

        label.text = (Clock.System.now() - lastRun.updatedAt).humanFormat()
        background = when (lastRun.status) {
            "completed" -> {
                if (lastRun.conclusion == "success")
                    COLOR_SUCCESS
                else
                    COLOR_FAILURE
            }

            else -> COLOR_RUNNING
        }
    }

    suspend fun updateTimer(s: String) = withContext(Dispatchers.Swing) {
        launch { timerLabel.text = s }
    }

}
