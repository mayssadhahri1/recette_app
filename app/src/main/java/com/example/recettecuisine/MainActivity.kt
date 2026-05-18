package com.example.recettecuisine

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton
import com.google.android.gms.location.*
import android.location.Geocoder
import java.util.Locale

/**
 * Écran de démarrage (splash / accueil) de l'application.
 *
 * Responsabilités :
 * - Demande et vérifie la permission de géolocalisation précise (GPS)
 * - Démarre les mises à jour de position via [FusedLocationProviderClient]
 * - Convertit les coordonnées GPS en adresse lisible grâce à [Geocoder]
 * - Affiche la ville, le quartier, le pays et les coordonnées dans [tvLocation]
 * - Redirige vers [HomeActivity] via le bouton "Commencer"
 *
 * Les mises à jour GPS sont suspendues dans [onPause] pour économiser la batterie.
 */
class MainActivity : AppCompatActivity() {

    /** Client GPS fourni par Google Play Services pour obtenir la position fusionnée */
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    /** Callback déclenché à chaque nouvelle position reçue du client GPS */
    private lateinit var locationCallback: LocationCallback

    /** Code identifiant la demande de permission de localisation */
    private val LOCATION_REQUEST_CODE = 1001

    /** Affiche les informations de localisation (ville, quartier, coordonnées) */
    private lateinit var tvLocation: TextView

    /**
     * Initialise l'interface, le client GPS et vérifie la permission
     * de localisation avant de démarrer les mises à jour de position.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLocation = findViewById(R.id.tvLocation)
        val btnGetStarted = findViewById<MaterialButton>(R.id.btnGetStarted)

        // Initialisation du client de localisation fusionnée (GPS + réseau)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Vérification de la permission avant toute tentative d'accès au GPS
        checkLocationPermission()

        // Bouton "Commencer" : navigue vers l'écran principal de l'application
        btnGetStarted.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }

    /**
     * Vérifie si la permission [ACCESS_FINE_LOCATION] est déjà accordée.
     * - Si oui : démarre immédiatement les mises à jour GPS
     * - Si non : affiche la boîte de dialogue système de demande de permission
     */
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission absente : demande à l'utilisateur via la boîte de dialogue système
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        } else {
            // Permission déjà accordée : démarrage direct des mises à jour
            startLocationUpdates()
        }
    }

    /**
     * Callback du système après réponse de l'utilisateur à la demande de permission.
     * - Permission accordée → démarre les mises à jour GPS
     * - Permission refusée → affiche un message d'erreur dans [tvLocation]
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Accord de l'utilisateur : démarrage des mises à jour
            startLocationUpdates()
        } else {
            // Refus de l'utilisateur : affichage d'un message dans la vue
            tvLocation.text = "Permission refusée ❌"
        }
    }

    /**
     * Configure et démarre les mises à jour de position GPS.
     *
     * La position est demandée toutes les 5 secondes avec une haute précision.
     * À chaque nouvelle position, le [Geocoder] tente de résoudre les coordonnées
     * en adresse lisible (quartier, ville, pays). En cas d'échec du géocodage
     * (adresse nulle ou exception), les coordonnées brutes sont affichées.
     *
     * La méthode vérifie une seconde fois la permission avant d'enregistrer
     * le callback (exigence du compilateur Android pour les APIs de localisation).
     */
    private fun startLocationUpdates() {

        // Paramètres de la requête : haute précision, intervalle de 5 secondes
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        // Callback déclenché à chaque mise à jour de position
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location: Location in result.locations) {

                    val latitude  = location.latitude
                    val longitude = location.longitude

                    // Géocodage inverse : conversion des coordonnées en adresse postale
                    val geocoder = Geocoder(this@MainActivity, Locale.getDefault())

                    try {
                        // Récupération du premier résultat d'adresse (1 résultat demandé)
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                        if (addresses != null && addresses.isNotEmpty()) {
                            val address = addresses[0]

                            // Extraction des composantes de l'adresse
                            val city    = address.locality     // Ville (ex : Sousse)
                            val area    = address.subLocality  // Quartier (ex : Sahloul)
                            val country = address.countryName  // Pays (ex : Tunisie)

                            // Affichage complet : quartier, ville, pays et coordonnées GPS
                            tvLocation.text = """
                        📍 $area
                        🌍 $city - $country
                        Lat: $latitude
                        Lng: $longitude
                    """.trimIndent()

                        } else {
                            // Géocodage sans résultat : affichage des coordonnées brutes
                            tvLocation.text = "Lat: $latitude\nLng: $longitude"
                        }

                    } catch (e: Exception) {
                        // Échec du géocodage (réseau indisponible, service injoignable…)
                        tvLocation.text = "Lat: $latitude\nLng: $longitude"
                    }
                }
            }
        }

        // Seconde vérification de permission requise par le compilateur avant requestLocationUpdates
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Enregistrement du callback sur le thread principal (mainLooper)
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
        }
    }

    /**
     * Arrête les mises à jour GPS lorsque l'activité passe en arrière-plan.
     * Évite une consommation inutile de batterie et de données pendant que
     * l'application n'est pas visible.
     */
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}