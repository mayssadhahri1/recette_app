package com.example.recettecuisine

import android.app.Application
import org.osmdroid.config.Configuration


class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Chargement de la configuration osmdroid depuis les préférences partagées.
        // Les paramètres persistants (dossier de cache, zoom, etc.) y sont stockés.
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )

        // Définition du User-Agent HTTP pour les requêtes vers les serveurs de tuiles.
        // Utiliser le nom du package (ex : com.example.recettecuisine) est une bonne
        // pratique recommandée par OpenStreetMap pour éviter les blocages de requêtes.
        Configuration.getInstance().userAgentValue = packageName
    }
}