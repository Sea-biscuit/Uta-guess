package com.utawaaa.guesssong.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "guess-song")
public class AppProperties {
    private String musicFolder = "music";
    private String format = "flac";
    private String audioPrefix = "/audio";
    private boolean rescanOnStartup = true;
    private int defaultClipSeconds = 10;
    private boolean scanLogEnabled = true;

    public String getMusicFolder() {
        return musicFolder;
    }

    public void setMusicFolder(String musicFolder) {
        this.musicFolder = musicFolder;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getAudioPrefix() {
        return audioPrefix;
    }

    public void setAudioPrefix(String audioPrefix) {
        this.audioPrefix = audioPrefix;
    }

    public boolean isRescanOnStartup() {
        return rescanOnStartup;
    }

    public void setRescanOnStartup(boolean rescanOnStartup) {
        this.rescanOnStartup = rescanOnStartup;
    }

    public int getDefaultClipSeconds() {
        return defaultClipSeconds;
    }

    public void setDefaultClipSeconds(int defaultClipSeconds) {
        this.defaultClipSeconds = defaultClipSeconds;
    }

    public boolean isScanLogEnabled() {
        return scanLogEnabled;
    }

    public void setScanLogEnabled(boolean scanLogEnabled) {
        this.scanLogEnabled = scanLogEnabled;
    }
}
