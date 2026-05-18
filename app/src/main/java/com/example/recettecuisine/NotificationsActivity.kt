package com.example.recettecuisine

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.*

// Écran qui affiche la liste de toutes les notifications de l'utilisateur
class NotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView       // La liste défilante des notifications
    private lateinit var db: DatabaseHelper               // La base de données locale
    private lateinit var adapter: NotificationAdapter     // L'adapteur qui remplit la liste

    // Appelé automatiquement à l'ouverture de l'écran
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications) // Charger la mise en page de l'écran

        // --- Barre de navigation en haut de l'écran ---
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Afficher la flèche retour

        // Retour à l'écran précédent quand on clique sur la flèche
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // --- Bouton "Tout effacer" ---
        val btnClearAll = findViewById<Button>(R.id.btnClearAll)
        btnClearAll.setOnClickListener {
            showClearAllDialog() // Afficher une confirmation
        }

        setupRecyclerView()
    }

    // Initialise le RecyclerView avec son adapteur et sa base de données
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewNotifications)
        db = DatabaseHelper(this)

        // Démarrer avec une liste vide, elle sera remplie dans onResume()
        adapter = NotificationAdapter(mutableListOf(), db)

        recyclerView.layoutManager = LinearLayoutManager(this) // Affichage en liste verticale
        recyclerView.adapter = adapter
    }

    // Affiche une boîte de dialogue pour confirmer la suppression de toutes les notifications
    private fun showClearAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("Effacer toutes les notifications")
            .setMessage("Cette action est irréversible. Voulez-vous continuer ?")
            .setPositiveButton("Oui") { _, _ ->
                db.clearAllNotifications() // Vider la base de données
                loadNotifications()        // Rafraîchir l'affichage (liste vide)
            }
            .setNegativeButton("Non", null) // Ne rien faire si l'utilisateur annule
            .show()
    }

    // Appelé automatiquement chaque fois que l'utilisateur revient sur cet écran
    override fun onResume() {
        super.onResume()
        loadNotifications() // Recharger les notifications pour avoir les données à jour
    }

    // Charge les notifications depuis la base de données et met à jour l'affichage
    private fun loadNotifications() {
        val notifications = db.getAllNotifications()
            .sortedByDescending { parseTime(it.time) }   // Trier du plus récent au plus ancien
            .map { it.copy(time = formatTime(it.time)) } // Convertir la date en format lisible

        adapter.updateList(notifications) // Envoyer la liste à l'adapteur pour l'afficher

        // Afficher un message "Aucune notification" si la liste est vide
        val layoutEmpty = findViewById<android.widget.LinearLayout>(R.id.layoutEmpty)
        if (notifications.isEmpty()) {
            layoutEmpty.visibility = android.view.View.VISIBLE  // Montrer le message vide
            recyclerView.visibility = android.view.View.GONE    // Cacher la liste
        } else {
            layoutEmpty.visibility = android.view.View.GONE     // Cacher le message vide
            recyclerView.visibility = android.view.View.VISIBLE // Montrer la liste
        }
    }

    // Convertit une date en texte ("2025-05-12 14:30:00") en objet Date utilisable pour le tri
    private fun parseTime(timeString: String): Date {
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(timeString) ?: Date()
        } catch (e: Exception) {
            Date() // En cas d'erreur, retourner la date d'aujourd'hui pour éviter un plantage
        }
    }

    // Convertit une date  ("12/05/2025 14:30")
    private fun formatTime(timeString: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(timeString)
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            timeString // En cas d'erreur, retourner la date originale sans modification
        }
    }
}