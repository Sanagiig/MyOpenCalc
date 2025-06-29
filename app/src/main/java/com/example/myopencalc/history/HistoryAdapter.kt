package com.example.myopencalc.history

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myopencalc.R

class HistoryAdapter(
    private var history: MutableList<History>,
    private val onElementClick: (value: String) -> Unit,
    private val context: Context
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    fun appendOneHistoryElement(history: History) {
        this.history.add(history)
        // Update the last 2 elements to avoid to have the same date and bar separator
        if (this.history.size > 1) {
            notifyItemInserted(this.history.size - 1)
            notifyItemRangeChanged(this.history.size - 2, 2)
        } else {
            notifyItemInserted(this.history.size - 1)
        }
    }

    /**
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     * @see .getItemViewType
     * @see .onBindViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        TODO("Not yet implemented")
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    /**

     * @param holder   The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    fun removeFirstHistoryElement() {
        this.history.removeAt(0)
        notifyItemRemoved(0)
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val calculation: TextView = itemView.findViewById(R.id.history_item_calculation)
        private val result: TextView = itemView.findViewById(R.id.history_item_result)
        private val time: TextView = itemView.findViewById(R.id.history_time)
        private val separator: View = itemView.findViewById(R.id.history_separator)
        private val sameDateSeparator: View = itemView.findViewById(R.id.history_same_date_separator)


    }
}