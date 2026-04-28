package com.utawaaa.guesssong.model;

import java.nio.file.Path;

public class Song {
    private long id;
    private String title;
    private String artist;
    private double duration;
    private String audioUrl;
    private String coverUrl;
    private Path filePath;
    private byte[] coverBytes;
    private String coverMimeType;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public byte[] getCoverBytes() {
        return coverBytes;
    }

    public void setCoverBytes(byte[] coverBytes) {
        this.coverBytes = coverBytes;
    }

    public String getCoverMimeType() {
        return coverMimeType;
    }

    public void setCoverMimeType(String coverMimeType) {
        this.coverMimeType = coverMimeType;
    }
}
