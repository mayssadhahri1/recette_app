# 🍲 Recettes de Cuisine — Application Android (Kotlin)

> Application mobile native pour découvrir des recettes, gérer les siennes et planifier les repas de la semaine.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white)
![Retrofit](https://img.shields.io/badge/Retrofit-REST-48B983)
![SQLite](https://img.shields.io/badge/SQLite-003B57?logo=sqlite&logoColor=white)
![Maps](https://img.shields.io/badge/Google%20Maps-4285F4?logo=googlemaps&logoColor=white)

## ✨ Fonctionnalités

- 🔐 **Authentification** des utilisateurs
- 🔎 **Découverte de recettes** via l'API publique **TheMealDB** (Retrofit + Gson)
- ➕ **Ajout, modification et détail** de ses propres recettes, classées par catégorie
- 🗓️ **Planning de la semaine** : choisir les plats de chaque jour
- 🔔 **Notifications** de rappel
- 🗺️ **Carte** avec géolocalisation (Google Maps / OSMDroid)

## 🏗️ Architecture

| Élément | Rôle |
|---|---|
| `AuthActivity`, `HomeActivity`, `RecipeDetailActivity`, `PlanningActivity`, `MapActivity`… | Écrans |
| `ApiService`, `RetrofitClient` | Appels à l'API REST |
| `DatabaseHelper` | Base **SQLite** locale (`users`, `recipes`, `week_dishes`, `notifications`) |
| `RecipeAdapter`, `WeekAdapter`, `NotificationAdapter` | Listes RecyclerView |
| `NotificationHelper` | Création des notifications Android |

## 🚀 Lancer le projet

1. Ouvrir le projet dans **Android Studio**
2. Ajouter une clé Google Maps si besoin (`AndroidManifest.xml`)
3. Lancer sur un émulateur ou un téléphone

## 🛠️ Stack

Kotlin · Android SDK · Retrofit · Gson · OkHttp · SQLite · Glide · Google Maps / Location · OSMDroid · Material Design

---

👩‍💻 **Mayssa Dhahri** — Élève-ingénieure en Génie Logiciel · [LinkedIn](https://www.linkedin.com/in/mayssa-dhahri)
