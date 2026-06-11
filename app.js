/* ============================================================
   벌떡 알람 — 잠을 깨워주는 알람 앱
   알람을 끄려면 "깨우기 미션"을 완료해야만 꺼집니다.
   ============================================================ */

const STORAGE_KEY = "beolddeok-alarms-v1";

/** @typedef {Object} Alarm
 *  id, time("HH:MM"), label, days[0-6], enabled, mission, missionCount,
 *  vibrate, snooze, lastFired(date string), snoozeUntil(ms|null) */

let alarms = loadAlarms();
let editingId = null;          // 현재 편집 중인 알람 id (null=신규)
let activeAlarm = null;        // 현재 울리는 알람
let audioCtx = null;
let beepTimer = null;
let vibrateTimer = null;
let wakeLock = null;

/* ---------- 저장/불러오기 ---------- */
function loadAlarms() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) || [];
  } catch {
    return [];
  }
}
function saveAlarms() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(alarms));
}

/* ---------- 시계 표시 ---------- */
function pad(n) { return String(n).padStart(2, "0"); }

function updateClock() {
  const now = new Date();
  document.getElementById("liveClock").textContent =
    pad(now.getHours()) + ":" + pad(now.getMinutes());
  const days = ["일", "월", "화", "수", "목", "금", "토"];
  document.getElementById("todayDate").textContent =
    `${now.getMonth() + 1}월 ${now.getDate()}일 (${days[now.getDay()]})`;
}

/* ---------- 알람 목록 렌더 ---------- */
function renderAlarms() {
  const list = document.getElementById("alarmList");
  const empty = document.getElementById("emptyHint");
  list.innerHTML = "";

  // 시간순 정렬
  const sorted = [...alarms].sort((a, b) => a.time.localeCompare(b.time));
  empty.classList.toggle("hidden", sorted.length > 0);

  const dayNames = ["일", "월", "화", "수", "목", "금", "토"];
  const missionNames = { math: "수학", typing: "받아쓰기", tap: "버튼", none: "미션없음" };

  for (const a of sorted) {
    const li = document.createElement("li");
    li.className = "alarm-item" + (a.enabled ? "" : " off");

    const main = document.createElement("div");
    main.className = "alarm-main";
    let repeatText;
    if (a.days.length === 0) repeatText = "한 번만";
    else if (a.days.length === 7) repeatText = "매일";
    else if (a.days.length === 5 && [1,2,3,4,5].every(d => a.days.includes(d))) repeatText = "주중";
    else if (a.days.length === 2 && a.days.includes(0) && a.days.includes(6)) repeatText = "주말";
    else repeatText = a.days.slice().sort().map(d => dayNames[d]).join(" ");

    main.innerHTML = `
      <div class="alarm-time">${a.time}</div>
      <div class="alarm-meta">
        <span>${a.label || "알람"}</span>
        <span>· ${repeatText}</span>
        <span class="alarm-badge">${missionNames[a.mission]}${a.mission !== "none" ? " ×" + a.missionCount : ""}</span>
      </div>`;
    main.onclick = () => openEdit(a.id);

    const sw = document.createElement("label");
    sw.className = "switch";
    sw.innerHTML = `<input type="checkbox" ${a.enabled ? "checked" : ""}/><span class="slider"></span>`;
    sw.querySelector("input").onchange = (e) => {
      a.enabled = e.target.checked;
      saveAlarms();
      renderAlarms();
    };

    li.appendChild(main);
    li.appendChild(sw);
    list.appendChild(li);
  }
}

/* ---------- 알람 편집 모달 ---------- */
function openEdit(id) {
  editingId = id;
  const modal = document.getElementById("editModal");
  const a = alarms.find(x => x.id === id);

  document.getElementById("editTitle").textContent = a ? "알람 편집" : "새 알람";
  document.getElementById("alarmTime").value = a ? a.time : defaultTime();
  document.getElementById("alarmLabel").value = a ? a.label : "";
  document.getElementById("missionType").value = a ? a.mission : "math";
  document.getElementById("missionCount").value = a ? a.missionCount : 3;
  document.getElementById("vibrateToggle").checked = a ? a.vibrate : true;
  document.getElementById("snoozeToggle").checked = a ? a.snooze : false;

  const selectedDays = a ? a.days : [];
  document.querySelectorAll("#dayPicker button").forEach(btn => {
    const d = Number(btn.dataset.day);
    btn.classList.toggle("on", selectedDays.includes(d));
  });

  document.getElementById("deleteAlarmBtn").style.visibility = a ? "visible" : "hidden";
  modal.classList.remove("hidden");
}

function defaultTime() {
  const t = new Date(Date.now() + 60 * 60 * 1000); // 1시간 뒤
  return pad(t.getHours()) + ":" + pad(t.getMinutes());
}

function closeEdit() {
  document.getElementById("editModal").classList.add("hidden");
  editingId = null;
}

function saveFromModal() {
  const days = [];
  document.querySelectorAll("#dayPicker button.on").forEach(btn => days.push(Number(btn.dataset.day)));

  const data = {
    time: document.getElementById("alarmTime").value,
    label: document.getElementById("alarmLabel").value.trim(),
    days,
    mission: document.getElementById("missionType").value,
    missionCount: Number(document.getElementById("missionCount").value),
    vibrate: document.getElementById("vibrateToggle").checked,
    snooze: document.getElementById("snoozeToggle").checked,
    enabled: true,
    snoozeUntil: null,
    lastFired: null,
  };

  if (editingId) {
    const a = alarms.find(x => x.id === editingId);
    Object.assign(a, data);
  } else {
    data.id = "a" + Date.now() + Math.random().toString(36).slice(2, 6);
    alarms.push(data);
  }
  saveAlarms();
  renderAlarms();
  closeEdit();
  requestNotifyPermission();
}

function deleteCurrent() {
  if (!editingId) return closeEdit();
  alarms = alarms.filter(x => x.id !== editingId);
  saveAlarms();
  renderAlarms();
  closeEdit();
}

/* ---------- 알람 발동 검사 (매초) ---------- */
function checkAlarms() {
  if (activeAlarm) return; // 이미 울리는 중

  const now = new Date();
  const hhmm = pad(now.getHours()) + ":" + pad(now.getMinutes());
  const todayKey = now.toDateString();
  const nowMs = now.getTime();

  for (const a of alarms) {
    // 스누즈 대기 중인 알람
    if (a.snoozeUntil && nowMs >= a.snoozeUntil) {
      a.snoozeUntil = null;
      saveAlarms();
      triggerAlarm(a);
      return;
    }
    if (!a.enabled) continue;
    if (a.snoozeUntil) continue; // 아직 스누즈 시간 안 됨

    if (a.time === hhmm && now.getSeconds() === 0) {
      // 요일 조건
      if (a.days.length > 0 && !a.days.includes(now.getDay())) continue;
      // 같은 분에 중복 발동 방지
      if (a.lastFired === todayKey + hhmm) continue;
      a.lastFired = todayKey + hhmm;
      saveAlarms();
      triggerAlarm(a);
      return;
    }
  }
}

/* ---------- 알람 울리기 ---------- */
function triggerAlarm(a) {
  activeAlarm = a;
  a._snoozeUsed = a._snoozeUsed || 0;

  document.getElementById("ringTime").textContent = a.time;
  document.getElementById("ringLabel").textContent = a.label || "알람";

  const ring = document.getElementById("ringScreen");
  ring.classList.remove("hidden");
  ring.classList.add("flash");

  // 스누즈 버튼
  const snoozeBtn = document.getElementById("snoozeBtn");
  snoozeBtn.classList.toggle("hidden", !(a.snooze && a._snoozeUsed < 1));

  startSound();
  startVibration(a);
  requestWakeLock();
  showNotification(a);
  buildMission(a);
}

/* ---------- 소리 (Web Audio, 파일 불필요) ---------- */
function startSound() {
  stopSound();
  try {
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
  } catch { return; }
  if (audioCtx.state === "suspended") audioCtx.resume();

  let high = false;
  const beep = () => {
    if (!audioCtx) return;
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.type = "square";
    osc.frequency.value = high ? 988 : 784; // 삐-뽀 교대음
    high = !high;
    gain.gain.setValueAtTime(0.0001, audioCtx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.4, audioCtx.currentTime + 0.02);
    gain.gain.exponentialRampToValueAtTime(0.0001, audioCtx.currentTime + 0.35);
    osc.connect(gain).connect(audioCtx.destination);
    osc.start();
    osc.stop(audioCtx.currentTime + 0.4);
  };
  beep();
  beepTimer = setInterval(beep, 600);
}
function stopSound() {
  if (beepTimer) clearInterval(beepTimer);
  beepTimer = null;
  if (audioCtx) { audioCtx.close().catch(() => {}); audioCtx = null; }
}

/* ---------- 진동 ---------- */
function startVibration(a) {
  if (!a.vibrate || !("vibrate" in navigator)) return;
  const buzz = () => navigator.vibrate([500, 300, 500, 300, 800]);
  buzz();
  vibrateTimer = setInterval(buzz, 2600);
}
function stopVibration() {
  if (vibrateTimer) clearInterval(vibrateTimer);
  vibrateTimer = null;
  if ("vibrate" in navigator) navigator.vibrate(0);
}

/* ---------- 화면 켜짐 유지 ---------- */
async function requestWakeLock() {
  try {
    if ("wakeLock" in navigator) wakeLock = await navigator.wakeLock.request("screen");
  } catch {}
}
function releaseWakeLock() {
  if (wakeLock) { wakeLock.release().catch(() => {}); wakeLock = null; }
}

/* ---------- 알림 ---------- */
function requestNotifyPermission() {
  if ("Notification" in window && Notification.permission === "default") {
    Notification.requestPermission().then(updatePermHint);
  }
}
function showNotification(a) {
  if ("Notification" in window && Notification.permission === "granted") {
    try { new Notification("⏰ " + (a.label || "알람"), { body: "일어나세요! 미션을 풀어야 꺼집니다.", requireInteraction: true }); } catch {}
  }
}
function updatePermHint() {
  const el = document.getElementById("permHint");
  if (!("Notification" in window)) { el.textContent = ""; return; }
  if (Notification.permission === "granted") el.textContent = "🔔 알림이 켜져 있어요.";
  else el.textContent = "🔕 더 확실히 깨려면 알림을 허용하세요 (탭하여 허용).";
}

/* ---------- 미션 (알람을 끄려면 반드시 완료) ---------- */
function buildMission(a) {
  const area = document.getElementById("missionArea");
  area.innerHTML = "";
  if (a.mission === "none") {
    const btn = document.createElement("button");
    btn.className = "mission-submit";
    btn.textContent = "알람 끄기";
    btn.onclick = () => dismissAlarm(a);
    area.appendChild(btn);
    return;
  }
  let solved = 0;
  const total = a.missionCount;

  const progress = document.createElement("div");
  progress.className = "mission-progress";
  area.appendChild(progress);

  const updateProgress = () => { progress.textContent = `진행: ${solved} / ${total}`; };
  updateProgress();

  const advance = () => {
    solved++;
    updateProgress();
    if (solved >= total) { dismissAlarm(a); return; }
    renderRound();
  };

  function renderRound() {
    // 기존 라운드 영역 비우기 (progress 다음만)
    [...area.children].slice(1).forEach(c => c.remove());
    if (a.mission === "math") renderMath(area, advance);
    else if (a.mission === "typing") renderTyping(area, advance);
    else if (a.mission === "tap") renderTap(area, advance);
  }
  renderRound();
}

/* 수학 문제 */
function renderMath(area, onSolved) {
  const x = Math.floor(Math.random() * 8) + 2;       // 2~9
  const y = Math.floor(Math.random() * 8) + 2;       // 2~9
  const ops = [["+", x + y], ["−", x - y], ["×", x * y]];
  const [op, ans] = ops[Math.floor(Math.random() * ops.length)];

  const q = document.createElement("div");
  q.className = "mission-q";
  q.textContent = `${x} ${op} ${y} = ?`;

  const input = document.createElement("input");
  input.className = "mission-input";
  input.type = "number";
  input.inputMode = "numeric";
  input.placeholder = "정답 입력";

  const submit = document.createElement("button");
  submit.className = "mission-submit";
  submit.textContent = "확인";

  const check = () => {
    if (Number(input.value) === ans) { input.value = ""; onSolved(); }
    else { input.classList.add("shake"); input.value = ""; setTimeout(() => input.classList.remove("shake"), 300); }
  };
  submit.onclick = check;
  input.onkeydown = (e) => { if (e.key === "Enter") check(); };

  area.append(q, input, submit);
  input.focus();
}

/* 문장 따라 입력 */
const PHRASES = [
  "오늘도 좋은 하루를 시작합니다",
  "나는 지금 당장 일어난다",
  "더 자면 지각이다 일어나자",
  "아침 공기가 상쾌하다",
  "벌떡 일어나서 물 한 잔 마시자",
];
function renderTyping(area, onSolved) {
  const target = PHRASES[Math.floor(Math.random() * PHRASES.length)];

  const t = document.createElement("div");
  t.className = "typing-target";
  t.textContent = target;

  const input = document.createElement("input");
  input.className = "mission-input";
  input.type = "text";
  input.autocomplete = "off";
  input.placeholder = "위 문장을 그대로 입력";

  const submit = document.createElement("button");
  submit.className = "mission-submit";
  submit.textContent = "확인";

  const check = () => {
    if (input.value.trim() === target) { input.value = ""; onSolved(); }
    else { input.classList.add("shake"); setTimeout(() => input.classList.remove("shake"), 300); }
  };
  submit.onclick = check;
  input.onkeydown = (e) => { if (e.key === "Enter") check(); };

  area.append(t, input, submit);
  input.focus();
}

/* 순서대로 버튼 누르기 */
function renderTap(area, onSolved) {
  const order = [1, 2, 3, 4, 5, 6, 7, 8, 9].sort(() => Math.random() - 0.5);
  let next = 0;

  const hint = document.createElement("div");
  hint.className = "tap-hint";
  hint.textContent = "1부터 9까지 순서대로 누르세요";

  const grid = document.createElement("div");
  grid.className = "tap-grid";

  order.forEach(n => {
    const b = document.createElement("button");
    b.className = "tap-btn";
    b.textContent = n;
    b.onclick = () => {
      if (n === next + 1) {
        next++;
        b.classList.add("done");
        b.disabled = true;
        if (next === 9) onSolved();
      } else {
        // 틀리면 처음부터
        next = 0;
        grid.querySelectorAll(".tap-btn").forEach(x => { x.classList.remove("done"); x.disabled = false; });
        hint.textContent = "❌ 순서가 틀렸어요! 1부터 다시.";
      }
    };
    grid.appendChild(b);
  });

  area.append(hint, grid);
}

/* ---------- 스누즈 / 해제 ---------- */
function snoozeAlarm() {
  const a = activeAlarm;
  if (!a) return;
  a._snoozeUsed = (a._snoozeUsed || 0) + 1;
  a.snoozeUntil = Date.now() + 5 * 60 * 1000;
  saveAlarms();
  stopRinging();
}

function dismissAlarm(a) {
  if (a) {
    a.snoozeUntil = null;
    a._snoozeUsed = 0;
    // 한 번만 울리는 알람(반복 없음)은 끈다
    if (a.days.length === 0) a.enabled = false;
    saveAlarms();
    renderAlarms();
  }
  stopRinging();
}

function stopRinging() {
  stopSound();
  stopVibration();
  releaseWakeLock();
  const ring = document.getElementById("ringScreen");
  ring.classList.add("hidden");
  ring.classList.remove("flash");
  activeAlarm = null;
}

/* ---------- 이벤트 바인딩 ---------- */
function init() {
  updateClock();
  renderAlarms();
  updatePermHint();

  document.getElementById("addAlarmBtn").onclick = () => openEdit(null);
  document.getElementById("cancelEditBtn").onclick = closeEdit;
  document.getElementById("saveAlarmBtn").onclick = saveFromModal;
  document.getElementById("deleteAlarmBtn").onclick = deleteCurrent;
  document.getElementById("snoozeBtn").onclick = snoozeAlarm;

  document.querySelectorAll("#dayPicker button").forEach(btn => {
    btn.onclick = () => btn.classList.toggle("on");
  });

  // 모달 바깥 탭 시 닫기
  document.getElementById("editModal").addEventListener("click", (e) => {
    if (e.target.id === "editModal") closeEdit();
  });

  // 알림 권한 힌트 클릭 시 요청
  document.getElementById("permHint").onclick = requestNotifyPermission;

  // 화면 복귀 시 wakeLock 재획득
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible" && activeAlarm) requestWakeLock();
  });

  // 1초마다 시계 갱신 + 알람 검사
  setInterval(() => { updateClock(); checkAlarms(); }, 1000);

  // 서비스워커 등록 (오프라인/홈화면 추가용)
  if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("sw.js").catch(() => {});
  }
}

document.addEventListener("DOMContentLoaded", init);
