package com.example.recettecuisine

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecipeAdapter(
    private var list: List<RecipeModel>,
    private val onItemClick: (RecipeModel) -> Unit,
    private val onEditClick: (RecipeModel) -> Unit,
    private val onDeleteClick: (RecipeModel) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitleItem)
        val tvDesc: TextView = view.findViewById(R.id.tvDescriptionItem)
        val img: ImageView = view.findViewById(R.id.imgRecipeItem)
        val btnEdit: Button = view.findViewById(R.id.btnEditItem)      // ⚠ correspond à item_recipe.xml
        val btnDelete: Button = view.findViewById(R.id.btnDeleteItem)  // ⚠ correspond à item_recipe.xml
        val tvTime: TextView = view.findViewById(R.id.tvTimeItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = list[position]

        holder.tvTitle.text = recipe.title
        holder.tvDesc.text = recipe.description
        holder.tvTime.text = "⏱ ${recipe.time}"

        // Image
        if (recipe.image.isNotEmpty()) {
            try {
                holder.img.setImageURI(Uri.parse(recipe.image))
            } catch (e: Exception) {
                holder.img.setImageResource(android.R.color.darker_gray)
            }
        } else {
            holder.img.setImageResource(android.R.color.darker_gray)
        }

        // Clic sur la carte → détail
        holder.itemView.setOnClickListener { onItemClick(recipe) }

        // Clic sur Modifier
        holder.btnEdit.setOnClickListener { onEditClick(recipe) }

        // Clic sur Supprimer
        holder.btnDelete.setOnClickListener { onDeleteClick(recipe) }
    }

    fun updateList(newList: List<RecipeModel>) {
        list = newList
        notifyDataSetChanged()
    }
}