package com.utawaaa.guesssong.model;

public record SongDto(long id, String title, String artist, double duration, String audioUrl, String coverUrl) {
    public static SongDto from(Song song) {
        return new SongDto(
                song.getId(),
                song.getTitle(),
                song.getArtist(),
                song.getDuration(),
                song.getAudioUrl(),
                song.getCoverUrl()
        );
    }
}
