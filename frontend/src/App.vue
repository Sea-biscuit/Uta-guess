<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";

const API_BASE = "http://localhost:8080";
const config = {
  introSeconds: 12,
  randomSeconds: 10,
  fadeInMs: 800,
  fadeOutMs: 800,
  defaultVolume: 1,
  defaultHideAnswer: true
};

const songs = ref([]);
const order = ref([]);
const currentIndex = ref(0);
const isAnswerVisible = ref(!config.defaultHideAnswer);
const isPlaying = ref(false);
const mode = ref("intro");
const sequenceMode = ref("ordered");
const randomStartBySongId = ref({});
const statusText = ref("准备中");
const audioRef = ref(null);

let stopTimer = null;
let fadeTimer = null;

const currentSong = computed(() => {
  if (!order.value.length) return null;
  const songId = order.value[currentIndex.value];
  return songs.value.find((item) => item.id === songId) || null;
});

const canPlay = computed(() => !!currentSong.value);
const currentCoverUrl = computed(() => {
  if (!currentSong.value?.coverUrl) return "";
  return `${API_BASE}${currentSong.value.coverUrl}`;
});

onMounted(async () => {
  await loadSongs();
  if (audioRef.value) {
    audioRef.value.onerror = () => {
      statusText.value = "音频加载失败，请检查后端与文件格式";
      isPlaying.value = false;
    };
  }
});

onBeforeUnmount(() => {
  hardStopAudio();
});

async function loadSongs() {
  try {
    const resp = await fetch(`${API_BASE}/api/songs`);
    if (!resp.ok) throw new Error("加载失败");
    const data = await resp.json();
    songs.value = data;
    order.value = data.map((s) => s.id);
    statusText.value = data.length ? "题库已加载" : "未找到歌曲";
  } catch (err) {
    statusText.value = `加载失败: ${err.message}`;
  }
}

function playIntro() {
  playByMode("intro");
}

function playRandom() {
  playByMode("random");
}

function replayCurrentSegment() {
  if (!currentSong.value) return;
  if (mode.value === "random" && randomStartBySongId.value[currentSong.value.id] == null) {
    randomStartBySongId.value[currentSong.value.id] = getRandomStart(currentSong.value.duration, config.randomSeconds);
  }
  playByMode(mode.value);
}

function rerollRandom() {
  if (!currentSong.value) return;
  randomStartBySongId.value[currentSong.value.id] = getRandomStart(currentSong.value.duration, config.randomSeconds);
  playByMode("random");
}

function playByMode(nextMode) {
  if (!currentSong.value || !audioRef.value) return;
  mode.value = nextMode;
  const song = currentSong.value;
  const audio = audioRef.value;

  hardStopAudio(false);
  const clipSeconds = nextMode === "intro" ? config.introSeconds : config.randomSeconds;
  const startTime = nextMode === "intro"
    ? 0
    : getOrCreateRandomStart(song.id, song.duration, clipSeconds);

  audio.src = `${API_BASE}${song.audioUrl}`;
  audio.load();
  audio.onloadedmetadata = async () => {
    try {
      audio.currentTime = startTime;
      audio.volume = 0;
      await audio.play();
      isPlaying.value = true;
      statusText.value = `${nextMode === "intro" ? "开头模式" : "随机模式"} 播放中`;
      fadeVolume(audio, 0, config.defaultVolume, config.fadeInMs);
      stopTimer = setTimeout(() => stopWithFadeOut(), clipSeconds * 1000);
    } catch (err) {
      isPlaying.value = false;
      statusText.value = `播放失败: ${err?.message || "浏览器拦截或格式不支持"}`;
    }
  };
}

function stopWithFadeOut() {
  if (!audioRef.value) return;
  const audio = audioRef.value;
  clearTimeout(stopTimer);
  fadeVolume(audio, audio.volume, 0, config.fadeOutMs, () => {
    audio.pause();
    isPlaying.value = false;
    statusText.value = "已停止";
  });
}

function hardStopAudio(resetSrc = true) {
  clearTimeout(stopTimer);
  clearInterval(fadeTimer);
  if (!audioRef.value) return;
  const audio = audioRef.value;
  audio.pause();
  audio.currentTime = 0;
  audio.volume = config.defaultVolume;
  if (resetSrc) {
    audio.src = "";
  }
  isPlaying.value = false;
}

function fadeVolume(audio, from, to, duration, onDone) {
  clearInterval(fadeTimer);
  const steps = 20;
  const delta = (to - from) / steps;
  const stepMs = Math.max(20, Math.floor(duration / steps));
  let current = from;
  audio.volume = clamp(current);
  fadeTimer = setInterval(() => {
    current += delta;
    const reached = delta >= 0 ? current >= to : current <= to;
    audio.volume = clamp(reached ? to : current);
    if (reached) {
      clearInterval(fadeTimer);
      if (onDone) onDone();
    }
  }, stepMs);
}

function clamp(value) {
  return Math.max(0, Math.min(1, value));
}

function getOrCreateRandomStart(songId, duration, clipSeconds) {
  if (randomStartBySongId.value[songId] == null) {
    randomStartBySongId.value[songId] = getRandomStart(duration, clipSeconds);
  }
  return randomStartBySongId.value[songId];
}

function getRandomStart(duration, clipSeconds) {
  if (duration <= clipSeconds) return 0;
  return Math.random() * (duration - clipSeconds);
}

function toggleAnswer() {
  isAnswerVisible.value = !isAnswerVisible.value;
}

function goNext() {
  if (!order.value.length) return;
  currentIndex.value = (currentIndex.value + 1) % order.value.length;
  resetForQuestionChange();
}

function goPrev() {
  if (!order.value.length) return;
  currentIndex.value = (currentIndex.value - 1 + order.value.length) % order.value.length;
  resetForQuestionChange();
}

function jumpTo(orderIndex) {
  currentIndex.value = orderIndex;
  resetForQuestionChange();
}

function resetForQuestionChange() {
  hardStopAudio();
  isAnswerVisible.value = !config.defaultHideAnswer;
  statusText.value = "已切题，等待播放";
}

function shuffleOrder() {
  const ids = [...songs.value.map((s) => s.id)];
  for (let i = ids.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [ids[i], ids[j]] = [ids[j], ids[i]];
  }
  order.value = ids;
  currentIndex.value = 0;
  resetForQuestionChange();
}

function changeSequenceMode(next) {
  sequenceMode.value = next;
  if (next === "ordered") {
    order.value = songs.value.map((s) => s.id);
  } else {
    shuffleOrder();
    return;
  }
  currentIndex.value = 0;
  resetForQuestionChange();
}
</script>

<template>
  <main class="page">
    <h1>晚会猜歌控制台</h1>
    <section class="panel">
      <div class="headline">
        <div>第 {{ currentIndex + 1 }} / {{ order.length || 0 }} 题</div>
        <div class="status">状态：{{ statusText }}</div>
      </div>

      <div class="answer">
        <template v-if="currentSong">
          <div class="cover-box">
            <img v-if="isAnswerVisible && currentCoverUrl" :src="currentCoverUrl" alt="专辑封面" class="cover-image" />
            <div v-else class="cover-placeholder">{{ isAnswerVisible ? "无封面" : "封面未揭晓" }}</div>
          </div>
          <div v-if="isAnswerVisible" class="answer-visible">
            <div class="title">{{ currentSong.title }}</div>
            <div class="artist">{{ currentSong.artist }}</div>
          </div>
          <div v-else class="answer-hidden">答案未揭晓</div>
        </template>
      </div>

      <div class="controls">
        <button :disabled="!canPlay" @click="playIntro">播放开头</button>
        <button :disabled="!canPlay" @click="playRandom">播放随机</button>
        <button :disabled="!canPlay || !isPlaying" @click="stopWithFadeOut">停止</button>
        <button :disabled="!canPlay" @click="replayCurrentSegment">重播片段</button>
        <button :disabled="!canPlay" @click="rerollRandom">重新随机并播放</button>
        <button :disabled="!canPlay" @click="toggleAnswer">
          {{ isAnswerVisible ? "隐藏答案" : "揭晓答案" }}
        </button>
      </div>

      <div class="controls">
        <button :disabled="!canPlay" @click="goPrev">上一题</button>
        <button :disabled="!canPlay" @click="goNext">下一题</button>
      </div>

      <div class="controls">
        <button :class="{ active: sequenceMode === 'ordered' }" @click="changeSequenceMode('ordered')">
          顺序模式
        </button>
        <button :class="{ active: sequenceMode === 'shuffled' }" @click="changeSequenceMode('shuffled')">
          乱序模式
        </button>
      </div>

      <div class="question-list">
        <button
          v-for="(songId, idx) in order"
          :key="songId"
          :class="{ current: idx === currentIndex }"
          @click="jumpTo(idx)"
        >
          {{ idx + 1 }}
        </button>
      </div>

      <audio ref="audioRef" preload="none" />
    </section>
  </main>
</template>
