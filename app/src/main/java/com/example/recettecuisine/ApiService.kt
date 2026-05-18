package com.example.recettecuisine

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Représente un plat retourné par l'API TheMealDB.
 * Seul le nom du plat est extrait de la réponse JSON.
 *
 * @property strMeal Nom du plat (correspond au champ "strMeal" dans le JSON)
 */
data class Meal(
    val strMeal: String
)

/**
 * Modèle de la réponse complète renvoyée par l'API lors d'une recherche.
 * L'API encapsule toujours les résultats dans un tableau "meals".
 *
 * @property meals Liste des plats correspondant à la recherche,
 *                 ou null / liste vide si aucun résultat trouvé.
 */
data class MealResponse(
    val meals: List<Meal>
)

interface ApiService {

    /**
     * Recherche des plats par nom via l'endpoint "search.php".
     *
     * Exemple d'URL générée : search.php?s=chicken
     *
     * @param query Mot-clé de recherche saisi par l'utilisateur
     *              (ex : "chicken", "pasta", "beef")
     * @return Un [Call] encapsulant la réponse [MealResponse] avec
     *         la liste des plats correspondants.
     */
    @GET("search.php")
    fun searchMeals(@Query("s") query: String): Call<MealResponse>
}