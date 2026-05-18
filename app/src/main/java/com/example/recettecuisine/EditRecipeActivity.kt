package com.example.recettecuisine

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity


class EditRecipeActivity : AppCompatActivity() {

    /** Accès à la base de données locale */
    private lateinit var db: DatabaseHelper

    // ── Champs de saisie du formulaire ────────────────────────────────────────
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etPortions: EditText
    private lateinit var etTime: EditText
    private lateinit var etTips: EditText
    private lateinit var spinnerCategory: Spinner

    // ── Conteneurs dynamiques pour les listes d'ingrédients et d'étapes ───────
    private lateinit var layoutIngredients: LinearLayout
    private lateinit var layoutSteps: LinearLayout

    // ── Boutons d'action ──────────────────────────────────────────────────────
    private lateinit var btnUpdate: Button
    private lateinit var btnCancel: Button
    private lateinit var btnAddIngredient: Button
    private lateinit var btnAddStep: Button
    private lateinit var btnCameraTop: Button
    private lateinit var btnBack: ImageButton
    private lateinit var imgRecipe: ImageView

    // ── Boutons de sélection du niveau de difficulté ──────────────────────────
    private lateinit var btnFacile: Button
    private lateinit var btnMoyen: Button
    private lateinit var btnDifficile: Button

    /** Niveau de difficulté actuellement sélectionné (par défaut : "Moyen") */
    private var selectedDifficulty = "Moyen"

    /** Identifiant de la recette à modifier, transmis via l'Intent */
    private var recipeId: Int = -1

    /** Chemin ou URI de l'image associée à la recette */
    private var recipeImage: String = ""

    /** Références aux EditText des lignes d'ingrédients ajoutées dynamiquement */
    private val ingredientViews = mutableListOf<EditText>()

    /** Références aux EditText des lignes d'étapes ajoutées dynamiquement */
    private val stepViews = mutableListOf<EditText>()

    /**
     * Lanceur du sélecteur d'image système (galerie).
     * Lorsqu'une image est choisie, elle est affichée dans [imgRecipe]
     * et son URI est conservé dans [recipeImage] pour la sauvegarde.
     */
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imgRecipe.setImageURI(it)
                recipeImage = it.toString()
            }
        }

    /**
     * Initialise l'interface, récupère l'ID de la recette depuis l'Intent,
     * charge ses données et configure tous les listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_recipe)

        db = DatabaseHelper(this)

        // Liaison des vues avec leurs identifiants XML
        etTitle           = findViewById(R.id.etTitle)
        etDescription     = findViewById(R.id.etDescription)
        etPortions        = findViewById(R.id.etPortions)
        etTime            = findViewById(R.id.etTime)
        etTips            = findViewById(R.id.etTips)
        spinnerCategory   = findViewById(R.id.spinnerCategory)
        layoutIngredients = findViewById(R.id.layoutIngredients)
        layoutSteps       = findViewById(R.id.layoutSteps)
        btnUpdate         = findViewById(R.id.btnUpdate)
        btnCancel         = findViewById(R.id.btnCancel)
        btnAddIngredient  = findViewById(R.id.btnAddIngredient)
        btnAddStep        = findViewById(R.id.btnAddStep)
        btnCameraTop      = findViewById(R.id.btnCameraTop)
        btnBack           = findViewById(R.id.btnBack)
        imgRecipe         = findViewById(R.id.imgRecipe)
        btnFacile         = findViewById(R.id.btnFacile)
        btnMoyen          = findViewById(R.id.btnMoyen)
        btnDifficile      = findViewById(R.id.btnDifficile)

        // Initialisation du spinner avec la liste des catégories disponibles
        val categories = listOf("Desserts", "Entrées", "Plat")
        spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Configuration des boutons de difficulté (couleur active / inactive)
        setupDifficultyButtons()

        // Listeners : ouverture du sélecteur d'image via le bouton caméra ou l'image elle-même
        btnCameraTop.setOnClickListener { imagePicker.launch("image/*") }
        imgRecipe.setOnClickListener   { imagePicker.launch("image/*") }

        // Listeners : navigation (retour et annulation ferment l'activité)
        btnBack.setOnClickListener   { finish() }
        btnCancel.setOnClickListener { finish() }

        // Listeners : ajout d'une ligne vide d'ingrédient ou d'étape
        btnAddIngredient.setOnClickListener { addIngredientRow("") }
        btnAddStep.setOnClickListener       { addStepRow("") }

        // Récupération de l'ID de la recette transmis par l'activité appelante
        recipeId = intent.getIntExtra("recipeId", -1)
        if (recipeId == -1) {
            // ID invalide : impossible d'identifier la recette, on quitte
            Toast.makeText(this, "Recette introuvable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Chargement des données de la recette dans le formulaire
        loadRecipe(recipeId)

        // Bouton de sauvegarde des modifications
        btnUpdate.setOnClickListener { updateRecipe() }
    }

    // ── Gestion de la difficulté ──────────────────────────────────────────────

    /**
     * Configure les trois boutons de difficulté (Facile / Moyen / Difficile).
     * Le bouton actif est mis en surbrillance (bleu), les autres restent neutres.
     * La fonction interne [refresh] est appelée à chaque changement de sélection
     * ainsi qu'à l'initialisation pour refléter la valeur par défaut.
     */
    private fun setupDifficultyButtons() {
        val activeColor   = 0xFF2563EB.toInt() // Bleu actif
        val inactiveColor = 0xFF2A2D45.toInt() // Gris foncé inactif

        // Met à jour la couleur de fond des trois boutons selon la sélection courante
        fun refresh() {
            btnFacile.backgroundTintList    = android.content.res.ColorStateList.valueOf(
                if (selectedDifficulty == "Facile")    activeColor else inactiveColor)
            btnMoyen.backgroundTintList     = android.content.res.ColorStateList.valueOf(
                if (selectedDifficulty == "Moyen")     activeColor else inactiveColor)
            btnDifficile.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (selectedDifficulty == "Difficile") activeColor else inactiveColor)
        }

        // Chaque bouton met à jour la sélection puis rafraîchit l'affichage
        btnFacile.setOnClickListener    { selectedDifficulty = "Facile";    refresh() }
        btnMoyen.setOnClickListener     { selectedDifficulty = "Moyen";     refresh() }
        btnDifficile.setOnClickListener { selectedDifficulty = "Difficile"; refresh() }

        refresh() // Applique l'état initial
    }

    // ── Ajout d'un ingrédient ─────────────────────────────────────────────────

    /**
     * Gonfle dynamiquement une nouvelle ligne d'ingrédient dans [layoutIngredients].
     * La ligne contient un champ de saisie pré-rempli avec [text] et un bouton
     * de suppression qui retire la ligne du conteneur et de la liste de références.
     *
     * @param text Valeur initiale du champ (vide pour une nouvelle ligne, pré-rempli au chargement)
     */
    private fun addIngredientRow(text: String) {
        // Inflation du layout de la ligne depuis le fichier XML dédié
        val row = LayoutInflater.from(this)
            .inflate(R.layout.item_ingredient_edit, layoutIngredients, false)

        val etIngredient = row.findViewById<EditText>(R.id.etIngredientItem)
        val btnDelete    = row.findViewById<ImageButton>(R.id.btnDeleteIngredient)

        etIngredient.setText(text)
        ingredientViews.add(etIngredient) // Conserve la référence pour la collecte des données

        // Suppression de la ligne et retrait de la référence
        btnDelete.setOnClickListener {
            layoutIngredients.removeView(row)
            ingredientViews.remove(etIngredient)
        }

        layoutIngredients.addView(row)
    }

    // ── Ajout d'une étape ─────────────────────────────────────────────────────

    /**
     * Gonfle dynamiquement une nouvelle ligne d'étape dans [layoutSteps].
     * Le numéro de l'étape est calculé à partir du nombre d'étapes déjà présentes.
     * La suppression d'une ligne déclenche une renumérotation de toutes les étapes restantes.
     *
     * @param text Valeur initiale du champ (vide pour une nouvelle ligne, pré-rempli au chargement)
     */
    private fun addStepRow(text: String) {
        // Inflation du layout de la ligne depuis le fichier XML dédié
        val row = LayoutInflater.from(this)
            .inflate(R.layout.item_step_edit, layoutSteps, false)

        val tvNumber  = row.findViewById<TextView>(R.id.tvStepNumber)
        val etStep    = row.findViewById<EditText>(R.id.etStepItem)
        val btnDelete = row.findViewById<ImageButton>(R.id.btnDeleteStep)

        // Numérotation automatique basée sur la position dans la liste
        tvNumber.text = (stepViews.size + 1).toString()
        etStep.setText(text)
        stepViews.add(etStep) // Conserve la référence pour la collecte des données

        btnDelete.setOnClickListener {
            layoutSteps.removeView(row)
            stepViews.remove(etStep)

            // Renumérotation de toutes les étapes restantes après suppression
            for (i in 0 until layoutSteps.childCount) {
                layoutSteps.getChildAt(i)
                    .findViewById<TextView>(R.id.tvStepNumber)
                    ?.let { it.text = (i + 1).toString() }
            }
        }

        layoutSteps.addView(row)
    }

    // ── Chargement de la recette ──────────────────────────────────────────────

    /**
     * Charge les données d'une recette depuis la base et remplit le formulaire.
     * Si la recette est introuvable (id invalide ou supprimée), l'activité se ferme.
     *
     * @param id Identifiant de la recette à charger
     */
    private fun loadRecipe(id: Int) {
        // Récupération de la recette ; fermeture si introuvable
        val recipe = db.getRecipeById(id) ?: run {
            Toast.makeText(this, "Recette introuvable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Remplissage des champs texte
        etTitle.setText(recipe.title)
        etDescription.setText(recipe.description)
        etPortions.setText(recipe.portion)
        etTime.setText(recipe.time)
        etTips.setText(recipe.tips)

        // Sélection de la catégorie dans le spinner (retombe sur 0 si non trouvée)
        val categories = listOf("Desserts", "Entrées", "Plats principaux", "Soupes", "Salades", "Boissons")
        val catIndex = categories.indexOf(recipe.category).takeIf { it >= 0 } ?: 0
        spinnerCategory.setSelection(catIndex)

        // Création d'une ligne par ingrédient (séparés par retour à la ligne)
        recipe.ingredients
            .split("\n")
            .filter { it.isNotBlank() }
            .forEach { addIngredientRow(it) }

        // Création d'une ligne par étape (séparées par retour à la ligne)
        recipe.steps
            .split("\n")
            .filter { it.isNotBlank() }
            .forEach { addStepRow(it) }

        // Chargement de l'image : tentative d'affichage via URI, icône par défaut si échec
        recipeImage = recipe.image
        if (recipe.image.isNotEmpty()) {
            try {
                imgRecipe.setImageURI(Uri.parse(recipe.image))
            } catch (e: Exception) {
                // URI invalide ou image inaccessible : affiche une icône de remplacement
                imgRecipe.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        // Affichage du nom de la recette dans le titre de la page (si la vue existe)
        findViewById<TextView>(R.id.tvRecipeName)?.let { it.text = recipe.title }
    }

    // ── Sauvegarde des modifications ──────────────────────────────────────────

    /**
     * Collecte les valeurs de tous les champs du formulaire, valide le titre,
     * puis met à jour la recette en base via [DatabaseHelper].
     *
     * Remarque : le champ "difficulté" ([selectedDifficulty]) n'est pas encore
     * transmis à [DatabaseHelper.updateRecipe] car le paramètre n'existe pas.
     *
     * En cas de succès, une notification interne est enregistrée et l'activité
     * se ferme pour revenir à l'écran précédent.
     */
    private fun updateRecipe() {
        // Lecture des champs du formulaire
        val title       = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val portions    = etPortions.text.toString().trim()
        val time        = etTime.text.toString().trim()
        val category    = spinnerCategory.selectedItem?.toString() ?: ""
        val tips        = etTips.text.toString().trim()

        // Collecte des ingrédients depuis les lignes dynamiques (lignes vides ignorées)
        val ingredients: String = ingredientViews
            .map { it.text.toString().trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")

        // Collecte des étapes depuis les lignes dynamiques (lignes vides ignorées)
        val steps: String = stepViews
            .map { it.text.toString().trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")

        // Validation : le titre est obligatoire
        if (title.isEmpty()) {
            Toast.makeText(this, "Veuillez saisir le titre de la recette", Toast.LENGTH_SHORT).show()
            return
        }

        // NOTE : la difficulté (selectedDifficulty) n'est pas encore prise en compte
        // par DatabaseHelper.updateRecipe — à intégrer lors d'une prochaine mise à jour.
        val success = db.updateRecipe(
            id          = recipeId,
            title       = title,
            description = description,
            portion     = portions,
            time        = time,
            ingredients = ingredients,
            steps       = steps,
            tips        = tips,
            image       = recipeImage,
            category    = category
        )

        if (success) {
            // Mise à jour réussie : notification interne + retour à l'écran précédent
            db.addNotification(
                title   = "Recette modifiée",
                message = "La recette '$title' a été mise à jour avec succès ✅"
            )
            Toast.makeText(this, "Recette modifiée ✅", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            // Échec de la mise à jour en base de données
            Toast.makeText(this, "Erreur lors de la modification ❌", Toast.LENGTH_SHORT).show()
        }
    }
}