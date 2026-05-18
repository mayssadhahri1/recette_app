package com.example.recettecuisine

data class NotificationModel(
    val id: Int,
    val title: String,
    val message: String,
    var time: String,
    var isRead: Boolean = false
)