package com.example.recettecuisine

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

// Adapteur pour afficher la liste des notifications dans un RecyclerView
class NotificationAdapter(
    private var notifications: MutableList<NotificationModel>, // La liste des notifications à afficher
    private val db: DatabaseHelper // La base de données pour sauvegarder les changements
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    // ViewHolder : représente une seule carte de notification à l'écran
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view as MaterialCardView          // La carte entière
        val tvTitle: TextView = view.findViewById(R.id.tvNotificationTitle)   // Le titre de la notification
        val tvMessage: TextView = view.findViewById(R.id.tvNotificationMessage) // Le message de la notification
        val tvTime: TextView = view.findViewById(R.id.tvNotificationTime)     // L'heure d'envoi
        val btnDelete: Button = view.findViewById(R.id.btnDeleteNotification) // Le bouton pour supprimer
    }

    // Création d'une nouvelle carte quand le RecyclerView en a besoin
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false) as MaterialCardView
        return ViewHolder(view)
    }

    // Retourne le nombre total de notifications dans la liste
    override fun getItemCount(): Int = notifications.size

    // Remplissage d'une carte avec les données d'une notification
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]

        // Jouer une animation d'apparition en fondu pour la carte
        holder.cardView.startAnimation(
            AnimationUtils.loadAnimation(holder.itemView.context, android.R.anim.fade_in)
        )

        // Afficher les informations de la notification
        holder.tvTitle.text = notification.title
        holder.tvMessage.text = notification.message
        holder.tvTime.text = formatRelativeTime(notification.time)

        // Si la notification n'est pas encore lue → on lui donne un fond coloré pour la distinguer
        if (!notification.isRead) {
            try {
                // Fond violet clair + titre en bleu pour indiquer "non lu"
                holder.cardView.setCardBackgroundColor(
                    holder.itemView.context.getColor(R.color.notification_unread)
                )
                holder.tvTitle.setTextColor(holder.itemView.context.getColor(R.color.primary))
            } catch (e: Exception) {
                // Si les couleurs du fichier ressources n'existent pas, on utilise des couleurs fixes
                holder.cardView.setCardBackgroundColor(0xFFF3E5F5.toInt())
                holder.tvTitle.setTextColor(0xFF2196F3.toInt())
            }
        } else {
            // Notification déjà lue → fond blanc simple
            holder.cardView.setCardBackgroundColor(android.graphics.Color.WHITE)
            holder.tvTitle.setTextColor(0xFF333333.toInt())
        }

        // Quand l'utilisateur clique sur "Supprimer" → lancer la suppression avec animation
        holder.btnDelete.setOnClickListener {
            deleteWithAnimation(holder.adapterPosition)
        }

        // Quand l'utilisateur clique sur la carte → marquer la notification comme lue
        holder.cardView.setOnClickListener {
            markAsRead(holder.adapterPosition)
        }
    }

    // Supprime une notification de la liste et de la base de données avec une animation
    private fun deleteWithAnimation(position: Int) {
        // Vérifier que la position est valide avant de continuer
        if (position != RecyclerView.NO_POSITION && position < notifications.size) {
            val notification = notifications[position]

            // Essayer de supprimer la notification dans la base de données
            val deleted = db.deleteNotification(notification.id)

            if (deleted) {
                // Supprimer aussi de la liste locale et animer la disparition
                notifications.removeAt(position)
                notifyItemRemoved(position)
                // Mettre à jour les positions des cartes restantes
                notifyItemRangeChanged(position, notifications.size - position)
            }
        }
    }

    // Marque une notification comme "lue" si elle ne l'était pas encore
    private fun markAsRead(position: Int) {
        // Vérifier que la position est valide
        if (position != RecyclerView.NO_POSITION && position < notifications.size) {
            val notification = notifications[position]
            if (!notification.isRead) {
                notification.isRead = true              // Mettre à jour l'objet en mémoire
                db.markNotificationAsRead(notification.id) // Sauvegarder dans la base de données
                notifyItemChanged(position)             // Rafraîchir l'affichage de cette carte
            }
        }
    }

    // Convertit une date en texte lisible : "À l'instant", "5 min", "2 h", ou "12/05/2025"
    private fun formatRelativeTime(timeString: String): String {
        return try {
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val notificationTime = dateFormat.parse(timeString) ?: return timeString
            val now = java.util.Date()
            val diff = now.time - notificationTime.time // Différence en millisecondes

            when {
                diff < 60000     -> "À l'instant"       // Moins d'une minute
                diff < 3600000   -> "${diff / 60000} min" // Moins d'une heure → afficher en minutes
                diff < 86400000  -> "${diff / 3600000} h" // Moins d'un jour → afficher en heures
                else -> java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    .format(notificationTime)            // Plus vieux → afficher la date complète
            }
        } catch (e: Exception) {
            timeString // En cas d'erreur, retourner le texte original sans modification
        }
    }

    // Met à jour toute la liste avec de nouvelles notifications et rafraîchit l'affichage
    fun updateList(newList: List<NotificationModel>) {
        notifications = newList.toMutableList()
        notifyDataSetChanged() // Redessiner toutes les cartes
    }
}