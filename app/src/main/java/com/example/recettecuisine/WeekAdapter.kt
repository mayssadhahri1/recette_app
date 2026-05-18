package com.example.recettecuisine

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WeekAdapter(
    private val context: Context,
    private val weekDays: MutableList<WeekDay>,
    private val onAddClick: (WeekDay) -> Unit,
    private val onRemoveClick: (WeekDay, String) -> Unit
) : RecyclerView.Adapter<WeekAdapter.WeekViewHolder>() {

    private val dayEmojis = listOf("🌅", "☀️", "🌤️", "⭐", "🎉", "🌿", "🌙")

    inner class WeekViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDayEmoji: TextView = view.findViewById(R.id.tvDayEmoji)
        val tvDay: TextView = view.findViewById(R.id.tvDay)
        val tvDishCount: TextView = view.findViewById(R.id.tvDishCount)
        val viewDayDot: View = view.findViewById(R.id.viewDayDot)
        val btnAddDish: LinearLayout = view.findViewById(R.id.btnAddDish)
        val llDishes: LinearLayout = view.findViewById(R.id.llDishes)
        val layoutEmpty: LinearLayout = view.findViewById(R.id.layoutEmpty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_week_day, parent, false)
        return WeekViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
        val day = weekDays[position]

        holder.tvDayEmoji.text = dayEmojis.getOrElse(position) { "🍽️" }
        holder.tvDay.text = day.dayName

        val count = day.dishes.size
        holder.tvDishCount.text = when (count) {
            0    -> "Vide"
            1    -> "1 plat"
            else -> "$count plats"
        }

        // Couleur du point indicateur
        val dotColor = if (count > 0) 0xFF22C55E.toInt() else 0xFF2A2D45.toInt()
        holder.viewDayDot.backgroundTintList = ColorStateList.valueOf(dotColor)

        holder.layoutEmpty.visibility = if (count == 0) View.VISIBLE else View.GONE

        // Nettoyage et remplissage de la liste des plats (évite les doublons au scroll)
        holder.llDishes.removeAllViews()
        day.dishes.forEach { dish ->
            val row = LayoutInflater.from(context)
                .inflate(R.layout.item_dish_row, holder.llDishes, false)

            row.findViewById<TextView>(R.id.tvDishName).text = dish
            row.findViewById<View>(R.id.btnRemoveDish).setOnClickListener {
                onRemoveClick(day, dish)
            }

            holder.llDishes.addView(row)
        }

        holder.btnAddDish.setOnClickListener { onAddClick(day) }
    }

    override fun getItemCount(): Int = weekDays.size
}