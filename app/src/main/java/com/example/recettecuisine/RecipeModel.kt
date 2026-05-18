package com.example.recettecuisine

data class RecipeModel(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val portion: String = "",
    val time: String = "",
    val ingredients: String = "",
    val steps: String = "",
    val tips: String = "",
    val image: String = "",
    val category: String = "Plats"
)