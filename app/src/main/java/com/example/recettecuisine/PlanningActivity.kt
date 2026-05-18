package com.example.recettecuisine

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class PlanningActivity : AppCompatActivity() {

    /** RecyclerView affichant la liste des jours de la semaine */
    private lateinit var recyclerWeek: RecyclerView

    /** Adaptateur gérant l'affichage et les interactions pour chaque jour */
    private lateinit var weekAdapter: WeekAdapter

    /** Accès à la base de données locale */
    private lateinit var db: DatabaseHelper

    /**
     * Liste des sept jours de la semaine avec leurs plats associés.
     * Initialisée avec des jours vides, puis alimentée depuis la base au chargement.
     */
    private val weekDays = mutableListOf(
        WeekDay("Lundi"),
        WeekDay("Mardi"),
        WeekDay("Mercredi"),
        WeekDay("Jeudi"),
        WeekDay("Vendredi"),
        WeekDay("Samedi"),
        WeekDay("Dimanche")
    )

    /**
     * Initialise l'interface, charge les plats depuis la base,
     * configure l'adaptateur avec ses deux callbacks (ajout / suppression),
     * puis attache l'adaptateur au RecyclerView.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planning)

        db = DatabaseHelper(this)
        recyclerWeek = findViewById(R.id.recyclerWeek)

        // Chargement des plats déjà planifiés depuis la base de données
        loadWeekFromDatabase()

        // Initialisation de l'adaptateur avec les callbacks d'ajout et de suppression
        weekAdapter = WeekAdapter(
            context  = this,
            weekDays = weekDays,

            // Ouvre la boîte de dialogue de choix du type de plat à ajouter
            onAddClick = { day -> showAddDishDialog(day) },

            // Supprime le plat en base et dans la liste locale, puis rafraîchit l'affichage
            onRemoveClick = { day, dish ->
                db.removeDishFromDay(day.dayName, dish)
                day.dishes.remove(dish)
                weekAdapter.notifyDataSetChanged()
                Toast.makeText(this, "$dish supprimé", Toast.LENGTH_SHORT).show()
            }
        )

        recyclerWeek.layoutManager = LinearLayoutManager(this)
        recyclerWeek.adapter = weekAdapter
    }

    /**
     * Affiche une boîte de dialogue proposant deux modes d'ajout d'un plat :
     * - "Recette récente" : sélection parmi les recettes enregistrées en base
     * - "Plat sans recette" : saisie manuelle libre du nom du plat
     *
     * @param day Jour auquel le plat sera ajouté
     */
    private fun showAddDishDialog(day: WeekDay) {
        val options = arrayOf("Recette récente", "Plat sans recette")
        AlertDialog.Builder(this)
            .setTitle("Ajouter un plat à ${day.dayName}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRecentRecipes(day)  // Choix depuis les recettes enregistrées
                    1 -> addDishManually(day)    // Saisie libre du nom du plat
                }
            }
            .show()
    }

    /**
     * Récupère la liste des titres de toutes les recettes enregistrées
     * et les affiche dans une boîte de dialogue de sélection.
     * Si aucune recette n'existe, un message d'avertissement est affiché.
     *
     * @param day Jour auquel la recette sélectionnée sera ajoutée
     */
    private fun showRecentRecipes(day: WeekDay) {
        val recipes = db.getAllRecipes().map { it.title }

        // Aucune recette en base : information de l'utilisateur et abandon
        if (recipes.isEmpty()) {
            Toast.makeText(this, "Aucune recette enregistrée", Toast.LENGTH_SHORT).show()
            return
        }

        // Affichage de la liste des recettes disponibles à sélectionner
        AlertDialog.Builder(this)
            .setTitle("Choisir une recette")
            .setItems(recipes.toTypedArray()) { _, which ->
                val selectedDish = recipes[which]
                saveDishToDay(day, selectedDish) // Sauvegarde la recette choisie
            }
            .show()
    }

    /**
     * Affiche une boîte de dialogue avec un champ de saisie libre
     * permettant d'entrer le nom d'un plat sans recette associée.
     * Le plat n'est enregistré que si le champ n'est pas vide.
     *
     * @param day Jour auquel le plat saisi sera ajouté
     */
    private fun addDishManually(day: WeekDay) {
        val input = EditText(this)
        input.hint = "Ex: Pâtes Carbonara"

        AlertDialog.Builder(this)
            .setTitle("Saisie manuelle")
            .setView(input)
            .setPositiveButton("Ajouter") { _, _ ->
                val dish = input.text.toString().trim()
                // Enregistrement uniquement si le champ n'est pas vide
                if (dish.isNotEmpty()) {
                    saveDishToDay(day, dish)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Ajoute un plat à un jour donné : met à jour la liste locale,
     * persiste en base de données et rafraîchit l'affichage du RecyclerView.
     *
     * @param day  Jour auquel le plat est ajouté
     * @param dish Nom du plat à enregistrer
     */
    private fun saveDishToDay(day: WeekDay, dish: String) {
        day.dishes.add(dish)              // Ajout dans la liste en mémoire
        db.addDishToDay(day.dayName, dish) // Persistance en base de données
        weekAdapter.notifyDataSetChanged() // Mise à jour de l'affichage
    }

    /**
     * Charge depuis la base de données les plats planifiés pour chaque jour.
     * Réinitialise la liste de chaque jour avant de la remplir pour éviter
     * les doublons en cas de rechargement.
     */
    private fun loadWeekFromDatabase() {
        weekDays.forEach { day ->
            day.dishes.clear()                               // Réinitialisation pour éviter les doublons
            day.dishes.addAll(db.getDishesForDay(day.dayName)) // Chargement depuis la base
        }
    }
}