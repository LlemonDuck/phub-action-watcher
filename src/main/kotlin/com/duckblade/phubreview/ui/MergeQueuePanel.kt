package com.duckblade.phubreview.ui

import com.duckblade.phubreview.MergeQueue
import com.duckblade.phubreview.MergeState
import com.duckblade.phubreview.QueuedItem
import com.duckblade.phubreview.cs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*

class MergeQueuePanel(
    private val queue: MergeQueue,
) : JPanel() {

    private val queueDisplay = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    init {
        queue.addQueueListener {
            Dispatchers.Swing { rerender(it) }
        }

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
                runBlocking {
                    cs.launch {
                        queue.add(prNumber)
                    }
                }
                text = ""
            }
        }
        add(input, BorderLayout.SOUTH)
    }

    private fun rerender(items: List<QueuedItem>) {
        queueDisplay.removeAll()

        items.forEach { pr ->
            queueDisplay.add(JLabel().apply {
                text = pr.subject ?: "Loading #${pr.number}..."
                foreground = when (pr.state) {
                    MergeState.LOADING -> Color.GRAY
                    MergeState.READY -> Color.BLACK
                    MergeState.MERGING -> Color(0x39753B)
                }

                alignmentX = CENTER_ALIGNMENT
                alignmentY = CENTER_ALIGNMENT
                verticalAlignment = SwingConstants.CENTER
                horizontalAlignment = SwingConstants.CENTER

                addMouseListener(object : MouseListener {
                    override fun mouseClicked(e: MouseEvent) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            runBlocking {
                                launch(Dispatchers.Default) {
                                    queue.remove(pr.number)
                                }
                            }
                        }
                    }

                    override fun mousePressed(e: MouseEvent?) {}
                    override fun mouseReleased(e: MouseEvent?) {}
                    override fun mouseEntered(e: MouseEvent?) {}
                    override fun mouseExited(e: MouseEvent?) {}
                })
            })
        }

        revalidate()
        repaint()
    }

}
