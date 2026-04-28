package com.utawaaa.guesssong.service;

import com.utawaaa.guesssong.config.AppProperties;
import com.utawaaa.guesssong.model.Song;
import jakarta.annotation.PostConstruct;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class SongService {
    private static final Logger log = LoggerFactory.getLogger(SongService.class);
    private static final String UNKNOWN_ARTIST = "未知歌手";

    private final AppProperties properties;
    private final Map<Long, Song> songsById = new ConcurrentHashMap<>();
    private volatile List<Song> songs = List.of();

    public SongService(AppProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        if (properties.isRescanOnStartup()) {
            scanSongs();
        }
    }

    public synchronized void scanSongs() {
        Path root = Paths.get(properties.getMusicFolder()).toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            log.warn("Music folder does not exist: {}", root);
            songs = List.of();
            songsById.clear();
            return;
        }

        AtomicLong idGen = new AtomicLong(1);
        List<Song> loaded = new ArrayList<>();
        String ext = "." + properties.getFormat().toLowerCase();

        try (Stream<Path> pathStream = Files.walk(root)) {
            List<Path> files = pathStream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(ext))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();

            for (Path file : files) {
                Optional<Song> songOpt = parseSong(file, idGen.getAndIncrement());
                songOpt.ifPresent(loaded::add);
            }
        } catch (IOException e) {
            log.error("Failed to scan songs under {}", root, e);
        }

        songs = List.copyOf(loaded);
        songsById.clear();
        for (Song song : loaded) {
            songsById.put(song.getId(), song);
        }

        log.info("Loaded {} song(s)", loaded.size());
    }

    public List<Song> listSongs() {
        return songs;
    }

    public Optional<Song> findById(long id) {
        return Optional.ofNullable(songsById.get(id));
    }

    private Optional<Song> parseSong(Path file, long id) {
        try {
            AudioFile audioFile = AudioFileIO.read(file.toFile());
            Tag tag = audioFile.getTagOrCreateAndSetDefault();

            String title = blankToNull(tag.getFirst(FieldKey.TITLE));
            if (title == null) {
                title = file.getFileName().toString().replaceFirst("\\.[^.]+$", "");
            }
            String artist = blankToNull(tag.getFirst(FieldKey.ARTIST));
            if (artist == null) {
                artist = UNKNOWN_ARTIST;
            }

            Song song = new Song();
            song.setId(id);
            song.setTitle(title);
            song.setArtist(artist);
            song.setDuration(audioFile.getAudioHeader().getTrackLength());
            song.setAudioUrl(properties.getAudioPrefix() + "/" + id);
            song.setCoverUrl("/cover/" + id);
            song.setFilePath(file);
            Artwork artwork = tag.getFirstArtwork();
            if (artwork != null && artwork.getBinaryData() != null && artwork.getBinaryData().length > 0) {
                song.setCoverBytes(artwork.getBinaryData());
                song.setCoverMimeType(blankToNull(artwork.getMimeType()));
            }

            if (properties.isScanLogEnabled()) {
                log.info("Loaded #{} - {} / {}", id, title, artist);
            }
            return Optional.of(song);
        } catch (Exception ex) {
            log.warn("Skip invalid audio file: {}", file, ex);
            return Optional.empty();
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
