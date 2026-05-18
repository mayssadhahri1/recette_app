package com.example.recettecuisine

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "RecettesDB.db", null, 7) {

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS users (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                name     TEXT NOT NULL,
                email    TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recipes (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                title       TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                portion     TEXT NOT NULL DEFAULT '',
                time        TEXT NOT NULL DEFAULT '',
                ingredients TEXT NOT NULL DEFAULT '',
                steps       TEXT NOT NULL DEFAULT '',
                tips        TEXT NOT NULL DEFAULT '',
                image       TEXT NOT NULL DEFAULT '',
                category    TEXT NOT NULL DEFAULT 'Plats'
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS week_dishes (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                day       TEXT NOT NULL,
                dish_name TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS notifications (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                title   TEXT NOT NULL,
                message TEXT NOT NULL,
                time    TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 7) {
            try {
                db.execSQL("ALTER TABLE recipes ADD COLUMN category TEXT NOT NULL DEFAULT 'Plats'")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // USERS
    // ════════════════════════════════════════════════════════════════════════

    fun addUser(name: String, email: String, password: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put("name", name)
                put("email", email)
                put("password", password)
            }
            writableDatabase.insert("users", null, values) != -1L
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun checkUser(email: String, password: String): Boolean {
        return try {
            readableDatabase.rawQuery(
                "SELECT id FROM users WHERE email=? AND password=?",
                arrayOf(email, password)
            ).use { cursor -> cursor.moveToFirst() }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // RECIPES
    // ════════════════════════════════════════════════════════════════════════

    fun addRecipe(
        title: String,
        description: String,
        portion: String,
        time: String,
        ingredients: String,
        steps: String,
        tips: String,
        image: String,
        category: String = "Plats"
    ): Boolean {
        return try {
            val values = ContentValues().apply {
                put("title",       title.trim())
                put("description", description.trim())
                put("portion",     portion.trim())
                put("time",        time.trim())
                put("ingredients", ingredients.trim())
                put("steps",       steps.trim())
                put("tips",        tips.trim())
                put("image",       image.trim())
                put("category",    category.trim())
            }
            writableDatabase.insert("recipes", null, values) != -1L
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun cursorToRecipe(cursor: android.database.Cursor): RecipeModel {
        return RecipeModel(
            id          = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
            title       = cursor.getString(cursor.getColumnIndexOrThrow("title"))       ?: "",
            description = cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: "",
            portion     = cursor.getString(cursor.getColumnIndexOrThrow("portion"))     ?: "",
            time        = cursor.getString(cursor.getColumnIndexOrThrow("time"))        ?: "",
            ingredients = cursor.getString(cursor.getColumnIndexOrThrow("ingredients")) ?: "",
            steps       = cursor.getString(cursor.getColumnIndexOrThrow("steps"))       ?: "",
            tips        = cursor.getString(cursor.getColumnIndexOrThrow("tips"))        ?: "",
            image       = cursor.getString(cursor.getColumnIndexOrThrow("image"))       ?: "",
            category    = try {
                cursor.getString(cursor.getColumnIndexOrThrow("category")) ?: "Plats"
            } catch (e: Exception) { "Plats" }
        )
    }

    fun getAllRecipes(): MutableList<RecipeModel> {
        val list = mutableListOf<RecipeModel>()
        return try {
            readableDatabase.rawQuery(
                "SELECT * FROM recipes ORDER BY id DESC", null
            ).use { cursor ->
                while (cursor.moveToNext()) list.add(cursorToRecipe(cursor))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            list
        }
    }

    fun getRecipeById(id: Int): RecipeModel? {
        return try {
            readableDatabase.rawQuery(
                "SELECT * FROM recipes WHERE id=?", arrayOf(id.toString())
            ).use { cursor ->
                if (cursor.moveToFirst()) cursorToRecipe(cursor) else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun updateRecipe(
        id: Int,
        title: String,
        description: String,
        portion: String,
        time: String,
        ingredients: String,
        steps: String,
        tips: String,
        image: String,
        category: String = "Plats"
    ): Boolean {
        return try {
            val values = ContentValues().apply {
                put("title",       title.trim())
                put("description", description.trim())
                put("portion",     portion.trim())
                put("time",        time.trim())
                put("ingredients", ingredients.trim())
                put("steps",       steps.trim())
                put("tips",        tips.trim())
                put("image",       image.trim())
                put("category",    category.trim())
            }
            writableDatabase.update("recipes", values, "id=?", arrayOf(id.toString())) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteRecipe(id: Int): Boolean {
        return try {
            writableDatabase.delete("recipes", "id=?", arrayOf(id.toString())) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // WEEK DISHES
    // ════════════════════════════════════════════════════════════════════════

    fun addDishToDay(day: String, dishName: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put("day",       day)
                put("dish_name", dishName)
            }
            writableDatabase.insert("week_dishes", null, values) != -1L
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getDishesForDay(day: String): MutableList<String> {
        val list = mutableListOf<String>()
        return try {
            readableDatabase.rawQuery(
                "SELECT dish_name FROM week_dishes WHERE day=?", arrayOf(day)
            ).use { cursor ->
                while (cursor.moveToNext())
                    list.add(cursor.getString(cursor.getColumnIndexOrThrow("dish_name")))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            list
        }
    }

    fun removeDishFromDay(day: String, dishName: String): Boolean {
        return try {
            writableDatabase.delete(
                "week_dishes", "day=? AND dish_name=?", arrayOf(day, dishName)
            ) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun clearWeekDishes() {
        try {
            writableDatabase.delete("week_dishes", null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // NOTIFICATIONS
    // ════════════════════════════════════════════════════════════════════════

    fun addNotification(title: String, message: String): Boolean {
        return try {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val values = ContentValues().apply {
                put("title",   title)
                put("message", message)
                put("time",    now)
            }
            writableDatabase.insert("notifications", null, values) != -1L
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getAllNotifications(): List<NotificationModel> {
        val list = mutableListOf<NotificationModel>()
        return try {
            readableDatabase.rawQuery(
                "SELECT * FROM notifications ORDER BY time DESC", null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        NotificationModel(
                            id      = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                            title   = cursor.getString(cursor.getColumnIndexOrThrow("title"))   ?: "",
                            message = cursor.getString(cursor.getColumnIndexOrThrow("message")) ?: "",
                            time    = cursor.getString(cursor.getColumnIndexOrThrow("time"))    ?: ""
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            list
        }
    }

    fun deleteNotification(id: Int): Boolean {
        return try {
            writableDatabase.delete("notifications", "id=?", arrayOf(id.toString())) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun clearAllNotifications(): Boolean {
        return try {
            writableDatabase.delete("notifications", null, null) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun markNotificationAsRead(id: Int): Boolean {
        return try {
            val values = ContentValues().apply { put("is_read", 1) }
            writableDatabase.update("notifications", values, "id = ?", arrayOf(id.toString())) > 0
        } catch (e: Exception) {
            false
        }
    }


    // Dans DatabaseHelper.kt

    fun getRecipesCount(): Int {
        return try {
            readableDatabase.rawQuery("SELECT COUNT(*) FROM recipes", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        } catch (e: Exception) { 0 }
    }

    fun getCategoriesCount(): Int {
        return try {
            // Compte le nombre de catégories uniques présentes dans la table recipes
            readableDatabase.rawQuery("SELECT COUNT(DISTINCT category) FROM recipes", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        } catch (e: Exception) { 0 }
    }
}