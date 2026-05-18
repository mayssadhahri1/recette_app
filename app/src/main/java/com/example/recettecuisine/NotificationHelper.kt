package com.example.recettecuisine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

// Outil global pour gérer l'envoi des notifications dans l'application
object NotificationHelper {

    // Identifiant unique du canal (obligatoire pour Android 8 et plus)
    private const val CHANNEL_ID = "recipe_channel"

    // Nom du canal visible dans les paramètres du téléphone
    private const val CHANNEL_NAME = "Recette Notifications"

    // Description du canal visible dans les paramètres du téléphone
    private const val CHANNEL_DESC = "Notifications pour les recettes"

    // Crée le canal de notification — à appeler une seule fois au démarrage de l'application
    fun createNotificationChannel(context: Context) {

        // Les canaux n'existent que sur Android 8 (Oreo) et versions supérieures
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Création du canal avec son identifiant, son nom et son niveau d'importance
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT // Importance normale : son activé, pas de pop-up
            ).apply {
                description = CHANNEL_DESC // Ajouter la description du canal
            }

            // Récupérer le gestionnaire de notifications du système Android
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Enregistrer le canal auprès du système (sans ça, aucune notification ne s'affiche)
            manager.createNotificationChannel(channel)
        }
    }

    // Envoie une notification visible dans la barre de statut du téléphone
    // → title        : le titre affiché en gras
    // → message      : le texte affiché en dessous du titre
    // → notificationId : un numéro unique pour identifier cette notification
    fun sendNotification(context: Context, title: String, message: String, notificationId: Int) {

        // Construction du contenu et de l'apparence de la notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Icône affichée dans la barre de statut
            .setContentTitle(title)                          // Titre de la notification
            .setContentText(message)                         // Message de la notification
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Priorité normale (ni silencieuse, ni urgente)
            .setAutoCancel(true)                             // Disparaît automatiquement quand l'utilisateur clique dessus

        // Envoyer la notification au système Android pour qu'elle s'affiche
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }
}