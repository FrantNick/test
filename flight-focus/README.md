# Flight Focus

Flight Focus turns a focus session into an immersive "flight" with rich visuals, stats, and achievements. The app is fully client-side (vanilla HTML/CSS/JS), works offline as a PWA, and stores all data in `localStorage`.

## Features
- 🛫 **Flight planning** with duration chips, custom times, and smart route suggestions.
- ✈️ **Animated in-flight view** that moves a plane across a curved SVG path over a stylised world map.
- 🔔 **Ambient cabin audio** powered by an inline base64 clip with automatic fallback synthesised noise.
- 🧾 **Boarding pass landing modal** that lets you save the flight to history.
- 📊 **History, stats & achievements** tracked locally (total minutes, flights, destinations, badges).
- 📱 **Responsive and accessible** design with keyboard support (space = pause/resume).
- 📦 **Offline capable PWA** with service worker precache and install prompt support.

## Getting Started

### Run locally
1. Download or clone this repository.
2. Open `flight-focus/index.html` directly in a modern browser to explore the app offline. Most functionality works without a server thanks to `localStorage`.

### PWA install & offline caching
Service workers require an HTTPS context or `http://localhost`. Start any static file server in the `flight-focus` folder:

```bash
cd flight-focus
python -m http.server 8000
```

Then visit `http://localhost:8000`.
- You should see an **Install** button when the browser fires the `beforeinstallprompt` event.
- Once loaded, the service worker precaches the core assets so the experience continues to work offline.

### Deploying
The project is a static site. Copy the contents of the `flight-focus` folder to your favourite static host (GitHub Pages, Netlify, Vercel, etc.). Ensure `manifest.webmanifest` and `service-worker.js` are served with the correct MIME types.

## Data & Settings
- Current session state: `localStorage['ff_state']`
- Flight history: `localStorage['ff_history']`
- App settings (mute, preferred durations): `localStorage['ff_settings']`

Use the **Reset** button in the top bar to clear all stored data. This is useful during development/testing.

## Customisation
- **Durations & default chips**: edit `DEFAULT_SETTINGS.preferredDurations` near the top of `app.js`.
- **Available cities & coordinates**: adjust the `CITIES` array in `app.js`. Coordinates map to the inline world-map SVG viewBox (`0 0 1000 500`).
- **Ambient sound**: swap the `AMBIENT_AUDIO_SRC` data URI near the top of `app.js` with your own base64 clip. A procedural white-noise fallback kicks in automatically if the element cannot play the provided data URI.

## Development Notes
- No external libraries or build steps are required.
- The timer stores progress every second, so refreshing mid-flight restores the countdown and plane position.
- Achievements unlock after 5 flights, 300 total minutes, and 5 distinct destinations.

## Resetting
If you need to start fresh, click **Reset** in the header or manually clear the keys above from `localStorage`.

---

**Quick start:** open `index.html` to try Flight Focus. For a full PWA experience (install & service worker), run a static server such as `python -m http.server` inside the `flight-focus` directory or deploy it to GitHub Pages. Update the city list and durations inside `app.js` (`CITIES` array and `DEFAULT_SETTINGS.preferredDurations`).
