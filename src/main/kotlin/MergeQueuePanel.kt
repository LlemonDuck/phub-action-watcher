package com.duckblade

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class MergeQueuePanel : JPanel() {

    private val queue = ArrayDeque<Int>()
    private val queueDisplay = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    init {
        layout = BorderLayout()

        add(JLabel("Merge Queue").apply {
            alignmentX = CENTER_ALIGNMENT
            alignmentY = CENTER_ALIGNMENT
            verticalAlignment = SwingConstants.CENTER
            horizontalAlignment = SwingConstants.CENTER
        }, BorderLayout.NORTH)

        add(queueDisplay, BorderLayout.CENTER)

        val input = JTextField().apply {
            addActionListener {
                val prNumber = text.toIntOrNull() ?: return@addActionListener
                queue.add(prNumber)
                text = ""
                rerender()
            }
        }
        add(input, BorderLayout.SOUTH)
    }

    private fun rerender() {
        queueDisplay.removeAll()

        synchronized(queue) {
            queue.forEach { pr ->
                queueDisplay.add(JLabel(pr.toString()).apply {
                    alignmentX = CENTER_ALIGNMENT
                    alignmentY = CENTER_ALIGNMENT
                    verticalAlignment = SwingConstants.CENTER
                    horizontalAlignment = SwingConstants.CENTER

                    addMouseListener(object : MouseListener {
                        override fun mouseClicked(e: MouseEvent) {
                            if (SwingUtilities.isRightMouseButton(e)) {
                                synchronized(queue) {
                                    queue.remove(pr)
                                }
                                SwingUtilities.invokeLater { rerender() }
                            }
                        }

                        override fun mousePressed(e: MouseEvent?) {}
                        override fun mouseReleased(e: MouseEvent?) {}
                        override fun mouseEntered(e: MouseEvent?) {}
                        override fun mouseExited(e: MouseEvent?) {}
                    })
                })
            }
        }

        revalidate()
        repaint()
    }

    @Synchronized
    fun pop(): Int? {
        val e = synchronized(queue) {
            queue.removeFirstOrNull()
        }
        SwingUtilities.invokeLater { rerender() }
        return e
    }

}