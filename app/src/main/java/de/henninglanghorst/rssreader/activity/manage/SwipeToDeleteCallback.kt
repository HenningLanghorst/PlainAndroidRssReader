package de.henninglanghorst.rssreader.activity.manage

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.View
import de.henninglanghorst.rssreader.R

class SwipeToDeleteCallback(
        private val context: Context,
        private val remove: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val background = ColorDrawable().apply { color = Color.parseColor("#f44336") }
    private val textPaint = Paint().apply { textSize = 50f; color = Color.WHITE; textAlign = Paint.Align.RIGHT }

    override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder) = false

    override fun onChildDraw(
            canvas: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
            dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        if (dX != 0f || isCurrentlyActive) {
            val itemView = viewHolder.itemView

            background.apply {
                bounds = Rect(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                draw(canvas)
            }
            canvas.drawDeleteTextInBoundsOf(itemView)
        }
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun Canvas.drawDeleteTextInBoundsOf(view: View) {
        drawText(context.getString(R.string.delete), view.textX, view.textY, textPaint)
    }

    private val View.textY get() = ((top + bottom) / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2)
    private val View.textX get() = (right - 20f)

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        viewHolder.adapterPosition
        remove(viewHolder.adapterPosition)
    }
}