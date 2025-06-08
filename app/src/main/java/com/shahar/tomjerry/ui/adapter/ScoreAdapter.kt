package com.shahar.tomjerry.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.shahar.tomjerry.R
import com.shahar.tomjerry.data.Score
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScoreAdapter(private val onItemClicked: (Score) -> Unit) : RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder>() {

    private var scores: List<Score> = emptyList()

    // The ViewHolder is a wrapper around a View that contains the layout for an individual
    // item in the list. It holds direct references to the UI components.
    class ScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val scoreTextView: MaterialTextView = itemView.findViewById(R.id.score_text_view)
        val dateTextView: MaterialTextView = itemView.findViewById(R.id.date_text_view)
    }

    // Called by RecyclerView when it needs a new ViewHolder of the given type to represent
    // an item. We inflate the XML layout for our row here.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_score, parent, false)
        return ScoreViewHolder(itemView)
    }

    // Called by RecyclerView to display the data at the specified position.
    // This method updates the contents of the ViewHolder to reflect the item at the
    // given position.
    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val currentScore = scores[position]

        holder.scoreTextView.text = "Score: ${currentScore.score}"

        // Format the timestamp into a readable date string
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateString = sdf.format(Date(currentScore.timestamp))
        holder.dateTextView.text = dateString

        holder.itemView.setOnClickListener {
            // When clicked, invoke the lambda function passed into the constructor.
            onItemClicked(currentScore)
        }

    }

    // Returns the total number of items in the data set held by the adapter.
    override fun getItemCount() = scores.size

    // A helper function to update the data in the adapter and refresh the list.
    fun submitList(newScores: List<Score>) {
        scores = newScores
        // notifyDataSetChanged() tells the RecyclerView that the data has changed and
        // it needs to redraw the entire list.
        notifyDataSetChanged()
    }
}
