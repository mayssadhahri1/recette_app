package com.example.recettecuisine

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WeekActivity : AppCompatActivity() {

    private lateinit var rvWeek: RecyclerView
    private lateinit var weekAdapter: WeekAdapter
    private lateinit var db: DatabaseHelper

    private val weekDays = mutableListOf(
        WeekDay("Lundi"),
        WeekDay("Mardi"),
        WeekDay("Mercredi"),
        WeekDay("Jeudi"),
        WeekDay("Vendredi"),
        WeekDay("Samedi"),
        WeekDay("Dimanche")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_week)

        db = DatabaseHelper(this)
        rvWeek = findViewById(R.id.rvWeek)

        loadWeekFromDatabase()

        weekAdapter = WeekAdapter(
            this,
            weekDays,
            onAddClick    = { day -> showAddDishDialog(day) },
            onRemoveClick = { day, dish -> confirmRemoveDish(day, dish) }
        )

        rvWeek.layoutManager = LinearLayoutManager(this)
        rvWeek.adapter = weekAdapter

        // Buttons
        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<android.widget.Button>(R.id.btnResetWeek).setOnClickListener {
            confirmResetWeek()
        }

        updateStats()
        buildDayDots()
    }

    // ── Add dish flow ─────────────────────────────────────────────────────

    private fun showAddDishDialog(day: WeekDay) {
        val options = arrayOf("📋  Recette récente", "✏️  Plat personnalisé")
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Ajouter à ${day.dayName}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRecentRecipes(day)
                    1 -> addDishManually(day)
                }
            }
            .show()
    }

    private fun showRecentRecipes(day: WeekDay) {
        val recipes = db.getAllRecipes().map { it.title }
        if (recipes.isEmpty()) {
            Toast.makeText(this, "Aucune recette disponible", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Choisir une recette")
            .setItems(recipes.toTypedArray()) { _, which ->
                addDishToDay(day, recipes[which])
            }
            .show()
    }

    private fun addDishManually(day: WeekDay) {
        val input = EditText(this).apply {
            hint = "Nom du plat"
            setHintTextColor(0xFF5B7FA6.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF23263A.toInt())
            setPadding(32, 24, 32, 24)
        }
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Plat personnalisé")
            .setView(input)
            .setPositiveButton("Ajouter") { _, _ ->
                val dish = input.text.toString().trim()
                if (dish.isNotEmpty()) addDishToDay(day, dish)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun addDishToDay(day: WeekDay, dish: String) {
        day.dishes.add(dish)
        weekAdapter.notifyDataSetChanged()
        updateStats()
        buildDayDots()
        autoSave()
        Toast.makeText(this, "\"$dish\" ajouté à ${day.dayName} ✅", Toast.LENGTH_SHORT).show()
    }

    // ── Remove ────────────────────────────────────────────────────────────

    private fun confirmRemoveDish(day: WeekDay, dish: String) {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("Supprimer ce plat ?")
            .setMessage("\"$dish\" sera retiré de ${day.dayName}.")
            .setPositiveButton("Supprimer") { _, _ ->
                db.removeDishFromDay(day.dayName, dish)
                day.dishes.remove(dish)
                weekAdapter.notifyDataSetChanged()
                updateStats()
                buildDayDots()
                autoSave()
                Toast.makeText(this, "Plat supprimé", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // ── Reset ─────────────────────────────────────────────────────────────

    private fun confirmResetWeek() {
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("⚠️ Réinitialiser la semaine ?")
            .setMessage("Tous les plats planifiés seront supprimés.")
            .setPositiveButton("Réinitialiser") { _, _ ->
                db.clearWeekDishes()
                weekDays.forEach { it.dishes.clear() }
                weekAdapter.notifyDataSetChanged()
                updateStats()
                buildDayDots()
                Toast.makeText(this, "Planning réinitialisé", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // ── Share ─────────────────────────────────────────────────────────────

    private fun shareWeekPlan() {
        val sb = StringBuilder()
        sb.appendLine("🍽️ Mon Planning de la semaine — KOUJINI")
        sb.appendLine()
        weekDays.forEach { day ->
            sb.appendLine("📅 ${day.dayName}")
            if (day.dishes.isEmpty()) {
                sb.appendLine("   • Vide")
            } else {
                day.dishes.forEach { sb.appendLine("   • $it") }
            }
            sb.appendLine()
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        startActivity(Intent.createChooser(intent, "Partager le planning"))
    }

    // ── Stats & Day dots ──────────────────────────────────────────────────

    private fun updateStats() {
        val totalDishes = weekDays.sumOf { it.dishes.size }
        val activeDays  = weekDays.count { it.dishes.isNotEmpty() }
        val percent     = ((activeDays / 7.0) * 100).toInt()

        // Subtitle
        val subtitle = if (totalDishes == 0) "Aucun plat planifié"
        else "$totalDishes plat${if (totalDishes > 1) "s" else ""} · $activeDays jour${if (activeDays > 1) "s" else ""} actif${if (activeDays > 1) "s" else ""}"
        findViewById<TextView>(R.id.tvWeekSubtitle).text = subtitle

        // Stat cards
        findViewById<TextView>(R.id.tvStatTotal).text   = totalDishes.toString()
        findViewById<TextView>(R.id.tvStatDays).text    = "$activeDays/7"
        findViewById<TextView>(R.id.tvStatPercent).text = "$percent%"
    }

    private fun buildDayDots() {
        val container = findViewById<LinearLayout>(R.id.layoutDayDots)
        container.removeAllViews()

        val dayInitials = listOf("L", "M", "M", "J", "V", "S", "D")

        weekDays.forEachIndexed { index, day ->
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Dot
            val dot = View(this).apply {
                val size = resources.getDimensionPixelSize(R.dimen.day_dot_size) // 10dp fallback
                layoutParams = LinearLayout.LayoutParams(size, size).also {
                    it.gravity = Gravity.CENTER_HORIZONTAL
                    it.bottomMargin = 4
                }
                val color = if (day.dishes.isNotEmpty()) 0xFF2563EB.toInt() else 0xFF2A2D45.toInt()
                backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                setBackgroundResource(android.R.drawable.presence_online) // circle
            }

            // Label
            val label = TextView(this).apply {
                text = dayInitials.getOrElse(index) { "?" }
                textSize = 9f
                setTextColor(
                    if (day.dishes.isNotEmpty()) 0xFF2563EB.toInt() else 0xFF5B7FA6.toInt()
                )
                gravity = Gravity.CENTER
            }

            col.addView(dot)
            col.addView(label)
            container.addView(col)
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────

    private fun autoSave() {
        db.clearWeekDishes()
        weekDays.forEach { day ->
            day.dishes.forEach { dish ->
                db.addDishToDay(day.dayName, dish)
            }
        }
    }

    private fun loadWeekFromDatabase() {
        weekDays.forEach { day ->
            day.dishes.clear()
            day.dishes.addAll(db.getDishesForDay(day.dayName))
        }
    }

    override fun onResume() {
        super.onResume()
        loadWeekFromDatabase()
        if (::weekAdapter.isInitialized) {
            weekAdapter.notifyDataSetChanged()
            updateStats()
            buildDayDots()
        }
    }
}