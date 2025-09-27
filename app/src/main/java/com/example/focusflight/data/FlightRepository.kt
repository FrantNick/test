package com.example.focusflight.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "focus_flight_prefs"
private const val KEY_HISTORY = "history"
private const val KEY_SOUND = "sound_enabled"
private const val KEY_HAPTIC = "haptics_enabled"

class FlightRepository(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHistory(): List<FlightLogEntry> {
        val raw = preferences.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(obj.toEntry())
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addEntry(entry: FlightLogEntry) {
        val history = getHistory().toMutableList()
        history.add(0, entry)
        saveHistory(history)
    }

    private fun saveHistory(history: List<FlightLogEntry>) {
        val array = JSONArray()
        history.forEach { entry ->
            array.put(entry.toJson())
        }
        preferences.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun getSoundEnabled(): Boolean = preferences.getBoolean(KEY_SOUND, true)

    fun setSoundEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun getHapticsEnabled(): Boolean = preferences.getBoolean(KEY_HAPTIC, true)

    fun setHapticsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_HAPTIC, enabled).apply()
    }

    private fun FlightLogEntry.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("routeId", routeId)
        put("routeLabel", routeLabel)
        put("durationMinutes", durationMinutes)
        put("category", category.name)
        put("timestamp", timestamp)
        put("completed", completed)
        put("milesEarned", milesEarned)
    }

    private fun JSONObject.toEntry(): FlightLogEntry = FlightLogEntry(
        id = getString("id"),
        routeId = getString("routeId"),
        routeLabel = getString("routeLabel"),
        durationMinutes = getInt("durationMinutes"),
        category = SeatClass.valueOf(getString("category")),
        timestamp = getLong("timestamp"),
        completed = getBoolean("completed"),
        milesEarned = getInt("milesEarned")
    )
}
