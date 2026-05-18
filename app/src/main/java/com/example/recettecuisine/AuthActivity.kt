package com.example.recettecuisine

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText


class AuthActivity : AppCompatActivity() {

    /** Accès à la base de données locale (utilisateurs) */
    private lateinit var db: DatabaseHelper

    /**
     * Initialise l'interface et configure les interactions utilisateur :
     * - Basculement entre les formulaires Connexion / Inscription
     * - Validation et traitement du formulaire de connexion
     * - Validation et traitement du formulaire d'inscription
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        db = DatabaseHelper(this)

        // Récupération des vues principales
        val toggleGroup    = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        val layoutLogin    = findViewById<View>(R.id.layoutLogin)
        val layoutRegister = findViewById<View>(R.id.layoutRegister)

        // Listener sur le groupe d'onglets :
        // affiche le formulaire correspondant à l'onglet sélectionné
        // et masque l'autre formulaire.
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == R.id.btnTabLogin) {
                    // Onglet "Connexion" sélectionné
                    layoutLogin.visibility    = View.VISIBLE
                    layoutRegister.visibility = View.GONE
                } else {
                    // Onglet "Inscription" sélectionné
                    layoutLogin.visibility    = View.GONE
                    layoutRegister.visibility = View.VISIBLE
                }
            }
        }

        // ── Bouton Connexion ──────────────────────────────────────────────────────
        // Vérifie que les champs ne sont pas vides, puis interroge la base de
        // données. En cas de succès, ouvre MainActivity et ferme AuthActivity
        // pour empêcher le retour arrière vers l'écran de connexion.
        findViewById<View>(R.id.btnLogin).setOnClickListener {
            val email = findViewById<TextInputEditText>(R.id.etLoginEmail).text.toString().trim()
            val pass  = findViewById<TextInputEditText>(R.id.etLoginPassword).text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                if (db.checkUser(email, pass)) {
                    // Identifiants valides : navigation vers l'écran principal
                    startActivity(Intent(this, MainActivity::class.java))
                    finish() // Retire AuthActivity de la pile de retour
                } else {
                    // Email ou mot de passe incorrect
                    Toast.makeText(this, "Identifiants incorrects", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Au moins un champ est vide
                Toast.makeText(this, "Champs vides", Toast.LENGTH_SHORT).show()
            }
        }

        // ── Bouton Inscription ────────────────────────────────────────────────────
        // Vérifie que tous les champs sont remplis, puis tente d'insérer
        // le nouvel utilisateur en base. En cas de succès, bascule
        // automatiquement vers l'onglet Connexion pour que l'utilisateur
        // puisse se connecter immédiatement.
        findViewById<View>(R.id.btnRegister).setOnClickListener {
            val name  = findViewById<TextInputEditText>(R.id.etRegisterName).text.toString().trim()
            val email = findViewById<TextInputEditText>(R.id.etRegisterEmail).text.toString().trim()
            val pass  = findViewById<TextInputEditText>(R.id.etRegisterPassword).text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                if (db.addUser(name, email, pass)) {
                    // Inscription réussie : message de confirmation et redirection vers Connexion
                    Toast.makeText(this, "Succès ! Connectez-vous.", Toast.LENGTH_SHORT).show()
                    toggleGroup.check(R.id.btnTabLogin) // Bascule auto vers l'onglet Connexion
                } else {
                    // Échec de l'insertion (ex : email déjà utilisé)
                    Toast.makeText(this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Un ou plusieurs champs d'inscription sont vides
                Toast.makeText(this, "Veuillez remplir tous les champs d'inscription", Toast.LENGTH_SHORT).show()
            }
        }
    }
}