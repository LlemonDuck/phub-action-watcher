package com.duckblade.phubreview.ui

import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.GridLayout
import javax.swing.JFrame

class WatcherFrame(
    private val statusPanel: StatusPanel,
    private val mergeQueuePanel: MergeQueuePanel,
) : JFrame() {
    
    init {
        layout = GridLayout(1, 2)
        background = Color.green
        setSize(500, 150)
        defaultCloseOperation = EXIT_ON_CLOSE
        isAlwaysOnTop = true
        
        add(statusPanel)
        add(mergeQueuePanel)

        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val wb = ge.maximumWindowBounds
        setLocation(0, wb.height - this.height)
    }
    
}