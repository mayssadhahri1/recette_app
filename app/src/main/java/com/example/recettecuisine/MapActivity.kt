package com.example.recettecuisine

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException
import java.util.concurrent.TimeUnit

class MapActivity : AppCompatActivity() {

    // ── Variables de l'écran ──────────────────────────────────────────────────

    private lateinit var map: MapView                  // La carte
    private lateinit var tvCount: TextView             // Texte "X trouvés"
    private lateinit var tvNearestName: TextView       // Nom du restaurant le plus proche
    private lateinit var tvNearestDistance: TextView   // Distance du plus proche
    private lateinit var bottomSheet: View             // Panneau en bas de l'écran
    private lateinit var fabMyLocation: FloatingActionButton  // Bouton rond pour recentrer
    private lateinit var chipGroup: ChipGroup          // Boutons filtres (resto, café...)

    private var userLocation: GeoPoint? = null  // Position GPS de l'utilisateur
    private var nearestLat: Double = 0.0        // Latitude du resto le plus proche
    private var nearestLng: Double = 0.0        // Longitude du resto le plus proche
    private var currentType: String = "restaurant"  // Type sélectionné par défaut

    // ── Démarrage de l'écran ──────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise OSMDroid (la bibliothèque de carte gratuite)
        // Sans ça, la carte ne s'affiche pas
        Configuration.getInstance().load(
            this, getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        // Charge le fichier XML de l'interface
        setContentView(R.layout.activity_map)

        // Connecte les variables aux éléments visuels du XML
        map               = findViewById(R.id.map)
        tvCount           = findViewById(R.id.tvCount)
        tvNearestName     = findViewById(R.id.tvNearestName)
        tvNearestDistance = findViewById(R.id.tvNearestDistance)
        bottomSheet       = findViewById(R.id.bottomSheet)
        fabMyLocation     = findViewById(R.id.fabMyLocation)
        chipGroup         = findViewById(R.id.chipGroupFilter)

        // Quand on clique sur la flèche retour → ferme cet écran
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // Configure la carte : fond de carte OpenStreetMap, zoom tactile activé
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)  // Zoom de départ (15 = vue quartier)

        // Ajoute un point bleu qui suit la position GPS en temps réel
        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)

        // Bouton rond en bas à droite → recentre la carte sur l'utilisateur
        fabMyLocation.setOnClickListener {
            userLocation?.let {
                map.controller.animateTo(it)   // Animation vers la position
                map.controller.setZoom(16.0)   // Zoom un peu plus proche
            }
        }

        // Quand l'utilisateur change de filtre (Restaurant / Café / Fast-food / Boulangerie)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->

            // Met à jour le type selon le bouton sélectionné
            currentType = when {
                checkedIds.contains(R.id.chipCafe)   -> "cafe"
                checkedIds.contains(R.id.chipFast)   -> "fast_food"
                checkedIds.contains(R.id.chipBakery) -> "bakery"
                else -> "restaurant"
            }

            // Efface tous les markers de la carte
            map.overlays.clear()

            // Remet le point bleu GPS (on vient de tout effacer)
            val locOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
            locOverlay.enableMyLocation()
            map.overlays.add(locOverlay)

            // Recharge les lieux selon le nouveau filtre
            userLocation?.let { loc ->
                addUserMarker(loc.latitude, loc.longitude)
                loadPlaces(loc.latitude, loc.longitude, currentType)
            }
        }

        // Bouton "Itinéraire" dans le panneau du bas
        // → Ouvre Google Maps (ou OpenStreetMap si Google Maps absent)
        findViewById<View>(R.id.btnItinerary).setOnClickListener {
            if (nearestLat != 0.0 && nearestLng != 0.0) {

                // Essaie d'ouvrir Google Maps
                val uri = Uri.parse("geo:$nearestLat,$nearestLng?q=$nearestLat,$nearestLng")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")

                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)  // Google Maps disponible
                } else {
                    // Google Maps absent → ouvre dans le navigateur web
                    val browserUri = Uri.parse(
                        "https://www.openstreetmap.org/directions?to=$nearestLat,$nearestLng"
                    )
                    startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                }
            }
        }

        // Lance la récupération de la position GPS
        getMyLocation()
    }

    // ── Récupérer la position GPS ─────────────────────────────────────────────

    private fun getMyLocation() {

        // Vérifie si l'utilisateur a accordé la permission GPS
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Pas de permission → demande à l'utilisateur
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001
            )
            return
        }

        // Récupère la dernière position GPS connue
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

            // Si GPS indisponible → utilise Sousse comme position par défaut
            val lat: Double = location?.latitude ?: 35.8256
            val lng: Double = location?.longitude ?: 10.6084

            if (location == null) {
                Toast.makeText(
                    this, "GPS indispo → Sousse par défaut", Toast.LENGTH_SHORT
                ).show()
            }

            // Sauvegarde la position et centre la carte dessus
            userLocation = GeoPoint(lat, lng)
            map.controller.setCenter(userLocation)
            map.controller.setZoom(15.0)

            // Ajoute le marker "Vous êtes ici"
            addUserMarker(lat, lng)

            // Lance la recherche de restaurants autour de cette position
            loadPlaces(lat, lng, currentType)
        }
    }

    // Ajoute un marker rouge "📍 Vous êtes ici" sur la carte
    private fun addUserMarker(lat: Double, lng: Double) {
        val myMarker = Marker(map)
        myMarker.position = GeoPoint(lat, lng)
        myMarker.title = "📍 Vous êtes ici"
        myMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(myMarker)
        map.invalidate()  // Rafraîchit l'affichage de la carte
    }

    // ── Chargement des lieux ──────────────────────────────────────────────────

    private fun loadPlaces(lat: Double, lng: Double, type: String) {
        tvCount.text = "Recherche..."

        // Liste de 3 serveurs Overpass (base de données OpenStreetMap)
        // Si le 1er est en panne → essaie le 2ème, puis le 3ème
        val servers = listOf(
            "https://overpass.kumi.systems/api/interpreter",     // Serveur 1 (Europe)
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter",  // Serveur 2 (Russie)
            "https://overpass-api.de/api/interpreter"            // Serveur 3 (Officiel)
        )

        // Convertit le type interne en nom reconnu par OpenStreetMap
        val amenity = when (type) {
            "cafe"      -> "cafe"
            "fast_food" -> "fast_food"
            "bakery"    -> "bakery"
            else        -> "restaurant"
        }

        // Commence par le premier serveur (index 0)
        tryNextServer(servers, 0, amenity, lat, lng)
    }

    // Essaie de contacter un serveur, si ça échoue → passe au suivant
    private fun tryNextServer(
        servers: List<String>,
        index: Int,       // Numéro du serveur à essayer
        amenity: String,  // Type de lieu (restaurant, cafe...)
        lat: Double,
        lng: Double
    ) {
        // Si on a essayé tous les serveurs sans succès → affiche erreur
        if (index >= servers.size) {
            runOnUiThread {
                tvCount.text = "0 trouvés"
                Toast.makeText(
                    this,
                    "❌ Tous les serveurs sont indisponibles, réessaie plus tard",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        // Requête Overpass : cherche tous les lieux du type voulu
        // dans un rayon de 1500m autour de la position
        val query: String =
            "[out:json][timeout:25];node[\"amenity\"=\"$amenity\"](around:1500,$lat,$lng);out body;"

        // Client HTTP avec délai max de 25 secondes
        val client = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()

        val body: RequestBody = query.toRequestBody("text/plain".toMediaType())

        // Prépare la requête HTTP vers le serveur Overpass
        val request = Request.Builder()
            .url(servers[index])
            .post(body)
            .addHeader("User-Agent", "RecetteCuisineApp/1.0")  // Obligatoire pour Overpass
            .build()

        // Informe l'utilisateur du serveur en cours d'essai
        runOnUiThread {
            Toast.makeText(
                this,
                "🔄 Serveur ${index + 1}/${servers.size}...",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Envoie la requête de façon asynchrone (sans bloquer l'écran)
        client.newCall(request).enqueue(object : Callback {

            // Si la connexion échoue → essaie le serveur suivant
            override fun onFailure(call: Call, e: IOException) {
                tryNextServer(servers, index + 1, amenity, lat, lng)
            }

            // Si le serveur répond
            override fun onResponse(call: Call, response: Response) {
                val json: String? = response.body?.string()

                // Si la réponse est vide, une page HTML d'erreur,
                // ou un code HTTP d'erreur → essaie le serveur suivant
                if (json == null ||
                    json.trimStart().startsWith("<") ||
                    !response.isSuccessful
                ) {
                    tryNextServer(servers, index + 1, amenity, lat, lng)
                    return
                }

                // Retourne sur le fil principal pour modifier l'interface
                runOnUiThread {
                    try {
                        // Transforme le texte JSON en objet exploitable
                        val obj = JSONObject(json)
                        val elements: JSONArray = obj.getJSONArray("elements")

                        if (elements.length() == 0) {
                            // Aucun lieu trouvé dans ce rayon
                            tvCount.text = "0 trouvés"
                            Toast.makeText(
                                this@MapActivity,
                                "Aucun endroit trouvé à proximité",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Affiche les markers sur la carte
                            showPlacesOnMap(elements, lat, lng)
                        }

                    } catch (e: Exception) {
                        // Erreur de parsing JSON → essaie le serveur suivant
                        tryNextServer(servers, index + 1, amenity, lat, lng)
                    }
                }
            }
        })
    }

    // ── Affichage des markers sur la carte ────────────────────────────────────

    private fun showPlacesOnMap(elements: JSONArray, userLat: Double, userLng: Double) {

        // Variables pour trouver le restaurant le plus proche
        var minDistance: Float = Float.MAX_VALUE  // Distance minimum trouvée
        var nearestPoint: GeoPoint? = null        // Coordonnées du plus proche
        var nearestName: String = ""              // Nom du plus proche

        // Parcourt tous les lieux retournés par l'API
        for (i in 0 until elements.length()) {
            val item = elements.getJSONObject(i)

            val lat: Double = item.getDouble("lat")  // Latitude du lieu
            val lon: Double = item.getDouble("lon")  // Longitude du lieu

            // Récupère le nom dans les tags OSM
            // Si pas de nom → utilise le nom par défaut selon le type
            val tags = item.optJSONObject("tags")
            val name: String = tags?.optString("name")
                ?.takeIf { it.isNotEmpty() } ?: getDefaultName(currentType)

            // Calcule la distance entre l'utilisateur et ce lieu (en mètres)
            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLng, lat, lon, results)
            val distance: Float = results[0]

            // Ajoute un marker orange sur la carte pour ce lieu
            val marker = Marker(map)
            marker.position = GeoPoint(lat, lon)
            marker.title = name
            marker.subDescription = "${distance.toInt()} m"  // Affiché sous le nom
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(marker)

            // Si ce lieu est plus proche que le précédent minimum → sauvegarde
            if (distance < minDistance) {
                minDistance = distance
                nearestPoint = GeoPoint(lat, lon)
                nearestName = name
                nearestLat = lat   // Sauvegarde pour le bouton Itinéraire
                nearestLng = lon
            }
        }

        // Met à jour le compteur en haut (ex: "12 trouvés")
        tvCount.text = "${elements.length()} trouvés"
        map.invalidate()  // Rafraîchit la carte

        // Traitement spécial pour le restaurant le plus proche
        nearestPoint?.let { point ->

            // Ajoute un 2ème marker ⭐ par-dessus le plus proche
            val nearestMarker = Marker(map)
            nearestMarker.position = point
            nearestMarker.title = "⭐ $nearestName"
            nearestMarker.subDescription = "Le plus proche — ${minDistance.toInt()} m"
            nearestMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(nearestMarker)
            map.invalidate()

            // Remplit le panneau du bas avec les infos du plus proche
            tvNearestName.text = nearestName
            tvNearestDistance.text = "📍 ${minDistance.toInt()} mètres de vous"

            // Fait monter le panneau du bas avec une animation (400ms)
            bottomSheet.animate()
                .translationY(0f)   // Position finale : visible
                .setDuration(400)   // Durée de l'animation
                .start()

            // Message récapitulatif en bas de l'écran
            Toast.makeText(
                this,
                "🍽️ Plus proche : $nearestName à ${minDistance.toInt()} m",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Retourne le nom par défaut selon le type de lieu
    private fun getDefaultName(type: String): String = when (type) {
        "cafe"      -> "Café"
        "fast_food" -> "Fast-food"
        "bakery"    -> "Boulangerie"
        else        -> "Restaurant"
    }

    // ── Gestion des permissions ───────────────────────────────────────────────

    // Appelé automatiquement après que l'utilisateur accepte/refuse la permission GPS
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission accordée → relance la récupération GPS
            getMyLocation()
        }
    }

    // ── Cycle de vie de la carte ──────────────────────────────────────────────

    // OSMDroid a besoin de ces deux méthodes pour gérer la carte correctement
    override fun onResume() { super.onResume(); map.onResume() }  // Reprend la carte
    override fun onPause()  { super.onPause();  map.onPause()  }  // Met en pause la carte
}