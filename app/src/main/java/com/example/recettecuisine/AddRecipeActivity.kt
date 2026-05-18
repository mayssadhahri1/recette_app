package com.example.recettecuisine

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

class AddRecipeActivity : AppCompatActivity() {


    private lateinit var db: DatabaseHelper


    private lateinit var stepsContainer: LinearLayout

    private var stepCount = 1

    private val stepEditTexts = mutableListOf<EditText>()

    /** Vue affichant la photo de la recette */
    private lateinit var imgRecipe: ImageView

    private lateinit var btnCamera: Button


    private lateinit var photoHint: LinearLayout

    /** Chemin du fichier image sauvegardé en stockage interne */
    private var savedImagePath: String = ""

    /** Code de demande de permission d'accès à la caméra */
    private val CAMERA_PERMISSION_CODE = 100

    /** Code de retour après l'ouverture de la caméra */
    private val CAMERA_REQUEST_CODE = 101

    /**
     * Initialise l'interface utilisateur et configure les listeners
     * des boutons, du spinner de catégories et de la caméra.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)

        // Création du canal de notifications si nécessaire
        NotificationHelper.createNotificationChannel(this)
        db = DatabaseHelper(this)

        // Récupération de toutes les vues depuis le layout
        val btnSaveBottom   = findViewById<Button>(R.id.btnSaveBottom)
        val etTitle         = findViewById<EditText>(R.id.etTitle)
        val etDescription   = findViewById<EditText>(R.id.etDescription)
        val etPortion       = findViewById<EditText>(R.id.etPortion)
        val etTime          = findViewById<EditText>(R.id.etTime)
        val etIngredients   = findViewById<EditText>(R.id.etIngredients)
        val etTips          = findViewById<EditText>(R.id.etTips)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        stepsContainer      = findViewById(R.id.stepsContainer)
        imgRecipe           = findViewById(R.id.imgRecipe)
        btnCamera           = findViewById(R.id.btnCamera)
        photoHint           = findViewById(R.id.photoHint)

        // Bouton retour : ferme l'activité en cours
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        // Configuration du spinner avec la liste de toutes les catégories disponibles
        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            RecipeCategory.ALL_CATEGORIES
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Ajout de la première étape déjà présente dans le layout XML
        val etStep1 = findViewById<EditText>(R.id.etStep1)
        stepEditTexts.add(etStep1)

        // Bouton "Ajouter une étape" : crée dynamiquement une nouvelle ligne d'étape
        findViewById<Button>(R.id.btnAddStep).setOnClickListener { addStep() }

        // Caméra : vérifie la permission avant d'ouvrir l'appareil photo
        btnCamera.setOnClickListener {
            if (checkCameraPermission()) openCamera() else requestCameraPermission()
        }

        // Bouton enregistrer en bas de l'écran
        btnSaveBottom.setOnClickListener { doSave() }
    }

    /**
     * Récupère les données saisies par l'utilisateur, valide le titre,
     * assemble les étapes en une seule chaîne, puis enregistre la recette
     * en base de données et envoie une notification de confirmation.
     */
    private fun doSave() {
        // Lecture des champs du formulaire
        val title            = findViewById<EditText>(R.id.etTitle).text.toString().trim()
        val description      = findViewById<EditText>(R.id.etDescription).text.toString().trim()
        val portion          = findViewById<EditText>(R.id.etPortion).text.toString().trim()
        val time             = findViewById<EditText>(R.id.etTime).text.toString().trim()
        val ingredients      = findViewById<EditText>(R.id.etIngredients).text.toString().trim()
        val tips             = findViewById<EditText>(R.id.etTips).text.toString().trim()
        val selectedCategory = findViewById<Spinner>(R.id.spinnerCategory).selectedItem?.toString() ?: ""

        // Validation : le titre est obligatoire
        if (title.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir le titre", Toast.LENGTH_SHORT).show()
            return
        }

        // Concaténation des étapes non vides séparées par " | "
        val steps = stepEditTexts
            .map { it.text.toString().trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" | ")

        // Insertion en base de données et vérification du résultat
        val success = db.addRecipe(
            title, description, portion, time, ingredients, steps, tips,
            savedImagePath, category = selectedCategory
        )

        if (success) {
            // Enregistrement d'une notification interne et envoi de la notif système
            db.addNotification("Nouvelle recette", "La recette \"$title\" a été ajoutée !")
            NotificationHelper.sendNotification(this, "Nouvelle recette",
                "La recette \"$title\" a été ajoutée !", 1)
            Toast.makeText(this, "Recette ajoutée ✅", Toast.LENGTH_SHORT).show()
            finish() // Retour à l'écran précédent
        } else {
            Toast.makeText(this, "Erreur lors de l'ajout ❌", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Crée dynamiquement une nouvelle ligne d'étape composée d'un badge
     * numéroté, d'un champ de saisie et d'un bouton de suppression.
     * La nouvelle étape est ajoutée à la liste et au conteneur visuel.
     */
    private fun addStep() {
        stepCount++

        // Conversion des valeurs dp en pixels selon la densité de l'écran
        val dp10 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()
        val dp12 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics).toInt()
        val dp14 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, resources.displayMetrics).toInt()
        val dp50 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics).toInt()

        // Ligne horizontale contenant le badge, le champ et le bouton supprimer
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp10
            layoutParams = lp
        }

        // Badge circulaire affichant le numéro de l'étape
        val badge = TextView(this).apply {
            text = "$stepCount"
            textSize = 12f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.bg_step_badge)
            val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28f, resources.displayMetrics).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = dp10 }
        }

        // Champ de saisie pour décrire le contenu de l'étape
        val etStep = EditText(this).apply {
            hint = "Décrire l'étape $stepCount..."
            setBackgroundResource(R.drawable.bg_edittext_dark)
            setPadding(dp14, dp12, dp14, dp12)
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.parseColor("#4A5568"))
            layoutParams = LinearLayout.LayoutParams(0, dp50, 1f)
        }

        // Bouton "✕" pour supprimer cette étape et renuméroter les suivantes
        val btnRemove = Button(this).apply {
            text = "✕"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#8A9BB0"))
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setOnClickListener {
                stepsContainer.removeView(row)   // Supprime la ligne du conteneur
                stepEditTexts.remove(etStep)      // Retire le champ de la liste
                renumberSteps()                  // Met à jour les numéros d'étapes
            }
        }

        // Assemblage de la ligne et ajout dans le conteneur
        row.addView(badge)
        row.addView(etStep)
        row.addView(btnRemove)
        stepsContainer.addView(row)
        stepEditTexts.add(etStep)
    }


    private fun renumberSteps() {
        stepCount = stepEditTexts.size
        for (i in 0 until stepsContainer.childCount) {
            val child = stepsContainer.getChildAt(i)
            if (child is LinearLayout) {
                val badge = child.getChildAt(0)
                if (badge is TextView) badge.text = "${i + 1}"
            }
        }
    }

    // ── Gestion de la caméra ──────────────────────────────────────────────────

    /** Vérifie si la permission d'accès à la caméra est déjà accordée */
    private fun checkCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    /** Demande la permission d'utiliser la caméra à l'utilisateur */
    private fun requestCameraPermission() =
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            openCamera()
        else
            Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
    }

    /** Lance l'intent de capture photo via l'application caméra native */
    private fun openCamera() =
        startActivityForResult(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE), CAMERA_REQUEST_CODE)


    private fun saveImageToInternalStorage(bitmap: Bitmap): String {
        val file = File(filesDir, "recipe_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return file.absolutePath
    }

    /**
     * Reçoit le résultat de la caméra.
     * Si une photo a été prise, elle est affichée dans imgRecipe,
     * l'indice photo est masqué et l'image est sauvegardée en interne.
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val photo = data?.extras?.get("data") as? Bitmap
            if (photo != null) {
                imgRecipe.setImageBitmap(photo)                      // Affiche la photo
                photoHint.visibility = android.view.View.GONE         // Cache l'indice
                savedImagePath = saveImageToInternalStorage(photo)   // Enregistre sur disque
            }
        }
    }
}