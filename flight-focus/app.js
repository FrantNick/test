const STORAGE_KEYS = {
  session: 'ff_state',
  history: 'ff_history',
  settings: 'ff_settings'
};

const DEFAULT_SETTINGS = {
  muted: false,
  preferredDurations: [25, 45, 60, 90],
  theme: 'dark'
};

const TAGS = ['Study', 'Work', 'Writing', 'Deep Work', 'Creative'];

const AMBIENT_AUDIO_SRC = 'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAIlYAAESsAAACABAAZGF0YQAAAAA=';

const CITIES = [
  { code: 'SFO', name: 'San Francisco', x: 110, y: 210, tier: 'medium' },
  { code: 'LAX', name: 'Los Angeles', x: 130, y: 260, tier: 'short' },
  { code: 'JFK', name: 'New York', x: 720, y: 190, tier: 'medium' },
  { code: 'CDG', name: 'Paris', x: 820, y: 170, tier: 'long' },
  { code: 'NRT', name: 'Tokyo', x: 930, y: 170, tier: 'long' },
  { code: 'SIN', name: 'Singapore', x: 880, y: 310, tier: 'long' },
  { code: 'SYD', name: 'Sydney', x: 950, y: 420, tier: 'long' },
  { code: 'DEN', name: 'Denver', x: 450, y: 210, tier: 'short' },
  { code: 'SEA', name: 'Seattle', x: 120, y: 150, tier: 'short' },
  { code: 'MEX', name: 'Mexico City', x: 300, y: 320, tier: 'medium' },
  { code: 'GIG', name: 'Rio de Janeiro', x: 470, y: 410, tier: 'long' },
  { code: 'DXB', name: 'Dubai', x: 780, y: 240, tier: 'long' }
];

const DURATION_TO_TIER = duration => {
  if (duration <= 30) return ['short'];
  if (duration <= 60) return ['short', 'medium'];
  return ['medium', 'long'];
};

let settings = loadFromStorage(STORAGE_KEYS.settings, DEFAULT_SETTINGS);
let history = loadFromStorage(STORAGE_KEYS.history, []);
let sessionState = loadFromStorage(STORAGE_KEYS.session, null);

const elements = {
  durationChips: document.getElementById('duration-chips'),
  tagChips: document.getElementById('tag-chips'),
  startButton: document.getElementById('start-button'),
  pauseButton: document.getElementById('pause-button'),
  cancelButton: document.getElementById('cancel-button'),
  muteButton: document.getElementById('mute-button'),
  timerDisplay: document.getElementById('timer-display'),
  routeLabel: document.getElementById('route-label'),
  tagLabel: document.getElementById('tag-label'),
  inflightSection: document.getElementById('inflight-section'),
  planningSection: document.getElementById('planning-section'),
  landingModal: document.getElementById('landing-modal'),
  ticketBody: document.getElementById('ticket-body'),
  saveFlightButton: document.getElementById('save-flight-button'),
  dismissModalButton: document.getElementById('dismiss-modal-button'),
  historyList: document.getElementById('history-list'),
  statMinutes: document.getElementById('stat-minutes'),
  statMiles: document.getElementById('stat-miles'),
  statFlights: document.getElementById('stat-flights'),
  statDestinations: document.getElementById('stat-destinations'),
  achievementBadges: document.getElementById('achievement-badges'),
  filterSelect: document.getElementById('filter-select'),
  exportButton: document.getElementById('export-button'),
  installButton: document.getElementById('install-button'),
  resetButton: document.getElementById('reset-button'),
  originSelect: document.getElementById('origin-select'),
  destinationSelect: document.getElementById('destination-select'),
  customDurationInput: document.getElementById('custom-duration-input'),
  routePath: document.getElementById('route-path'),
  plane: document.getElementById('plane'),
  originDot: document.getElementById('origin-dot'),
  destinationDot: document.getElementById('destination-dot'),
  ambientAudio: document.getElementById('ambient-audio'),
  confetti: document.getElementById('confetti')
};

let beforeInstallPromptEvent = null;
let animationFrame = null;
let lastPersist = 0;
let activeFilter = 'all';
let generatedAudio = null;

function createId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return `ff-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function loadFromStorage(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return structuredClone(fallback);
    const parsed = JSON.parse(raw);
    return parsed;
  } catch (error) {
    console.warn('Unable to parse storage for', key, error);
    return structuredClone(fallback);
  }
}

function saveToStorage(key, value) {
  localStorage.setItem(key, JSON.stringify(value));
}

function structuredClone(value) {
  return JSON.parse(JSON.stringify(value));
}

function init() {
  renderDurationChips();
  renderTagChips();
  populateCitySelects();
  renderHistory();
  renderStats();
  renderAchievements();
  updateMuteButton();

  elements.startButton.addEventListener('click', onStart);
  elements.pauseButton.addEventListener('click', togglePause);
  elements.cancelButton.addEventListener('click', cancelSession);
  elements.muteButton.addEventListener('click', toggleMute);
  elements.saveFlightButton.addEventListener('click', commitFlightToHistory);
  elements.dismissModalButton.addEventListener('click', closeLandingModal);
  elements.filterSelect.addEventListener('change', e => {
    activeFilter = e.target.value;
    renderHistory();
  });
  elements.exportButton.addEventListener('click', exportHistoryCsv);
  elements.resetButton.addEventListener('click', resetData);
  elements.customDurationInput.addEventListener('change', () => {
    selectDuration(Number(elements.customDurationInput.value));
  });
  elements.originSelect.addEventListener('change', () => {
    updateDestinationOptions(getSelectedDuration());
  });

  document.addEventListener('keydown', e => {
    if (elements.inflightSection.hasAttribute('hidden')) return;
    if (e.code === 'Space') {
      e.preventDefault();
      togglePause();
    }
  });

  window.addEventListener('beforeinstallprompt', event => {
    event.preventDefault();
    beforeInstallPromptEvent = event;
    elements.installButton.hidden = false;
  });

  elements.installButton.addEventListener('click', async () => {
    if (!beforeInstallPromptEvent) return;
    beforeInstallPromptEvent.prompt();
    const { outcome } = await beforeInstallPromptEvent.userChoice;
    console.log('Install prompt outcome', outcome);
    beforeInstallPromptEvent = null;
    elements.installButton.hidden = true;
  });

  elements.ambientAudio.src = AMBIENT_AUDIO_SRC;
  elements.ambientAudio.volume = 0.18;
  elements.ambientAudio.loop = true;
  elements.ambientAudio.addEventListener('error', setupGeneratedAmbient);

  if (sessionState && sessionState.status === 'IN_FLIGHT') {
    resumeFromStoredState();
  } else if (sessionState && sessionState.status === 'LANDED') {
    elements.planningSection.removeAttribute('hidden');
    elements.inflightSection.setAttribute('hidden', '');
    showLandingModal();
  } else {
    sessionState = null;
    saveToStorage(STORAGE_KEYS.session, null);
  }
}

function renderDurationChips() {
  const durations = (settings.preferredDurations.length ? settings.preferredDurations : DEFAULT_SETTINGS.preferredDurations)
    .slice()
    .sort((a, b) => a - b);
  elements.durationChips.innerHTML = '';
  durations.forEach((duration, index) => {
    const chip = document.createElement('button');
    chip.className = 'chip';
    chip.setAttribute('role', 'radio');
    chip.setAttribute('aria-checked', index === 0 ? 'true' : 'false');
    chip.dataset.value = duration;
    chip.textContent = `${duration}m`;
    chip.addEventListener('click', () => {
      selectDuration(duration);
    });
    elements.durationChips.appendChild(chip);
  });
  selectDuration(durations[0]);
}

function renderTagChips() {
  elements.tagChips.innerHTML = '';
  TAGS.forEach((tag, index) => {
    const chip = document.createElement('button');
    chip.className = 'chip';
    chip.setAttribute('role', 'radio');
    chip.dataset.value = tag;
    chip.textContent = tag;
    chip.setAttribute('aria-checked', index === 0 ? 'true' : 'false');
    chip.addEventListener('click', () => selectTag(tag));
    elements.tagChips.appendChild(chip);
  });
}

function populateCitySelects() {
  elements.originSelect.innerHTML = '';
  elements.destinationSelect.innerHTML = '';
  CITIES.forEach(city => {
    const option = new Option(`${city.name} (${city.code})`, city.code);
    elements.originSelect.add(option.cloneNode(true));
    elements.destinationSelect.add(new Option(`${city.name} (${city.code})`, city.code));
  });
  elements.originSelect.value = 'SFO';
  elements.destinationSelect.value = 'JFK';
  updateDestinationOptions(getSelectedDuration());
}

function selectDuration(duration) {
  if (!duration) return;
  const chips = elements.durationChips.querySelectorAll('.chip');
  chips.forEach(chip => {
    const isSelected = Number(chip.dataset.value) === Number(duration);
    chip.setAttribute('aria-checked', isSelected ? 'true' : 'false');
  });
  if (elements.customDurationInput.value && Number(elements.customDurationInput.value) !== duration) {
    elements.customDurationInput.value = '';
  }
  updateDestinationOptions(duration);
}

function getSelectedDuration() {
  const selectedChip = elements.durationChips.querySelector('.chip[aria-checked="true"]');
  if (selectedChip) return Number(selectedChip.dataset.value);
  const custom = Number(elements.customDurationInput.value);
  return Number.isFinite(custom) && custom > 0 ? custom : DEFAULT_SETTINGS.preferredDurations[0];
}

function selectTag(tag) {
  elements.tagChips.querySelectorAll('.chip').forEach(chip => {
    const isSelected = chip.dataset.value === tag;
    chip.setAttribute('aria-checked', isSelected ? 'true' : 'false');
  });
}

function getSelectedTag() {
  const selectedChip = elements.tagChips.querySelector('.chip[aria-checked="true"]');
  return selectedChip ? selectedChip.dataset.value : TAGS[0];
}

function updateDestinationOptions(duration) {
  const tiers = DURATION_TO_TIER(duration || getSelectedDuration());
  const originCode = elements.originSelect.value;
  const eligible = CITIES.filter(city => city.code !== originCode && tiers.includes(city.tier));
  const fallback = CITIES.filter(city => city.code !== originCode);
  const destinationValue = elements.destinationSelect.value;
  elements.destinationSelect.innerHTML = '';
  (eligible.length ? eligible : fallback).forEach(city => {
    elements.destinationSelect.add(new Option(`${city.name} (${city.code})`, city.code));
  });
  const validList = eligible.length ? eligible : fallback;
  if (validList.some(city => city.code === destinationValue)) {
    elements.destinationSelect.value = destinationValue;
  }
  if (!elements.destinationSelect.value && validList.length) {
    elements.destinationSelect.value = validList[0].code;
  }
}

function onStart() {
  const durationMinutes = elements.customDurationInput.value ? Number(elements.customDurationInput.value) : getSelectedDuration();
  const origin = CITIES.find(city => city.code === elements.originSelect.value);
  const destination = CITIES.find(city => city.code === elements.destinationSelect.value);
  const tag = getSelectedTag();

  if (!durationMinutes || !origin || !destination) {
    alert('Please select a duration and route.');
    return;
  }
  if (origin.code === destination.code) {
    alert('Choose a different destination from the origin.');
    return;
  }

  const durationMs = durationMinutes * 60 * 1000;

  sessionState = {
    id: createId(),
    status: 'IN_FLIGHT',
    startTimestamp: Date.now(),
    elapsedMs: 0,
    durationMs,
    paused: false,
    origin,
    destination,
    tag,
    muted: settings.muted
  };

  navigator.vibrate?.([25]);
  showInflightUI();
  initializeFlightPath(origin, destination);
  updateTimer(0);
  updatePlanePosition(0);
  playAmbient();
  persistSession();
  startAnimationLoop();
}

function resumeFromStoredState() {
  if (!sessionState) return;
  showInflightUI();
  initializeFlightPath(sessionState.origin, sessionState.destination);
  if (!sessionState.paused) {
    const now = Date.now();
    sessionState.startTimestamp = now - sessionState.elapsedMs;
  }
  updateTimer(sessionState.elapsedMs);
  updatePlanePosition(sessionState.elapsedMs / sessionState.durationMs);
  updateMuteButton();
  if (!sessionState.muted) {
    playAmbient();
  }
  startAnimationLoop();
}

function showInflightUI() {
  elements.planningSection.setAttribute('hidden', '');
  elements.inflightSection.removeAttribute('hidden');
  elements.routeLabel.textContent = `${sessionState.origin.code} → ${sessionState.destination.code}`;
  elements.tagLabel.textContent = sessionState.tag;
  elements.pauseButton.textContent = sessionState.paused ? 'Resume' : 'Pause';
  elements.pauseButton.setAttribute('aria-pressed', sessionState.paused ? 'true' : 'false');
  elements.muteButton.setAttribute('aria-pressed', sessionState.muted ? 'true' : 'false');
}

function initializeFlightPath(origin, destination) {
  const pathD = buildArcPath(origin, destination);
  elements.routePath.setAttribute('d', pathD);
  elements.originDot.setAttribute('cx', origin.x);
  elements.originDot.setAttribute('cy', origin.y);
  elements.destinationDot.setAttribute('cx', destination.x);
  elements.destinationDot.setAttribute('cy', destination.y);
  elements.plane.setAttribute('transform', `translate(${origin.x}, ${origin.y})`);
}

function buildArcPath(from, to) {
  const midX = (from.x + to.x) / 2;
  const midY = Math.min(from.y, to.y) - 120;
  const curvature = Math.max(50, Math.abs(from.x - to.x) / 2);
  const controlX = midX;
  const controlY = midY - curvature;
  return `M ${from.x} ${from.y} Q ${controlX} ${controlY} ${to.x} ${to.y}`;
}

function startAnimationLoop() {
  if (animationFrame) cancelAnimationFrame(animationFrame);
  const step = () => {
    if (!sessionState || sessionState.status !== 'IN_FLIGHT') return;
    const now = Date.now();
    if (!sessionState.paused) {
      const elapsed = now - sessionState.startTimestamp;
      sessionState.elapsedMs = Math.min(sessionState.durationMs, elapsed);
      updateTimer(sessionState.elapsedMs);
      updatePlanePosition(sessionState.elapsedMs / sessionState.durationMs);
      if (sessionState.elapsedMs >= sessionState.durationMs) {
        completeSession();
        return;
      }
    }
    if (now - lastPersist > 1000) {
      persistSession();
    }
    animationFrame = requestAnimationFrame(step);
  };
  animationFrame = requestAnimationFrame(step);
}

function updateTimer(elapsedMs) {
  const remainingMs = Math.max(sessionState.durationMs - elapsedMs, 0);
  const totalSeconds = Math.round(remainingMs / 1000);
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0');
  const seconds = String(totalSeconds % 60).padStart(2, '0');
  elements.timerDisplay.textContent = `${minutes}:${seconds}`;
}

function updatePlanePosition(progress) {
  const path = elements.routePath;
  if (!path || !path.getTotalLength) return;
  const clamped = Math.max(0, Math.min(progress, 1));
  const length = path.getTotalLength();
  const point = path.getPointAtLength(length * clamped);
  const prevPoint = path.getPointAtLength(Math.max(0, length * (clamped - 0.01)));
  const angle = Math.atan2(point.y - prevPoint.y, point.x - prevPoint.x) * (180 / Math.PI);
  elements.plane.setAttribute('transform', `translate(${point.x}, ${point.y}) rotate(${angle})`);
}

function togglePause() {
  if (!sessionState || sessionState.status !== 'IN_FLIGHT') return;
  sessionState.paused = !sessionState.paused;
  if (sessionState.paused) {
    sessionState.elapsedMs = Math.min(sessionState.elapsedMs, sessionState.durationMs);
    persistSession();
    elements.pauseButton.textContent = 'Resume';
    elements.pauseButton.setAttribute('aria-pressed', 'true');
    pauseAmbient();
  } else {
    sessionState.startTimestamp = Date.now() - sessionState.elapsedMs;
    persistSession();
    elements.pauseButton.textContent = 'Pause';
    elements.pauseButton.setAttribute('aria-pressed', 'false');
    playAmbient();
  }
}

function cancelSession() {
  if (!sessionState) return;
  if (!confirm('Abort this focus flight? Progress will be lost.')) return;
  stopAmbient();
  resetSessionState();
}

function completeSession() {
  cancelAnimationFrame(animationFrame);
  animationFrame = null;
  elements.timerDisplay.textContent = '00:00';
  stopAmbient();
  navigator.vibrate?.([25]);
  sessionState.status = 'LANDED';
  sessionState.elapsedMs = sessionState.durationMs;
  sessionState.completedAt = new Date().toISOString();
  showLandingModal();
  persistSession();
}

function persistSession() {
  saveToStorage(STORAGE_KEYS.session, sessionState);
  lastPersist = Date.now();
}

function resetSessionState() {
  cancelAnimationFrame(animationFrame);
  animationFrame = null;
  sessionState = null;
  saveToStorage(STORAGE_KEYS.session, null);
  elements.inflightSection.setAttribute('hidden', '');
  elements.planningSection.removeAttribute('hidden');
  elements.timerDisplay.textContent = '00:00';
}

function showLandingModal() {
  elements.ticketBody.innerHTML = '';
  const ticketCard = document.createElement('div');
  ticketCard.className = 'ticket-card';
  const departure = `${sessionState.origin.name} (${sessionState.origin.code})`;
  const arrival = `${sessionState.destination.name} (${sessionState.destination.code})`;
  const durationMinutes = Math.round(sessionState.durationMs / 60000);
  const timestamp = new Date(sessionState.completedAt).toLocaleString();

  ticketCard.innerHTML = `
    <div class="ticket-route">
      <span>${sessionState.origin.code}</span>
      <span>✈</span>
      <span>${sessionState.destination.code}</span>
    </div>
    <div>${departure} → ${arrival}</div>
    <div class="ticket-meta">
      <span>${durationMinutes} minutes</span>
      <span>${timestamp}</span>
    </div>
    <div class="ticket-meta">
      <span>Category: ${sessionState.tag}</span>
      <span>Miles: ${durationMinutes * 100}</span>
    </div>
  `;
  elements.ticketBody.appendChild(ticketCard);
  generateConfetti();
  elements.landingModal.removeAttribute('hidden');
}

function closeLandingModal() {
  elements.landingModal.setAttribute('hidden', '');
  elements.confetti.innerHTML = '';
  resetSessionState();
}

function commitFlightToHistory() {
  if (!sessionState || !sessionState.completedAt) return;
  const durationMinutes = Math.round(sessionState.durationMs / 60000);
  history.push({
    id: sessionState.id,
    dateISO: sessionState.completedAt,
    minutes: durationMinutes,
    origin: sessionState.origin.code,
    destination: sessionState.destination.code,
    tag: sessionState.tag,
    miles: durationMinutes * 100
  });
  saveToStorage(STORAGE_KEYS.history, history);
  renderHistory();
  renderStats();
  renderAchievements();
  closeLandingModal();
}

function renderHistory() {
  elements.historyList.innerHTML = '';
  updateFilterOptions();
  const filtered = history
    .filter(entry => (activeFilter === 'all' ? true : entry.tag === activeFilter))
    .sort((a, b) => new Date(b.dateISO) - new Date(a.dateISO));
  if (!filtered.length) {
    const empty = document.createElement('li');
    empty.textContent = 'No flights logged yet.';
    empty.className = 'history-item';
    elements.historyList.appendChild(empty);
    return;
  }
  filtered.forEach(entry => {
    const li = document.createElement('li');
    li.className = 'history-item';
    const left = document.createElement('span');
    left.innerHTML = `<strong>${entry.origin} → ${entry.destination}</strong><span class="history-meta">${new Date(entry.dateISO).toLocaleString()}</span>`;
    const right = document.createElement('span');
    right.innerHTML = `${entry.minutes} min<br/><span class="history-meta">${entry.tag}</span>`;
    li.append(left, right);
    elements.historyList.appendChild(li);
  });
}

function renderStats() {
  const totalMinutes = history.reduce((sum, item) => sum + item.minutes, 0);
  const uniqueDestinations = new Set(history.map(item => item.destination)).size;
  elements.statMinutes.textContent = totalMinutes;
  elements.statMiles.textContent = totalMinutes * 100;
  elements.statFlights.textContent = history.length;
  elements.statDestinations.textContent = uniqueDestinations;
}

function renderAchievements() {
  elements.achievementBadges.innerHTML = '';
  const totalMinutes = history.reduce((sum, item) => sum + item.minutes, 0);
  const uniqueDestinations = new Set(history.map(item => item.destination)).size;
  const achievements = [];
  if (history.length >= 5) achievements.push('Frequent Flyer — 5 flights logged');
  if (totalMinutes >= 300) achievements.push('Long Haul — 300 minutes focused');
  if (uniqueDestinations >= 5) achievements.push('World Explorer — 5 destinations reached');
  achievements.forEach(text => {
    const badge = document.createElement('span');
    badge.className = 'badge';
    badge.textContent = text;
    elements.achievementBadges.appendChild(badge);
  });
}

function updateFilterOptions() {
  const uniqueTags = Array.from(new Set(history.map(item => item.tag)));
  const current = elements.filterSelect.value;
  elements.filterSelect.innerHTML = '<option value="all">All</option>';
  uniqueTags.forEach(tag => {
    const option = new Option(tag, tag, false, tag === current);
    elements.filterSelect.add(option);
  });
  if (!uniqueTags.includes(current)) {
    elements.filterSelect.value = 'all';
  }
  activeFilter = elements.filterSelect.value;
}

function generateConfetti() {
  elements.confetti.innerHTML = '';
  const colors = ['var(--accent)', '#ffe185', '#ff9ecd'];
  for (let i = 0; i < 24; i++) {
    const span = document.createElement('span');
    const left = Math.random() * 100;
    const delay = Math.random() * 0.4;
    span.style.left = `${left}%`;
    span.style.top = `${Math.random() * 20}%`;
    span.style.background = `linear-gradient(180deg, ${colors[i % colors.length]}, rgba(255,255,255,0.8))`;
    span.style.animationDelay = `${delay}s`;
    elements.confetti.appendChild(span);
  }
}

function exportHistoryCsv() {
  if (!history.length) {
    alert('No flights to export yet.');
    return;
  }
  const header = 'Date,Origin,Destination,Minutes,Category,Miles';
  const rows = history
    .map(entry => {
      const date = new Date(entry.dateISO).toLocaleString();
      return [date, entry.origin, entry.destination, entry.minutes, entry.tag, entry.miles]
        .map(value => `"${String(value).replace(/"/g, '""')}"`)
        .join(',');
    })
    .join('\n');
  const csv = `${header}\n${rows}`;
  const blob = new Blob([csv], { type: 'text/csv' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = 'flight-focus-history.csv';
  anchor.click();
  URL.revokeObjectURL(url);
}

function toggleMute() {
  settings.muted = !settings.muted;
  sessionState && (sessionState.muted = settings.muted);
  updateMuteButton();
  saveToStorage(STORAGE_KEYS.settings, settings);
  if (sessionState) {
    persistSession();
  }
  if (settings.muted) {
    pauseAmbient();
  } else if (sessionState && sessionState.status === 'IN_FLIGHT' && !sessionState.paused) {
    playAmbient();
  }
}

function updateMuteButton() {
  elements.muteButton.setAttribute('aria-pressed', settings.muted ? 'true' : 'false');
  elements.muteButton.textContent = settings.muted ? 'Unmute' : 'Mute';
}

function playAmbient() {
  if (settings.muted) return;
  if (generatedAudio) {
    generatedAudio.play();
    return;
  }
  const audio = elements.ambientAudio;
  audio.play().catch(() => {
    setupGeneratedAmbient();
    generatedAudio?.play();
  });
}

function pauseAmbient() {
  if (generatedAudio) {
    generatedAudio.pause();
    return;
  }
  elements.ambientAudio.pause();
}

function stopAmbient() {
  if (generatedAudio) {
    generatedAudio.stop();
    generatedAudio = null;
  }
  elements.ambientAudio.pause();
  elements.ambientAudio.currentTime = 0;
}

function setupGeneratedAmbient() {
  if (generatedAudio) return;
  const AudioContext = window.AudioContext || window.webkitAudioContext;
  if (!AudioContext) return;
  const context = new AudioContext();
  const bufferSize = 2 * context.sampleRate;
  const buffer = context.createBuffer(1, bufferSize, context.sampleRate);
  const data = buffer.getChannelData(0);
  for (let i = 0; i < bufferSize; i++) {
    data[i] = (Math.random() * 2 - 1) * 0.02;
  }
  const source = context.createBufferSource();
  source.buffer = buffer;
  source.loop = true;
  const gain = context.createGain();
  gain.gain.value = 0.3;
  source.connect(gain).connect(context.destination);
  source.start(0);
  context.suspend();
  generatedAudio = {
    play() {
      if (context.state === 'suspended') context.resume();
    },
    pause() {
      if (context.state === 'running') context.suspend();
    },
    stop() {
      source.stop();
      context.close();
    }
  };
}

function resetData() {
  if (!confirm('This will clear all history and session data. Continue?')) return;
  localStorage.removeItem(STORAGE_KEYS.history);
  localStorage.removeItem(STORAGE_KEYS.session);
  localStorage.removeItem(STORAGE_KEYS.settings);
  stopAmbient();
  settings = structuredClone(DEFAULT_SETTINGS);
  history = [];
  sessionState = null;
  renderHistory();
  renderStats();
  renderAchievements();
  updateMuteButton();
  elements.planningSection.removeAttribute('hidden');
  elements.inflightSection.setAttribute('hidden', '');
}

init();
