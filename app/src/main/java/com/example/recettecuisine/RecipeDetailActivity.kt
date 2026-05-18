package com.example.recettecuisine

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        db = DatabaseHelper(this)

        val imgRecipe      = findViewById<ImageView>(R.id.imgRecipe)
        val tvTitle        = findViewById<TextView>(R.id.tvTitle)
        val tvCategory     = findViewById<TextView>(R.id.tvCategory)
        val tvDescription  = findViewById<TextView>(R.id.tvDescription)
        val tvIngredients  = findViewById<TextView>(R.id.tvIngredients)
        val tvTips         = findViewById<TextView>(R.id.tvTips)
        val tvTime         = findViewById<TextView>(R.id.tvTime)
        val tvPortion      = findViewById<TextView>(R.id.tvPortion)
        val tvDifficulty   = findViewById<TextView>(R.id.tvDifficulty)
        val stepsContainer = findViewById<LinearLayout>(R.id.stepsContainer)

        val recipeId = intent.getIntExtra("recipeId", -1)
        if (recipeId == -1) { finish(); return }

        val recipe = db.getRecipeById(recipeId)
        if (recipe == null) { finish(); return }

        // ── Données texte ────────────────────────────────────────────────────
        tvTitle.text       = recipe.title
        tvDescription.text = recipe.description.ifEmpty { "Aucune description." }
        tvTips.text        = recipe.tips.ifEmpty { "Aucune astuce pour cette recette." }
        tvTime.text        = recipe.time.ifEmpty { "N/A" }
        tvPortion.text     = recipe.portion.ifEmpty { "N/A" }
        tvDifficulty.text  = "Facile"

        // Catégorie
        if (recipe.category.isNotEmpty()) tvCategory.text = recipe.category

        // Ingrédients formatés
        val ingredientLines = recipe.ingredients
            .split("\n")
            .filter { it.isNotBlank() }
            .joinToString("\n") { line ->
                if (line.trimStart().startsWith("•")) line else "• ${line.trim()}"
            }
        tvIngredients.text = ingredientLines.ifEmpty { "Aucun ingrédient renseigné." }

        // ── Étapes ──────────────────────────────────────────────────────────
        val stepsList = recipe.steps
            .split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (stepsList.isEmpty()) {
            val tv = TextView(this).apply {
                text = "Aucune étape renseignée."
                setTextColor(Color.parseColor("#8A9BB0"))
                textSize = 14f
            }
            stepsContainer.addView(tv)
        } else {
            stepsList.forEachIndexed { index, step ->
                stepsContainer.addView(buildStepView(index + 1, step))
            }
        }

        // ── Image ────────────────────────────────────────────────────────────
        if (recipe.image.isNotEmpty()) {
            try {
                imgRecipe.setImageURI(Uri.parse(recipe.image))
            } catch (e: Exception) {
                imgRecipe.setBackgroundColor(Color.parseColor("#D8DEE9"))
            }
        }

        // ── Bouton retour ────────────────────────────────────────────────────
        // Back button - tap the whole container or the icon
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }

    // ── Construit une ligne d'étape propre ───────────────────────────────────
    private fun buildStepView(number: Int, text: String): LinearLayout {
        val dp8  = dp(8)
        val dp12 = dp(12)
        val dp14 = dp(14)
        val dp16 = dp(16)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.TOP
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp12
            layoutParams = lp
        }

        // Badge numéro cercle bleu
        val badge = TextView(this).apply {
            this.text = "$number"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            val size = dp(32)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.marginEnd = dp12
            lp.topMargin = dp(2)
            layoutParams = lp
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#185FA5"))
            }
            background = shape
        }

        // Card texte de l'étape
        val card = CardView(this).apply {
            radius = dp(12).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
        }

        // Ligne colorée à gauche dans la card
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val accentBar = android.view.View(this).apply {
            val lp = LinearLayout.LayoutParams(dp(3), LinearLayout.LayoutParams.MATCH_PARENT)
            layoutParams = lp
            setBackgroundColor(Color.parseColor("#B5D4F4"))
        }

        val tvStep = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#1A2D45"))
            textSize = 14f
            setPadding(dp12, dp12, dp12, dp12)
            setLineSpacing(0f, 1.5f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        inner.addView(accentBar)
        inner.addView(tvStep)
        card.addView(inner)
        row.addView(badge)
        row.addView(card)

        return row
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
        ).toInt()
}