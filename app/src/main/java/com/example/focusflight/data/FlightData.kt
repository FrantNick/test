package com.example.focusflight.data

data class FlightRoute(
    val id: String,
    val origin: String,
    val destination: String,
    val durationMinutes: Int,
    val distanceMiles: Int,
    val originLatitude: Float,
    val originLongitude: Float,
    val destinationLatitude: Float,
    val destinationLongitude: Float,
    val recommendedSeats: List<SeatClass>
) {
    val milesPerMinute: Int get() = (distanceMiles / durationMinutes).coerceAtLeast(60)
}

data class FlightLogEntry(
    val id: String,
    val routeId: String,
    val routeLabel: String,
    val durationMinutes: Int,
    val category: SeatClass,
    val timestamp: Long,
    val completed: Boolean,
    val milesEarned: Int
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val achieved: Boolean
)

enum class SeatClass(val displayName: String, val description: String) {
    ECONOMY("Economy", "Light lift tasks and quick flights."),
    PREMIUM("Premium", "Steady cruising for meaningful progress."),
    BUSINESS("Business", "Deep work with minimal turbulence."),
    FIRST("First Class", "Long haul creative or strategic missions.")
}

object FlightData {
    val routes: List<FlightRoute> = listOf(
        FlightRoute(
            id = "seattle_vancouver",
            origin = "Seattle",
            destination = "Vancouver",
            durationMinutes = 25,
            distanceMiles = 127,
            originLatitude = 47.6062f,
            originLongitude = -122.3321f,
            destinationLatitude = 49.2827f,
            destinationLongitude = -123.1207f,
            recommendedSeats = listOf(SeatClass.ECONOMY, SeatClass.PREMIUM)
        ),
        FlightRoute(
            id = "nyc_boston",
            origin = "New York",
            destination = "Boston",
            durationMinutes = 45,
            distanceMiles = 190,
            originLatitude = 40.7128f,
            originLongitude = -74.0060f,
            destinationLatitude = 42.3601f,
            destinationLongitude = -71.0589f,
            recommendedSeats = listOf(SeatClass.ECONOMY, SeatClass.PREMIUM)
        ),
        FlightRoute(
            id = "la_denver",
            origin = "Los Angeles",
            destination = "Denver",
            durationMinutes = 90,
            distanceMiles = 830,
            originLatitude = 34.0522f,
            originLongitude = -118.2437f,
            destinationLatitude = 39.7392f,
            destinationLongitude = -104.9903f,
            recommendedSeats = listOf(SeatClass.PREMIUM, SeatClass.BUSINESS)
        ),
        FlightRoute(
            id = "london_rome",
            origin = "London",
            destination = "Rome",
            durationMinutes = 120,
            distanceMiles = 888,
            originLatitude = 51.5074f,
            originLongitude = -0.1278f,
            destinationLatitude = 41.9028f,
            destinationLongitude = 12.4964f,
            recommendedSeats = listOf(SeatClass.BUSINESS, SeatClass.FIRST)
        ),
        FlightRoute(
            id = "tokyo_singapore",
            origin = "Tokyo",
            destination = "Singapore",
            durationMinutes = 180,
            distanceMiles = 3320,
            originLatitude = 35.6762f,
            originLongitude = 139.6503f,
            destinationLatitude = 1.3521f,
            destinationLongitude = 103.8198f,
            recommendedSeats = listOf(SeatClass.BUSINESS, SeatClass.FIRST)
        ),
        FlightRoute(
            id = "sydney_paris",
            origin = "Sydney",
            destination = "Paris",
            durationMinutes = 300,
            distanceMiles = 10562,
            originLatitude = -33.8688f,
            originLongitude = 151.2093f,
            destinationLatitude = 48.8566f,
            destinationLongitude = 2.3522f,
            recommendedSeats = listOf(SeatClass.FIRST)
        )
    )
}
