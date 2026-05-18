package com.example.recettecuisine

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup


class HomeActivity : AppCompatActivity() {

    // ── Vues principales ──────────────────────────────────────────────────────
    private lateinit var recyclerView: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnAdd: MaterialButton
    private lateinit var btnCalendar: View
    private lateinit var btnNotifications: View

    /** Indicateur visuel (point rouge) affiché quand des notifications existent */
    private lateinit var notifDot: View

    private lateinit var tvGreeting: TextView

    /** Affiche le nombre de recettes correspondant aux filtres actifs */
    private lateinit var tvRecipeCount: TextView

    private lateinit var chipGroup: ChipGroup

    // ── Données et adaptateur ─────────────────────────────────────────────────

    /** Adaptateur du RecyclerView gérant l'affichage et les actions sur chaque recette */
    private lateinit var adapter: RecipeAdapter

    /** Accès à la base de données locale */
    private lateinit var db: DatabaseHelper

    /** Liste complète des recettes chargées depuis la base (avant filtrage) */
    private var allRecipes = mutableListOf<RecipeModel>()

    /** Catégorie actuellement sélectionnée dans le groupe de chips (par défaut : "Tout") */
    private var currentCategory = "Tout"

    /**
     * Initialise l'interface, configure l'adaptateur avec ses trois callbacks
     * (clic, édition, suppression), puis charge les recettes et configure
     * la recherche et le filtrage par catégorie.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Liaison des vues
        recyclerView     = findViewById(R.id.recyclerView)
        etSearch         = findViewById(R.id.etSearch)
        btnAdd           = findViewById(R.id.btnAdd)
        btnCalendar      = findViewById(R.id.btnCalendar)
        btnNotifications = findViewById(R.id.btnNotifications)
        notifDot         = findViewById(R.id.notifDot)
        tvGreeting       = findViewById(R.id.tvGreeting)
        tvRecipeCount    = findViewById(R.id.tvRecipeCount)
        chipGroup        = findViewById(R.id.chipGroup)

        db = DatabaseHelper(this)
        NotificationHelper.createNotificationChannel(this)

        // Message de bienvenue : nom de l'utilisateur si fourni, sinon date du jour
        val userName = intent.getStringExtra("userName") ?: ""
        tvGreeting.text = if (userName.isNotEmpty()) "Bonjour $userName" else getCurrentDate()

        // ── Configuration de l'adaptateur ────────────────────────────────────────
        adapter = RecipeAdapter(
            allRecipes,

            // Clic sur une recette : ouvre le détail
            onItemClick = { recipe ->
                val intent = Intent(this, RecipeDetailActivity::class.java)
                intent.putExtra("recipeId", recipe.id)
                startActivity(intent)
            },

            // Clic sur "Modifier" : ouvre l'éditeur avec l'ID de la recette
            onEditClick = { recipe ->
                val intent = Intent(this, EditRecipeActivity::class.java)
                intent.putExtra("recipeId", recipe.id)
                startActivity(intent)
            },

            // Clic sur "Supprimer" : affiche une boîte de confirmation avant suppression
            onDeleteClick = { recipe ->
                AlertDialog.Builder(this)
                    .setTitle("Supprimer la recette")
                    .setMessage("Voulez-vous vraiment supprimer \"${recipe.title}\" ?")
                    .setPositiveButton("Oui") { dialog, _ ->
                        val deleted = db.deleteRecipe(recipe.id)
                        if (deleted) {
                            Toast.makeText(this, "Recette supprimée", Toast.LENGTH_SHORT).show()

                            // Enregistrement et envoi d'une notification de suppression
                            db.addNotification(
                                title   = "Recette supprimée",
                                message = "La recette \"${recipe.title}\" a été supprimée !"
                            )
                            NotificationHelper.sendNotification(
                                this,
                                "Recette supprimée",
                                "La recette \"${recipe.title}\" a été supprimée !",
                                recipe.id
                            )

                            // Rechargement de la liste et mise à jour du point de notification
                            loadRecipes()
                            updateNotificationDot()
                        } else {
                            Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show()
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Non") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        )

        // Bouton carte : ouvre l'activité de géolocalisation
        findViewById<MaterialButton>(R.id.btnMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        // Initialisation du RecyclerView avec un layout vertical
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Chargement initial des données et mise en place des filtres
        loadRecipes()
        updateNotificationDot()
        setupChipFilter()
        setupSearch()

        // Navigation vers les autres écrans
        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddRecipeActivity::class.java))
        }
        btnCalendar.setOnClickListener {
            startActivity(Intent(this, PlanningActivity::class.java))
        }
        btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
    }


    override fun onResume() {
        super.onResume()
        loadRecipes()
        updateNotificationDot()
    }

    /**
     * Recharge toutes les recettes depuis la base de données,
     * puis applique les filtres actifs (recherche + catégorie).
     */
    private fun loadRecipes() {
        allRecipes.clear()
        allRecipes.addAll(db.getAllRecipes())
        applyFilters()
    }

    /**
     * Attache un [TextWatcher] sur le champ de recherche.
     * À chaque modification du texte, les filtres sont ré-appliqués
     * pour mettre à jour la liste en temps réel.
     */
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    /**
     * Configure le listener du groupe de chips pour mettre à jour [currentCategory]
     * selon le chip sélectionné, puis ré-applique les filtres.
     * Si aucun chip n'est sélectionné, retombe sur "Tout".
     */
    private fun setupChipFilter() {
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            currentCategory = when {
                checkedIds.contains(R.id.chipAll)      -> "Tout"
                checkedIds.contains(R.id.chipEntrees)  -> "Entrées"
                checkedIds.contains(R.id.chipPlats)    -> "Plats"
                checkedIds.contains(R.id.chipDesserts) -> "Desserts"
                else -> "Tout"
            }
            applyFilters()
        }
    }

    private fun applyFilters() {
        val query = etSearch.text.toString().trim()

        val filtered = allRecipes.filter { recipe ->
            // Vérifie si la recette correspond au texte de recherche
            val matchesSearch = query.isEmpty() ||
                    recipe.title.contains(query, ignoreCase = true) ||
                    recipe.description.contains(query, ignoreCase = true)

            // Vérifie si la recette appartient à la catégorie active
            val matchesCategory = currentCategory == "Tout" ||
                    recipe.category.equals(currentCategory, ignoreCase = true)

            matchesSearch && matchesCategory
        }

        // Mise à jour de l'adaptateur avec la liste filtrée
        adapter.updateList(filtered)

        // Mise à jour du compteur avec accord grammatical sur "recette(s)"
        val count = filtered.size
        tvRecipeCount.text = "$count recette${if (count > 1) "s" else ""}"
    }


    private fun updateNotificationDot() {
        val count = db.getAllNotifications().size
        notifDot.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    /**
     * Retourne la date du jour formatée en français (ex : "Lundi 7 janvier").
     * Utilisée comme message d'accueil quand aucun nom d'utilisateur n'est fourni.
     *
     * @return Date du jour avec la première lettre en majuscule
     */
    private fun getCurrentDate(): String {
        val sdf = java.text.SimpleDateFormat("EEEE d MMMM", java.util.Locale.FRENCH)
        return sdf.format(java.util.Date()).replaceFirstChar { it.uppercase() }
    }
}