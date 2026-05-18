package com.example.recettecuisine

data class WeekDay(
    val dayName: String,
    val dishes: MutableList<String> = mutableListOf()
)