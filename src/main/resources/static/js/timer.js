(function() {
    'use strict';

    let currentMode = 'stopwatch';

    const stopwatchPanel = document.getElementById('stopwatchPanel');
    const countdownPanel = document.getElementById('countdownPanel');
    const modeBtns = document.querySelectorAll('.mode-btn');

    const timerDisplay = document.getElementById('timerDisplay');
    const statusDot = document.querySelector('#stopwatchPanel .status-dot');
    const statusText = document.getElementById('statusText');
    const startBtn = document.getElementById('startBtn');
    const stopBtn = document.getElementById('stopBtn');
    const resetBtn = document.getElementById('resetBtn');
    
    const progressRing = document.getElementById('progressRing');
    const sessionSummary = document.getElementById('sessionSummary');
    const summaryTask = document.getElementById('summaryTask');
    const summaryDuration = document.getElementById('summaryDuration');
    const summaryEndTime = document.getElementById('summaryEndTime');

    const countdownEventInput = document.getElementById('countdownEvent');
    const hoursInput = document.getElementById('countdownHours');
    const minutesInput = document.getElementById('countdownMinutes');
    const secondsInput = document.getElementById('countdownSeconds');
    const countdownDisplay = document.getElementById('countdownDisplay');
    const countdownStatusDot = document.querySelector('#countdownPanel .status-dot');
    const countdownStatusText = document.getElementById('countdownStatusText');
    const countdownStartBtn = document.getElementById('countdownStartBtn');
    const countdownPauseBtn = document.getElementById('countdownPauseBtn');
    const countdownResetBtn = document.getElementById('countdownResetBtn');
    const countdownProgressRing = document.getElementById('countdownProgressRing');
    const targetDurationLabel = document.getElementById('targetDurationLabel');
    const countdownSummary = document.getElementById('countdownSummary');
    const summaryCountdownEvent = document.getElementById('summaryCountdownEvent');
    const summaryTargetDuration = document.getElementById('summaryTargetDuration');
    const summaryActualDuration = document.getElementById('summaryActualDuration');

    const historyList = document.getElementById('historyList');
    const clearHistoryBtn = document.getElementById('clearHistoryBtn');
    const todayTotalSpan = document.getElementById('todayTotal');

    const stopwatchEventInput = document.getElementById('stopwatchEvent');

    // ===== 滚动选择器 =====
    function initScrollPicker(pickerId, hiddenInputId, max) {
        const picker = document.getElementById(pickerId);
        const hiddenInput = document.getElementById(hiddenInputId);
        const wrapper = picker ? picker.parentElement : null;
        if (!picker || !hiddenInput || !wrapper) return;

        // 插入高亮条
        const highlight = document.createElement('div');
        highlight.className = 'scroll-picker-highlight';
        wrapper.appendChild(highlight);

        // 生成选项
        for (let i = 0; i <= max; i++) {
            const opt = document.createElement('div');
            opt.className = 'picker-option';
            opt.textContent = String(i).padStart(2, '0');
            opt.dataset.value = i;
            picker.appendChild(opt);
        }

        const itemHeight = 42;
        const padding = (picker.clientHeight - itemHeight) / 2; // 顶部留白

        function updateSelection() {
            // 用实际 DOM 位置找最接近中心的选项（不依赖像素计算）
            const pickerRect = picker.getBoundingClientRect();
            const centerY = pickerRect.top + pickerRect.height / 2;
            let closestOpt = null;
            let minDist = Infinity;

            picker.querySelectorAll('.picker-option').forEach(opt => {
                const rect = opt.getBoundingClientRect();
                const optCenter = rect.top + rect.height / 2;
                const dist = Math.abs(centerY - optCenter);
                if (dist < minDist) {
                    minDist = dist;
                    closestOpt = opt;
                }
            });

            if (closestOpt) {
                const val = parseInt(closestOpt.dataset.value);
                hiddenInput.value = val;
                // 手动触发 change 事件，确保其他代码能感知到值变化
                hiddenInput.dispatchEvent(new Event('change', { bubbles: true }));
                picker.querySelectorAll('.picker-option').forEach(opt => {
                    const v = parseInt(opt.dataset.value);
                    opt.classList.remove('selected', 'nearby');
                    if (v === val) opt.classList.add('selected');
                    else if (v === val - 1 || v === val + 1) opt.classList.add('nearby');
                });
            }
        }

        // 滚动时更新
        picker.addEventListener('scroll', updateSelection);

        // 初始滚动到默认值
        const initVal = parseInt(hiddenInput.value) || 0;
        // 计算目标 scrollTop：选项0中心在 padding+itemHeight/2, 选项N在 padding + N*itemHeight + itemHeight/2
        // 要使选项N居中：scrollTop = padding + N*itemHeight + itemHeight/2 - clientHeight/2 = N*itemHeight
        picker.scrollTop = initVal * itemHeight;
        // 等渲染完成后矫正
        requestAnimationFrame(() => {
            picker.scrollTop = initVal * itemHeight;
            updateSelection();
        });
    }

    // 初始化三个滚轮
    initScrollPicker('pickerHours', 'countdownHours', 23);
    initScrollPicker('pickerMinutes', 'countdownMinutes', 59);
    initScrollPicker('pickerSeconds', 'countdownSeconds', 59);

    let stopwatchSeconds = 0;
    let stopwatchInterval = null;
    let isStopwatchRunning = false;
    let currentTaskId = null;
    let currentTaskName = '';
    let activeStopwatchLogId = null;

    let countdownTargetSeconds =0;
    let countdownRemainingSeconds =0;
    let countdownInterval = null;
    let isCountdownRunning = false;
    let isCountdownPaused = false;
    let activeCountdownLogId = null;
    let countdownEventDesc = '';

    const CIRCUMFERENCE = 2 * Math.PI * 90;
    const CYCLE_SECONDS = 25 * 60;

    if (progressRing) {
        progressRing.style.strokeDasharray = CIRCUMFERENCE;
        progressRing.style.strokeDashoffset = CIRCUMFERENCE;
    }
    if (countdownProgressRing) {
        countdownProgressRing.style.strokeDasharray = CIRCUMFERENCE;
        countdownProgressRing.style.strokeDashoffset = CIRCUMFERENCE;
    }

    let historyRecords = [];

    function formatTime(totalSeconds) {
        const m = Math.floor(totalSeconds / 60);
        const s = totalSeconds % 60;
        return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }

    function formatCountdownDisplay(secs) {
        const m = Math.floor(secs / 60);
        const s = secs % 60;
        return `${m}:${s.toString().padStart(2, '0')}`;
    }

    function updateStopwatchUI() {
        timerDisplay.textContent = formatTime(stopwatchSeconds);
        const progress = Math.min(stopwatchSeconds / CYCLE_SECONDS, 1);
        const offset = CIRCUMFERENCE * (1 - progress);
        if (progressRing) progressRing.style.strokeDashoffset = offset;
    }

    function updateStopwatchStatus(running) {
        if (running) {
            statusDot.className = 'status-dot active';
            statusText.textContent = 'Running';
        } else {
            statusDot.className = 'status-dot';
            statusText.textContent = stopwatchSeconds > 0 ? 'Stopped' : 'Ready';
        }
    }

    function updateStopwatchButtons(running) {
        startBtn.disabled = running;
        stopBtn.disabled = !running;
        resetBtn.disabled = running;
    }

   

    function stopwatchTick() {
        stopwatchSeconds++;
        updateStopwatchUI();
    }

    async function callStartStopwatchAPI(eventDesc) {
    const resp = await fetch(`/api/timer/start?event=${encodeURIComponent(eventDesc)}`, { method: 'POST' });
    if (!resp.ok) throw new Error(await resp.text());
    return resp.json();
}
    async function callStopStopwatchAPI() {
        const resp = await fetch('/api/timer/stop', { method: 'POST' });
        if (!resp.ok) throw new Error(await resp.text());
        return resp.json();
    }

    
        async function startStopwatch() {
    if (isStopwatchRunning) return;

    // 获取用户输入的事件描述
    const eventDesc = stopwatchEventInput.value.trim();
    if (!eventDesc) {
        alert('请输入事件描述');
        return;
    }

    // 复用 currentTaskName 来保存事件描述，以便摘要和历史记录显示
    currentTaskName = eventDesc;
    try {
        // 调用后端正计时开始接口，传递 event 参数
        const log = await callStartStopwatchAPI(eventDesc);
        activeStopwatchLogId = log.id;
        isStopwatchRunning = true;
        stopwatchInterval = setInterval(stopwatchTick, 1000);
        updateStopwatchStatus(true);
        updateStopwatchButtons(true);
    } catch (e) {
        alert(e.message);
    }
}

    async function stopStopwatch() {
        if (!isStopwatchRunning) return;
        try {
            const log = await callStopStopwatchAPI();
            clearInterval(stopwatchInterval);
            stopwatchInterval = null;
            isStopwatchRunning = false;
            if (log.durationSeconds !== undefined) {
                stopwatchSeconds = log.durationSeconds;
                updateStopwatchUI();
            }
            updateStopwatchStatus(false);
            updateStopwatchButtons(false);
            showStopwatchSummary(log);
            addHistoryItem('stopwatch', {
                taskName: currentTaskName,
                durationSeconds: log.durationSeconds,
                endTime: log.endTime
            });
            activeStopwatchLogId = null;
            await loadHistoryFromBackend();
        } catch (e) {
            alert(e.message);
            if (isStopwatchRunning) {
                clearInterval(stopwatchInterval);
                stopwatchInterval = null;
                isStopwatchRunning = false;
                updateStopwatchStatus(false);
                updateStopwatchButtons(false);
            }
        }
    }

    function resetStopwatch() {
        if (isStopwatchRunning) return;
        stopwatchSeconds = 0;
        updateStopwatchUI();
        updateStopwatchStatus(false);
        sessionSummary.style.display = 'none';
    }

    function showStopwatchSummary(log) {
        summaryTask.textContent = currentTaskName;
        summaryDuration.textContent = formatTime(log.durationSeconds);
        summaryEndTime.textContent = log.endTime ? new Date(log.endTime).toLocaleTimeString() : '';
        sessionSummary.style.display = 'block';
    }

    function updateCountdownUI() {
        countdownDisplay.textContent = formatCountdownDisplay(countdownRemainingSeconds);
        const total = countdownTargetSeconds;
        const progress = total > 0 ? (total - countdownRemainingSeconds) / total : 0;
        const offset = CIRCUMFERENCE * (1 - progress);
        if (countdownProgressRing) countdownProgressRing.style.strokeDashoffset = offset;
    }

    function updateCountdownStatus(running, paused) {
        if (running && !paused) {
            countdownStatusDot.className = 'status-dot active';
            countdownStatusText.textContent = 'Running';
        } else if (running && paused) {
            countdownStatusDot.className = 'status-dot';
            countdownStatusText.textContent = 'Paused';
        } else {
            countdownStatusDot.className = 'status-dot';
            countdownStatusText.textContent = countdownRemainingSeconds > 0 ? 'Stopped' : 'Ready';
        }
    }

function updateCountdownButtons(running, paused) {
    countdownStartBtn.disabled = running && !paused;
    countdownPauseBtn.disabled = !running;
    countdownResetBtn.disabled = running && !paused;  // 仅在运行中且未暂停时禁用
}

    function readCountdownSettings() {
        const h = parseInt(hoursInput.value) || 0;
        const m = parseInt(minutesInput.value) || 0;
        const s = parseInt(secondsInput.value) || 0;
        return h * 3600 + m * 60 + s;
    }

    function applySettingsToCountdown() {
        countdownTargetSeconds = readCountdownSettings();
        countdownRemainingSeconds = countdownTargetSeconds;
        updateCountdownUI();
        const m = Math.floor(countdownTargetSeconds / 60);
        targetDurationLabel.textContent = `${m}m`;
    }

    function countdownTick() {
        if (countdownRemainingSeconds > 0) {
            countdownRemainingSeconds--;
            updateCountdownUI();
            if (countdownRemainingSeconds === 0) {
                completeCountdown();
            }
        }
    }

    async function callStartCountdownAPI(targetSecs, eventDesc) {
        console.log('=== 开始倒计时 ===');
console.log('输入的事件描述:', countdownEventInput.value);
        const url = `/api/countdown/start?targetSeconds=${targetSecs}` + (eventDesc ? `&event=${encodeURIComponent(eventDesc)}` : '');
        console.log('请求 URL:', url);
        const resp = await fetch(url, { method: 'POST' });
        if (!resp.ok) throw new Error(await resp.text());
        return resp.json();
    }

    async function callCompleteCountdownAPI() {
        const resp = await fetch('/api/countdown/complete', { method: 'POST' });
        if (!resp.ok) throw new Error(await resp.text());
        return resp.json();
    }

    async function callCancelCountdownAPI() {
        const resp = await fetch('/api/countdown/cancel', { method: 'POST' });
        if (!resp.ok) throw new Error(await resp.text());
        return resp.json();
    }

  async function startCountdown() {
    if (isCountdownRunning && !isCountdownPaused) return;
    if (isCountdownPaused) {
        isCountdownPaused = false;
        countdownInterval = setInterval(countdownTick, 1000);
        updateCountdownStatus(true, false);
        updateCountdownButtons(true, false);
        return;
    }
    applySettingsToCountdown();
    
    // 读取事件描述
    const eventDesc = countdownEventInput.value.trim();
    countdownEventDesc = eventDesc;
    
    const targetSecs = countdownTargetSeconds;
    
    // 构建 URL（包含 event 参数）
    let url = `/api/countdown/start?targetSeconds=${targetSecs}`;
    if (eventDesc) {
        url += `&event=${encodeURIComponent(eventDesc)}`;
    }
    
    try {
        const resp = await fetch(url, { method: 'POST' });
        if (!resp.ok) throw new Error(await resp.text());
        const log = await resp.json();
        activeCountdownLogId = log.id;
        isCountdownRunning = true;
        isCountdownPaused = false;
        countdownInterval = setInterval(countdownTick, 1000);
        updateCountdownStatus(true, false);
        updateCountdownButtons(true, false);
    } catch (e) {
        alert(e.message);
        console.error(e);
    }
}

   async function pauseCountdown() {
    if (!isCountdownRunning) return;
    // 停止前端计时
    clearInterval(countdownInterval);
    countdownInterval = null;
    isCountdownRunning = false;
    isCountdownPaused = false;

    try {
        // 调用后端取消接口，计算实际用时并保存
        const log = await callCancelCountdownAPI();
        // 更新显示
        countdownRemainingSeconds = 0;
        updateCountdownUI();
        updateCountdownStatus(false, false);
        updateCountdownButtons(false, false);
        // 显示摘要卡片
        showCountdownSummary(log, 'Paused');
        // 加入历史并刷新
        addHistoryItem('countdown', {
            event: countdownEventDesc || log.eventDescription || 'Countdown',
            targetSeconds: log.targetDurationSeconds,
            actualSeconds: log.actualDurationSeconds,
            endTime: log.endTime
        });
        activeCountdownLogId = null;
        await loadHistoryFromBackend();
        // 暂停时用户刚点击，可以播放提示音
        playAlarm();
    } catch (e) {
        alert('保存记录失败: ' + e.message);
        // 即使失败也复位前端
        countdownRemainingSeconds = 0;
        updateCountdownUI();
        updateCountdownStatus(false, false);
        updateCountdownButtons(false, false);
    }
}

    async function completeCountdown() {
        if (countdownInterval) {
            clearInterval(countdownInterval);
            countdownInterval = null;
        }
        isCountdownRunning = false;
        isCountdownPaused = false;
        try {
            const log = await callCompleteCountdownAPI();
            countdownRemainingSeconds = 0;
            updateCountdownUI();
            updateCountdownStatus(false, false);
            updateCountdownButtons(false, false);
            showCountdownSummary(log, 'Completed');
            addHistoryItem('countdown', {
                event: countdownEventDesc || 'Countdown',
                targetSeconds: log.targetDurationSeconds,
                actualSeconds: log.actualDurationSeconds,
                endTime: log.endTime
            });
            activeCountdownLogId = null;
            await loadHistoryFromBackend();
            playAlarm();
        } catch (e) {
            alert(e.message);
        }
    }

    async function cancelCountdown() {
        if (!isCountdownRunning) return;
        clearInterval(countdownInterval);
        countdownInterval = null;
        try {
            const log = await callCancelCountdownAPI();
            isCountdownRunning = false;
            isCountdownPaused = false;
            updateCountdownStatus(false, false);
            updateCountdownButtons(false, false);
            showCountdownSummary(log, 'Cancelled');
            addHistoryItem('countdown', {
                event: countdownEventDesc || 'Countdown',
                targetSeconds: log.targetDurationSeconds,
                actualSeconds: log.actualDurationSeconds,
                endTime: log.endTime
            });
            activeCountdownLogId = null;
            await loadHistoryFromBackend();
        } catch (e) {
            alert(e.message);
        }
    }

   function resetCountdown() {
    // 如果正在运行且未暂停，不允许复位
    if (isCountdownRunning && !isCountdownPaused) return;
    
    // 强制设置为0秒
    countdownTargetSeconds = 0;
    countdownRemainingSeconds = 0;
    updateCountdownUI();
    
    // 同时将输入框也归零（避免下次复位又变回去）
    hoursInput.value = 0;
    minutesInput.value = 0;
    secondsInput.value = 0;
    
    // 更新进度环和状态
    updateCountdownStatus(false, false);
    updateCountdownButtons(false, false);
    countdownSummary.style.display = 'none';
}

    function showCountdownSummary(log, type) {
        summaryCountdownEvent.textContent = log.eventDescription || countdownEventDesc || 'Untitled';
        summaryTargetDuration.textContent = formatTime(log.targetDurationSeconds);
        summaryActualDuration.textContent = formatTime(log.actualDurationSeconds);
        countdownSummary.style.display = 'block';
    }

// ========== 音效系统 ==========
const SOUND_STORAGE_KEY = 'timerSoundPref';
const SAVED_SOUNDS_KEY = 'timerSavedSounds';

function getSoundPref() {
    return localStorage.getItem(SOUND_STORAGE_KEY) || 'beep';
}

function setSoundPref(name) {
    localStorage.setItem(SOUND_STORAGE_KEY, name);
}

function getSavedSounds() {
    try {
        const raw = localStorage.getItem(SAVED_SOUNDS_KEY);
        return raw ? JSON.parse(raw) : [];
    } catch (e) { return []; }
}

function saveSoundsList(list) {
    // 控制总大小：每个音效 dataUrl 限制在 ~800KB，总数不超过 8 个
    const trimmed = list.slice(0, 8);
    localStorage.setItem(SAVED_SOUNDS_KEY, JSON.stringify(trimmed));
}

function findSoundById(id) {
    return getSavedSounds().find(s => s.id === id);
}

// 用 Web Audio API 生成预设音效
function playPresetSound(audioCtx, type) {
    const t = audioCtx.currentTime;
    const masterGain = audioCtx.createGain();
    masterGain.connect(audioCtx.destination);
    masterGain.gain.setValueAtTime(1.2, t);

    if (type === 'beep') {
        // 经典嘟嘟声：三声短促 beep
        for (let i = 0; i < 3; i++) {
            const osc = audioCtx.createOscillator();
            const g = audioCtx.createGain();
            osc.connect(g); g.connect(audioCtx.destination);
            osc.type = 'triangle';
            osc.frequency.value = 660;
            const start = t + i * 0.4;
            g.gain.setValueAtTime(1.5, start);
            g.gain.exponentialRampToValueAtTime(0.01, start + 0.25);
            osc.start(start); osc.stop(start + 0.3);
        }
    } else if (type === 'alarm') {
        // 闹钟铃声：快速交替高低音
        for (let i = 0; i < 8; i++) {
            const osc = audioCtx.createOscillator();
            const g = audioCtx.createGain();
            osc.connect(g); g.connect(audioCtx.destination);
            osc.type = 'square';
            osc.frequency.value = i % 2 === 0 ? 880 : 440;
            const start = t + i * 0.25;
            g.gain.setValueAtTime(0.8, start);
            g.gain.exponentialRampToValueAtTime(0.01, start + 0.2);
            osc.start(start); osc.stop(start + 0.22);
        }
    } else if (type === 'chime') {
        // 风铃渐响：升序音阶
        const notes = [523, 587, 659, 784, 880];
        notes.forEach((freq, i) => {
            const osc = audioCtx.createOscillator();
            const g = audioCtx.createGain();
            osc.connect(g); g.connect(audioCtx.destination);
            osc.type = 'sine';
            osc.frequency.value = freq;
            const start = t + i * 0.2;
            g.gain.setValueAtTime(0.01, start);
            g.gain.exponentialRampToValueAtTime(1.0, start + 0.05);
            g.gain.exponentialRampToValueAtTime(0.01, start + 0.6);
            osc.start(start); osc.stop(start + 0.7);
        });
    } else if (type === 'bell') {
        // 钟声：低沉 + 泛音
        const osc1 = audioCtx.createOscillator();
        const osc2 = audioCtx.createOscillator();
        const g1 = audioCtx.createGain();
        const g2 = audioCtx.createGain();
        osc1.connect(g1); g1.connect(audioCtx.destination);
        osc2.connect(g2); g2.connect(audioCtx.destination);
        osc1.type = 'triangle'; osc1.frequency.value = 330;
        osc2.type = 'sine'; osc2.frequency.value = 660;
        g1.gain.setValueAtTime(1.8, t);
        g1.gain.exponentialRampToValueAtTime(0.01, t + 1.5);
        g2.gain.setValueAtTime(0.6, t);
        g2.gain.exponentialRampToValueAtTime(0.01, t + 1.2);
        osc1.start(t); osc1.stop(t + 1.6);
        osc2.start(t); osc2.stop(t + 1.3);
    } else if (type === 'digital') {
        // 电子提示音：短促上升音
        for (let i = 0; i < 2; i++) {
            const osc = audioCtx.createOscillator();
            const g = audioCtx.createGain();
            osc.connect(g); g.connect(audioCtx.destination);
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(600, t + i * 0.6);
            osc.frequency.linearRampToValueAtTime(1200, t + i * 0.6 + 0.15);
            const start = t + i * 0.6;
            g.gain.setValueAtTime(1.0, start);
            g.gain.exponentialRampToValueAtTime(0.01, start + 0.25);
            osc.start(start); osc.stop(start + 0.3);
        }
    }
}

let customAudioBuffer = null;
let customSoundCache = {}; // { id: AudioBuffer }

function loadCustomSoundBuffer() {
    const pref = getSoundPref();
    if (!pref || pref === 'beep' || pref === 'alarm' || pref === 'chime' || pref === 'bell' || pref === 'digital') {
        customAudioBuffer = null;
        return;
    }
    // pref 是自定义音效的 id
    const sound = findSoundById(pref);
    if (!sound) { customAudioBuffer = null; return; }
    if (customSoundCache[pref]) { customAudioBuffer = customSoundCache[pref]; return; }
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    fetch(sound.dataUrl)
        .then(res => res.arrayBuffer())
        .then(buf => audioCtx.decodeAudioData(buf))
        .then(decoded => {
            customSoundCache[pref] = decoded;
            customAudioBuffer = decoded;
        })
        .catch(() => { customAudioBuffer = null; });
}

function playCustomSound() {
    if (!customAudioBuffer) return;
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const source = audioCtx.createBufferSource();
    const gain = audioCtx.createGain();
    source.buffer = customAudioBuffer;
    source.connect(gain);
    gain.connect(audioCtx.destination);
    gain.gain.setValueAtTime(1.5, audioCtx.currentTime);
    source.start(0);
}

// ========== 音频剪辑编辑器 ==========
let editorAudioBuffer = null;
let editorFileName = '';
let editorPreviewSource = null;
let editorPreviewCtx = null;
let isEditorPreviewPlaying = false;
let editingSoundId = null; // 正在编辑的音效 id（null = 新建）
let pendingEditorMode = false;   // true = 文件选择器是编辑器触发的

function showEditorUploadZone() {
    var uz = document.getElementById('customUploadZone');
    var eb = document.getElementById('customEditorBody');
    var sb = document.getElementById('audioSaveBtn');
    if (uz) uz.style.display = 'block';
    if (eb) eb.style.display = 'none';
    if (sb) sb.disabled = true;
}

// ===== 点击轨道快速跳转 =====
function handleTrimTrackClick(e) {
    // 如果用户正在拖拽手柄，不处理点击
    if (e.target.tagName === 'INPUT') return;

    const wrapper = document.querySelector('.dual-range');
    if (!wrapper) return;
    const rect = wrapper.getBoundingClientRect();
    const pct = Math.round(((e.clientX - rect.left) / rect.width) * 100);
    if (pct < 0 || pct > 100) return;

    const startSlider = document.getElementById('trimStartSlider');
    const endSlider = document.getElementById('trimEndSlider');
    if (!startSlider || !endSlider) return;

    const startVal = parseInt(startSlider.value);
    const endVal = parseInt(endSlider.value);

    // 判断点击位置更接近哪个手柄，就移动哪个
    const distToStart = Math.abs(pct - startVal);
    const distToEnd = Math.abs(pct - endVal);

    if (distToStart <= distToEnd) {
        // 移动起始手柄，但不能超过结束手柄
        startSlider.value = Math.min(pct, endVal - 1);
    } else {
        // 移动结束手柄，但不能低于起始手柄
        endSlider.value = Math.max(pct, startVal + 1);
    }
    onTrimSliderChange();
}

let trimTrackClickBound = false;
function ensureTrimTrackClick() {
    if (trimTrackClickBound) return;
    const track = document.querySelector('.dual-range-track');
    if (track) {
        track.addEventListener('click', handleTrimTrackClick);
        track.style.cursor = 'pointer';
        trimTrackClickBound = true;
    }
}

function showEditorWaveform() {
    var uz = document.getElementById('customUploadZone');
    var eb = document.getElementById('customEditorBody');
    var sb = document.getElementById('audioSaveBtn');
    if (uz) uz.style.display = 'none';
    if (eb) eb.style.display = 'block';
    if (sb) sb.disabled = false;
    // 确保轨道点击快速跳转已绑定
    ensureTrimTrackClick();
}

// 加载文件到编辑器
function loadFileToEditor(file) {
    if (!file.type.startsWith('audio/')) {
        alert('请选择音频文件（mp3、wav、ogg 等）');
        return;
    }
    if (file.size > 50 * 1024 * 1024) {
        alert('文件过大，请选择 50MB 以内的音频文件');
        return;
    }
    const reader = new FileReader();
    reader.onload = function(ev) {
        const arrayBuffer = ev.target.result;
        const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        audioCtx.decodeAudioData(arrayBuffer).then(buffer => {
            editorAudioBuffer = buffer;
            editorFileName = file.name.replace(/\.[^.]+$/, '');
            const nameInput = document.getElementById('audioCustomName');
            if (!nameInput.value.trim()) {
                nameInput.value = editorFileName;
            }
            document.getElementById('trimStartSlider').value = 0;
            document.getElementById('trimEndSlider').value = 100;
            // 初始化填充条
            var fill = document.getElementById('dualRangeFill');
            if (fill) { fill.style.left = '0%'; fill.style.width = '100%'; }
            updateTrimDisplay();
            showEditorWaveform();
            requestAnimationFrame(() => drawWaveform());
        }).catch(() => {
            alert('无法解码该音频文件，请尝试其他格式（MP3、WAV、OGG）');
        });
    };
    reader.readAsArrayBuffer(file);
}

// 拖拽文件到编辑器
function handleEditorDrop(event) {
    const file = event.dataTransfer.files[0];
    if (file) {
        editingSoundId = null;
        document.getElementById('audioCustomName').value = '';
        stopTrimPreview();
        loadFileToEditor(file);
    }
}

// 打开自定义音效面板（从下拉框选择「自定义添加」或编辑已有音效）
function openCustomPanel() {
    editingSoundId = null;
    var nameInput = document.getElementById('audioCustomName');
    if (nameInput) nameInput.value = '';
    var s1 = document.getElementById('trimStartSlider');
    var s2 = document.getElementById('trimEndSlider');
    if (s1) s1.value = 0;
    if (s2) s2.value = 100;
    editorAudioBuffer = null;
    stopTrimPreview();
    showEditorUploadZone();
    var panel = document.getElementById('customSoundPanel');
    if (panel) panel.style.display = 'block';
}

// 关闭自定义音效面板
function closeCustomPanel() {
    var panel = document.getElementById('customSoundPanel');
    if (panel) panel.style.display = 'none';
    stopTrimPreview();
    editorAudioBuffer = null;
    editingSoundId = null;
    pendingEditorMode = false;
    var fileInput = document.getElementById('customSoundFile');
    if (fileInput) fileInput.value = '';
    // 恢复下拉框
    restoreSoundSelect();
}

// 编辑器内浏览文件按钮
function triggerEditorFileInput() {
    pendingEditorMode = true;
    stopTrimPreview();
    document.getElementById('customSoundFile').click();
}

// 编辑已有的自定义音效
function openEditorForSound(id) {
    const sound = findSoundById(id);
    if (!sound) return;
    editingSoundId = id;
    var panel = document.getElementById('customSoundPanel');
    if (panel) panel.style.display = 'block';
    showEditorUploadZone();
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    fetch(sound.dataUrl)
        .then(res => res.arrayBuffer())
        .then(buf => audioCtx.decodeAudioData(buf))
        .then(buffer => {
            editorAudioBuffer = buffer;
            editorFileName = sound.name;
            var nameInput = document.getElementById('audioCustomName');
            if (nameInput) nameInput.value = sound.name;
            var s1 = document.getElementById('trimStartSlider');
            var s2 = document.getElementById('trimEndSlider');
            if (s1) s1.value = 0;
            if (s2) s2.value = 100;
            var fill = document.getElementById('dualRangeFill');
            if (fill) { fill.style.left = '0%'; fill.style.width = '100%'; }
            updateTrimDisplay();
            showEditorWaveform();
            requestAnimationFrame(() => drawWaveform());
        })
        .catch(() => {
            alert('加载音效失败');
            closeCustomPanel();
        });
}

function drawWaveform() {
    const canvas = document.getElementById('audioWaveform');
    if (!canvas || !editorAudioBuffer) return;
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const w = canvas.width = rect.width * dpr;
    const h = canvas.height = 80 * dpr;
    canvas.style.width = rect.width + 'px';
    canvas.style.height = '80px';
    const ctx = canvas.getContext('2d');
    ctx.scale(dpr, dpr);
    const cw = rect.width;
    const ch = 80;
    ctx.clearRect(0, 0, cw, ch);

    const data = editorAudioBuffer.getChannelData(0);
    const step = Math.ceil(data.length / cw);
    const mid = ch / 2;
    const amp = ch / 2 - 6;

    // 波形主体
    ctx.beginPath();
    ctx.moveTo(0, mid);
    for (let i = 0; i < cw; i++) {
        let max = 0;
        const start = i * step;
        const end = Math.min(start + step, data.length);
        for (let j = start; j < end; j++) {
            const abs = Math.abs(data[j]);
            if (abs > max) max = abs;
        }
        const y = mid - max * amp;
        ctx.lineTo(i, y);
    }
    for (let i = cw - 1; i >= 0; i--) {
        let max = 0;
        const start = i * step;
        const end = Math.min(start + step, data.length);
        for (let j = start; j < end; j++) {
            const abs = Math.abs(data[j]);
            if (abs > max) max = abs;
        }
        const y = mid + max * amp;
        ctx.lineTo(i, y);
    }
    ctx.closePath();
    // 渐变填充
    var grad = ctx.createLinearGradient(0, 0, 0, ch);
    grad.addColorStop(0, '#818cf8');
    grad.addColorStop(0.5, '#6366f1');
    grad.addColorStop(1, '#4f46e5');
    ctx.fillStyle = grad;
    ctx.fill();

    // 裁剪区域高亮
    const sPct = parseInt(document.getElementById('trimStartSlider').value) / 100;
    const ePct = parseInt(document.getElementById('trimEndSlider').value) / 100;
    const x1 = sPct * cw;
    const x2 = ePct * cw;

    if (x2 - x1 > 0) {
        ctx.fillStyle = 'rgba(0,0,0,0.12)';
        ctx.fillRect(0, 0, x1, ch);
        ctx.fillRect(x2, 0, cw - x2, ch);
    }
    ctx.strokeStyle = '#818cf8';
    ctx.lineWidth = 2.5;
    ctx.setLineDash([5, 4]);
    ctx.beginPath();
    ctx.moveTo(x1, 0); ctx.lineTo(x1, ch);
    ctx.moveTo(x2, 0); ctx.lineTo(x2, ch);
    ctx.stroke();
    ctx.setLineDash([]);
}

function formatSeconds(secs) {
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return m + ':' + String(s).padStart(2, '0');
}

let waveformRafId = null;

function onTrimSliderChange() {
    const startSlider = document.getElementById('trimStartSlider');
    const endSlider = document.getElementById('trimEndSlider');
    let startVal = parseInt(startSlider.value);
    let endVal = parseInt(endSlider.value);
    if (startVal >= endVal) {
        if (document.activeElement === startSlider) {
            endVal = Math.min(100, startVal + 1);
            endSlider.value = endVal;
        } else {
            startVal = Math.max(0, endVal - 1);
            startSlider.value = startVal;
        }
    }
    // 立即更新填充条（无延迟）
    const fill = document.getElementById('dualRangeFill');
    if (fill) {
        fill.style.left = startVal + '%';
        fill.style.width = (endVal - startVal) + '%';
    }
    updateTrimDisplay();
    // 使用 RAF 避免波形绘制卡顿
    if (waveformRafId) cancelAnimationFrame(waveformRafId);
    waveformRafId = requestAnimationFrame(() => drawWaveform());
}

function updateTrimDisplay() {
    if (!editorAudioBuffer) return;
    const dur = editorAudioBuffer.duration;
    const sPct = parseInt(document.getElementById('trimStartSlider').value) / 100;
    const ePct = parseInt(document.getElementById('trimEndSlider').value) / 100;
    document.getElementById('trimStartTime').textContent = formatSeconds(dur * sPct);
    document.getElementById('trimEndTime').textContent = formatSeconds(dur * ePct);
}

function toggleTrimPreview() {
    if (isEditorPreviewPlaying) { stopTrimPreview(); return; }
    if (!editorAudioBuffer) return;
    const dur = editorAudioBuffer.duration;
    const sPct = parseInt(document.getElementById('trimStartSlider').value) / 100;
    const ePct = parseInt(document.getElementById('trimEndSlider').value) / 100;
    const startSec = dur * sPct;
    const sliceLen = dur * (ePct - sPct);
    if (sliceLen <= 0.05) return;

    editorPreviewCtx = new (window.AudioContext || window.webkitAudioContext)();
    const source = editorPreviewCtx.createBufferSource();
    source.buffer = editorAudioBuffer;
    const gain = editorPreviewCtx.createGain();
    source.connect(gain); gain.connect(editorPreviewCtx.destination);
    gain.gain.setValueAtTime(1.2, editorPreviewCtx.currentTime);
    source.start(0, startSec, sliceLen);
    source.onended = () => { isEditorPreviewPlaying = false; updatePreviewBtn(); };
    editorPreviewSource = source;
    isEditorPreviewPlaying = true;
    updatePreviewBtn();
}

function stopTrimPreview() {
    if (editorPreviewSource) { try { editorPreviewSource.stop(); } catch(e) {} editorPreviewSource = null; }
    if (editorPreviewCtx) { editorPreviewCtx.close().catch(() => {}); editorPreviewCtx = null; }
    isEditorPreviewPlaying = false;
    updatePreviewBtn();
}

function updatePreviewBtn() {
    const btn = document.getElementById('audioPreviewBtn');
    if (!btn) return;
    if (isEditorPreviewPlaying) {
        btn.innerHTML = '<i class="fas fa-stop"></i>';
        btn.classList.add('playing');
    } else {
        btn.innerHTML = '<i class="fas fa-play"></i>';
        btn.classList.remove('playing');
    }
}

// 将 AudioBuffer 编码为 WAV 格式的 data URL
function encodeWAV(audioBuffer) {
    const numChannels = 1;
    const sampleRate = audioBuffer.sampleRate;
    const format = 1; // PCM
    const bitsPerSample = 16;
    const data = audioBuffer.getChannelData(0);
    const byteRate = sampleRate * numChannels * bitsPerSample / 8;
    const blockAlign = numChannels * bitsPerSample / 8;
    const dataLength = data.length * (bitsPerSample / 8);
    const buffer = new ArrayBuffer(44 + dataLength);
    const view = new DataView(buffer);

    function writeString(offset, str) {
        for (let i = 0; i < str.length; i++) view.setUint8(offset + i, str.charCodeAt(i));
    }

    writeString(0, 'RIFF');
    view.setUint32(4, 36 + dataLength, true);
    writeString(8, 'WAVE');
    writeString(12, 'fmt ');
    view.setUint32(16, 16, true);
    view.setUint16(20, format, true);
    view.setUint16(22, numChannels, true);
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, byteRate, true);
    view.setUint16(32, blockAlign, true);
    view.setUint16(34, bitsPerSample, true);
    writeString(36, 'data');
    view.setUint32(40, dataLength, true);

    let offset = 44;
    for (let i = 0; i < data.length; i++) {
        const sample = Math.max(-1, Math.min(1, data[i]));
        const intSample = sample < 0 ? sample * 0x8000 : sample * 0x7FFF;
        view.setInt16(offset, intSample, true);
        offset += 2;
    }

    const blob = new Blob([buffer], { type: 'audio/wav' });
    return new Promise(resolve => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.readAsDataURL(blob);
    });
}

// 快速上传（直接存储，不弹编辑器）
function quickUploadSound(file) {
    if (!file.type.startsWith('audio/')) {
        alert('请选择音频文件（mp3、wav、ogg 等）');
        return;
    }
    const reader = new FileReader();
    reader.onload = function(ev) {
        const dataUrl = ev.target.result;
        if (dataUrl.length > 5000000) {
            alert('音频文件太大，请使用短一点的音频（建议 30 秒以内）');
            return;
        }
        const name = file.name.replace(/\.[^.]+$/, '');
        const sounds = getSavedSounds();
        const id = 'custom_' + Date.now();
        // 估算时长（粗略）
        const audio = new Audio(dataUrl);
        audio.addEventListener('loadedmetadata', () => {
            sounds.push({ id, name, dataUrl, duration: audio.duration || 1 });
            saveSoundsList(sounds);
            updateSoundSelectOptions();
            renderSavedSoundsList();
            // 自动选中
            document.getElementById('soundSelect').value = id;
            setSoundPref(id);
            loadCustomSoundBuffer();
            showToast('已添加「' + name + '」');
        });
        audio.addEventListener('error', () => {
            sounds.push({ id, name, dataUrl, duration: 1 });
            saveSoundsList(sounds);
            updateSoundSelectOptions();
            renderSavedSoundsList();
            document.getElementById('soundSelect').value = id;
            setSoundPref(id);
            loadCustomSoundBuffer();
            showToast('已添加「' + name + '」');
        });
    };
    reader.readAsDataURL(file);
}

async function saveTrimmedAudio() {
    if (!editorAudioBuffer) {
        alert('请先加载音频文件');
        return;
    }
    const saveBtn = document.getElementById('audioSaveBtn');
    if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<i class="fas fa-spinner fa-pulse"></i> 保存中...';
    }
    try {
    const name = document.getElementById('audioCustomName').value.trim() || editorFileName || '自定义音效';
    const dur = editorAudioBuffer.duration;
    const sPct = parseInt(document.getElementById('trimStartSlider').value) / 100;
    const ePct = parseInt(document.getElementById('trimEndSlider').value) / 100;
    const startSec = dur * sPct;
    const sliceLen = dur * (ePct - sPct);

    if (sliceLen <= 0.1) {
        alert('裁剪片段太短，请调整起止点');
        return;
    }

    // 裁剪
    const offlineCtx = new OfflineAudioContext(1, Math.ceil(sliceLen * editorAudioBuffer.sampleRate), editorAudioBuffer.sampleRate);
    const source = offlineCtx.createBufferSource();
    source.buffer = editorAudioBuffer;
    source.connect(offlineCtx.destination);
    source.start(0, startSec, sliceLen);
    const rendered = await offlineCtx.startRendering();

    const dataUrl = await encodeWAV(rendered);

    if (dataUrl.length > 5000000) {
        alert('音频文件太大，请缩短裁剪范围（建议 30 秒以内）');
        return;
    }

    let sounds = getSavedSounds();

    if (editingSoundId) {
        // 编辑模式：替换已有音效
        sounds = sounds.map(s => {
            if (s.id === editingSoundId) {
                return { ...s, name, dataUrl, duration: sliceLen };
            }
            return s;
        });
        delete customSoundCache[editingSoundId];
    } else {
        // 新建模式
        const id = 'custom_' + Date.now();
        sounds.push({ id, name, dataUrl, duration: sliceLen });
    }

    saveSoundsList(sounds);
    updateSoundSelectOptions();
    renderSavedSoundsList();

    // 选中刚保存的音效（自动加入下拉框）
    const lastSound = sounds[sounds.length - 1];
    const selectId = editingSoundId || (lastSound ? lastSound.id : 'beep');
    const sel = document.getElementById('soundSelect');
    if (sel) {
        // 确认选项存在后再设置
        if (sel.querySelector('option[value="' + selectId + '"]')) {
            sel.value = selectId;
        } else if (isPresetSound(selectId)) {
            sel.value = selectId;
        } else {
            sel.value = 'beep';
        }
    }
    setSoundPref(selectId);
    loadCustomSoundBuffer();

    closeCustomPanel();
    showToast(editingSoundId ? '音效已更新 ✓' : '已保存「' + name + '」，已加入铃声列表');
    } catch (e) {
        console.error('保存音效失败', e);
        alert('保存失败：' + (e.message || '未知错误'));
        if (saveBtn) {
            saveBtn.disabled = false;
            saveBtn.innerHTML = '<i class="fas fa-check"></i> 保存音效';
        }
    }
}

function deleteSavedSound(id) {
    let sounds = getSavedSounds();
    sounds = sounds.filter(s => s.id !== id);
    saveSoundsList(sounds);
    delete customSoundCache[id];
    if (getSoundPref() === id) {
        setSoundPref('beep');
        customAudioBuffer = null;
    }
    updateSoundSelectOptions();
    renderSavedSoundsList();
    // 确保下拉框选中有效值
    const sel = document.getElementById('soundSelect');
    const pref = getSoundPref();
    if (sel && findSoundById(pref)) {
        sel.value = pref;
    } else if (sel && isPresetSound(pref)) {
        sel.value = pref;
    } else if (sel) {
        sel.value = 'beep';
    }
}

function updateSoundSelectOptions() {
    const sel = document.getElementById('soundSelect');
    if (!sel) return;
    const sounds = getSavedSounds();
    const pref = getSoundPref();

    // 保存当前选中值
    const currentVal = sel.value;

    // 清除所有自定义音效选项（保留预设 + __custom__）
    sel.querySelectorAll('option.custom-sound-option').forEach(opt => opt.remove());

    // 添加已保存的自定义音效到下拉框（插入在 __custom__ 之前）
    const customOpt = sel.querySelector('option[value="__custom__"]');
    sounds.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s.id;
        opt.className = 'custom-sound-option';
        opt.textContent = '🎵 ' + s.name + ' (' + formatSeconds(s.duration) + ')';
        opt.style.color = '#6366f1';
        opt.style.fontWeight = '500';
        if (customOpt) {
            sel.insertBefore(opt, customOpt);
        } else {
            sel.appendChild(opt);
        }
    });

    // 恢复选中项
    if (sounds.some(s => s.id === currentVal) || currentVal === '__custom__' || isPresetSound(currentVal)) {
        sel.value = currentVal;
    } else if (sounds.some(s => s.id === pref)) {
        sel.value = pref;
    } else if (isPresetSound(pref)) {
        sel.value = pref;
    } else {
        sel.value = 'beep';
    }
}

function renderSavedSoundsList() {
    const list = document.getElementById('savedSoundsList');
    if (!list) return;
    const sounds = getSavedSounds();
    const pref = getSoundPref();
    if (sounds.length === 0) {
        list.innerHTML = '';
        return;
    }
    list.innerHTML = sounds.map(s => `
        <div class="saved-sound-item${s.id === pref ? ' active' : ''}" onclick="selectSavedSound('${s.id}')">
            <div class="saved-sound-icon"><i class="fas fa-music"></i></div>
            <span class="saved-sound-name">${escapeHtml(s.name)}</span>
            <span class="saved-sound-dur">${formatSeconds(s.duration)}</span>
            <button class="saved-sound-edit" onclick="event.stopPropagation();openEditorForSound('${s.id}')" title="编辑音效"><i class="fas fa-pen"></i></button>
            <button class="saved-sound-del" onclick="event.stopPropagation();deleteSavedSound('${s.id}')" title="删除音效"><i class="fas fa-trash-alt"></i></button>
        </div>
    `).join('');
}

function selectSavedSound(id) {
    const sound = findSoundById(id);
    if (!sound) return;
    document.getElementById('soundSelect').value = id;
    setSoundPref(id);
    loadCustomSoundBuffer();
    renderSavedSoundsList();
}

function updateCustomSoundInfoUI() {
    // 不再使用 info bar，改用 saved sounds list
    renderSavedSoundsList();
}

function isPresetSound(type) {
    return ['beep','alarm','chime','bell','digital'].includes(type);
}

// 恢复下拉框到当前保存的音效选项
function restoreSoundSelect() {
    const sel = document.getElementById('soundSelect');
    if (!sel) return;
    const pref = getSoundPref();
    if (findSoundById(pref)) {
        sel.value = pref;
    } else if (isPresetSound(pref)) {
        sel.value = pref;
    } else {
        sel.value = 'beep';
    }
    renderSavedSoundsList();
}

function playAlarm() {
    const type = getSoundPref();

    // 浏览器通知
    if (Notification.permission === 'granted') {
        new Notification('Countdown Finished', { body: countdownEventDesc || 'Time is up!' });
    } else if (Notification.permission !== 'denied') {
        Notification.requestPermission();
    }

    if (!isPresetSound(type)) {
        if (customAudioBuffer) {
            playCustomSound();
        } else {
            // 自定义音效未加载，回退到 beep
            const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
            playPresetSound(audioCtx, 'beep');
        }
        return;
    }

    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    if (audioCtx.state === 'suspended') {
        audioCtx.resume().then(() => playPresetSound(audioCtx, type));
    } else {
        playPresetSound(audioCtx, type);
    }
}

// 试听当前选择的音效
function testSound() {
    const type = getSoundPref();
    if (!isPresetSound(type)) {
        if (customAudioBuffer) {
            playCustomSound();
        } else {
            const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
            playPresetSound(audioCtx, 'beep');
        }
        return;
    }
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    if (audioCtx.state === 'suspended') {
        audioCtx.resume().then(() => playPresetSound(audioCtx, type));
    } else {
        playPresetSound(audioCtx, type);
    }
}

// ========== 音效 UI 事件绑定 ==========
function bindSoundEvents() {
    const soundSelect = document.getElementById('soundSelect');
    const soundTestBtn = document.getElementById('soundTestBtn');
    const customSoundFile = document.getElementById('customSoundFile');

    // 刷新自定义音效列表
    updateSoundSelectOptions();
    renderSavedSoundsList();

    // 恢复之前保存的音效偏好
    const savedPref = getSoundPref();
    if (savedPref && findSoundById(savedPref)) {
        soundSelect.value = savedPref;
        loadCustomSoundBuffer();
    } else if (savedPref && !isPresetSound(savedPref)) {
        soundSelect.value = 'beep';
        setSoundPref('beep');
    } else {
        soundSelect.value = savedPref || 'beep';
    }

    soundSelect.addEventListener('change', function() {
        const val = this.value;
        if (val === '__custom__') {
            // 打开自定义音效面板（不重置下拉框，让 __custom__ 保持选中状态）
            openCustomPanel();
            return;
        }
        if (isPresetSound(val)) {
            setSoundPref(val);
            customAudioBuffer = null;
        } else if (findSoundById(val)) {
            setSoundPref(val);
            loadCustomSoundBuffer();
        }
        renderSavedSoundsList();
    });

    soundTestBtn.addEventListener('click', testSound);

    // 编辑器模式：选择文件 → 加载到编辑器
    customSoundFile.addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (!file) { pendingEditorMode = false; return; }
        pendingEditorMode = false;
        loadFileToEditor(file);
        this.value = '';
    });

    // 初始化显示
    renderSavedSoundsList();
}

function showToast(msg) {
    // 简单的 toast 提示
    const t = document.createElement('div');
    t.textContent = msg;
    t.style.cssText = 'position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:#2C3E50;color:#fff;padding:12px 32px;border-radius:25px;font-size:15px;font-weight:600;z-index:99999;pointer-events:none;';
    document.body.appendChild(t);
    setTimeout(() => { t.style.transition = 'opacity 0.3s'; t.style.opacity = '0'; }, 1200);
    setTimeout(() => t.remove(), 1600);
}



    async function addHistoryItem(type, data) {
    // 直接重新加载历史，确保与数据库完全一致
    await loadHistoryFromBackend();
}
function renderHistory() {
    if (historyRecords.length === 0) {
        historyList.innerHTML = '<div class="history-empty">No records yet</div>';
        return;
    }
    let html = '';
    historyRecords.forEach(r => {
        const icon = r.type === 'stopwatch' ? '⏱️' : '⏳';
        // 修复时间显示：如果 endTime 无效则显示 '—'
        const timeStr = r.endTime ? new Date(r.endTime).toLocaleTimeString() : '—';
        html += `<div class="history-item">
            <div class="history-info">
                <span class="history-task">${icon} ${escapeHtml(r.title)}</span>
                <span class="history-meta">${timeStr}</span>
            </div>
            <div class="history-duration">${r.detail}</div>
        </div>`;
    });
    historyList.innerHTML = html;
}

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function updateTodayTotal() {
        let total = 0;
        historyRecords.forEach(r => {
            if (r.type === 'stopwatch') {
                const parts = r.detail.split(':');
                if (parts.length === 2) {
                    total += (+parts[0] * 60) + (+parts[1]);
                }
            }
        });
        const m = Math.floor(total / 60);
        const s = total % 60;
        todayTotalSpan.textContent = `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }

   async function clearHistory() {
    if (confirm('确定要永久删除所有历史记录吗？')) {
        try {
            await fetch('/api/timer/history', { method: 'DELETE' });
            await fetch('/api/countdown/history', { method: 'DELETE' });
        } catch (e) {
            console.error('删除失败', e);
        }
        // 清空本地数据并刷新显示
        historyRecords = [];
        renderHistory();
        updateTodayTotal();
        sessionSummary.style.display = 'none';
        countdownSummary.style.display = 'none';
    }
}

    function switchMode(mode) {
        currentMode = mode;
        modeBtns.forEach(btn => {
            btn.classList.toggle('active', btn.dataset.mode === mode);
        });
        if (mode === 'stopwatch') {
            stopwatchPanel.style.display = 'block';
            countdownPanel.style.display = 'none';
            if (isCountdownRunning) {
                pauseCountdown();
            }
        } else {
            stopwatchPanel.style.display = 'none';
            countdownPanel.style.display = 'block';
            if (isStopwatchRunning) {
                stopStopwatch();
            }
            applySettingsToCountdown();
        }
    }
function bindEvents() {
    modeBtns.forEach(btn => {
        btn.addEventListener('click', () => switchMode(btn.dataset.mode));
    });
    startBtn.addEventListener('click', startStopwatch);
    stopBtn.addEventListener('click', stopStopwatch);
    resetBtn.addEventListener('click', resetStopwatch);
    countdownStartBtn.addEventListener('click', startCountdown);
    countdownPauseBtn.addEventListener('click', pauseCountdown);
    countdownResetBtn.addEventListener('click', resetCountdown);
    [hoursInput, minutesInput, secondsInput].forEach(i => i.addEventListener('change', () => {
        if (!isCountdownRunning) applySettingsToCountdown();
    }));
    clearHistoryBtn.addEventListener('click', clearHistory);
}
   
     

  async function loadHistoryFromBackend() {
    const allRecords = [];

    // 正计时历史（独立容错）
    try {
        const resp = await fetch('/api/timer/history');
        if (resp.ok) {
            const logs = await resp.json();
            logs.forEach(log => {
                allRecords.push({
                    type: 'stopwatch',
                    title: log.eventDescription || log.taskName || 'Task',
                    detail: formatTime(log.durationSeconds),
                    endTime: log.endTime,
                    timestamp: log.createdAt
                });
            });
        }
    } catch (e) { console.warn('正计时历史加载失败', e); }

    // 倒计时历史（独立容错）
    try {
        const resp = await fetch('/api/countdown/history');
        if (resp.ok) {
            const logs = await resp.json();
            logs.forEach(log => {
                allRecords.push({
                    type: 'countdown',
                    title: log.eventDescription || 'Countdown',
                    detail: `${formatTime(log.actualDurationSeconds)} / ${formatTime(log.targetDurationSeconds)}`,
                    endTime: log.endTime,
                    timestamp: log.createdAt
                });
            });
        }
    } catch (e) { console.warn('倒计时历史加载失败', e); }

    allRecords.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
    historyRecords = allRecords;
    renderHistory();
    updateTodayTotal();
}

    // ========== 暴露给 HTML onclick 的全局函数 ==========
    window.openCustomPanel = openCustomPanel;
    window.closeCustomPanel = closeCustomPanel;
    window.openEditorForSound = openEditorForSound;
    window.saveTrimmedAudio = saveTrimmedAudio;
    window.toggleTrimPreview = toggleTrimPreview;
    window.onTrimSliderChange = onTrimSliderChange;
    window.deleteSavedSound = deleteSavedSound;
    window.selectSavedSound = selectSavedSound;
    window.triggerEditorFileInput = triggerEditorFileInput;
    window.handleEditorDrop = handleEditorDrop;

    async function init() {
        updateStopwatchUI();
        updateStopwatchStatus(false);
        updateStopwatchButtons(false);

        applySettingsToCountdown();
        updateCountdownStatus(false, false);
        updateCountdownButtons(false, false);
        bindEvents();
        bindSoundEvents();
        loadCustomSoundBuffer();
        switchMode('stopwatch');
        if (Notification.permission === 'default') Notification.requestPermission();
         await loadHistoryFromBackend();
    }
    init();
})();
