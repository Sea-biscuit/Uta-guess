import { computed, createApp, onBeforeUnmount, onMounted, ref } from "./vendor/vue.esm-browser.prod.js";

createApp({
  setup() {
    const config = {
      introSeconds: 12,
      randomSeconds: 10,
      fadeInMs: 800,
      fadeOutMs: 800,
      defaultVolume: 1,
      defaultHideAnswer: true
    };

    const songs = ref([]);
    const selectedIds = ref([]);
    const drawCount = ref(0);
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
    const songCount = computed(() => songs.value.length);
    const canDraw = computed(() =>
      Number.isInteger(drawCount.value) && drawCount.value >= 1 && drawCount.value <= songCount.value
    );
    const currentCoverUrl = computed(() => {
      if (!currentSong.value?.coverUrl) return "";
      return currentSong.value.coverUrl;
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
        const resp = await fetch("/api/songs");
        if (!resp.ok) throw new Error("加载失败");
        const data = await resp.json();
        songs.value = data;
        selectedIds.value = data.map((s) => s.id);
        drawCount.value = data.length;
        order.value = [...selectedIds.value];
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

      audio.src = song.audioUrl;
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
        audio.onloadedmetadata = null;
        audio.removeAttribute("src");
        audio.load();
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

    function shuffle(ids) {
      for (let i = ids.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [ids[i], ids[j]] = [ids[j], ids[i]];
      }
      return ids;
    }

    function drawSongs() {
      if (!canDraw.value) return;
      const ids = songs.value.map((song) => song.id);
      const picked = new Set(shuffle([...ids]).slice(0, drawCount.value));
      selectedIds.value = ids.filter((id) => picked.has(id));
      randomStartBySongId.value = {};
      changeSequenceMode(sequenceMode.value);
      statusText.value = `已抽取 ${order.value.length} 首，等待播放`;
    }

    function changeSequenceMode(next) {
      sequenceMode.value = next;
      if (!selectedIds.value.length) return;
      const ids = [...selectedIds.value];
      order.value = next === "ordered" ? ids : shuffle(ids);
      currentIndex.value = 0;
      resetForQuestionChange();
    }

    return {
      drawCount,
      songCount,
      canDraw,
      drawSongs,
      order,
      currentIndex,
      isAnswerVisible,
      isPlaying,
      sequenceMode,
      statusText,
      audioRef,
      currentSong,
      canPlay,
      currentCoverUrl,
      playIntro,
      playRandom,
      stopWithFadeOut,
      replayCurrentSegment,
      rerollRandom,
      toggleAnswer,
      goPrev,
      goNext,
      changeSequenceMode,
      jumpTo
    };
  }
}).mount("#app");
