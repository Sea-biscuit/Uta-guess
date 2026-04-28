package com.utawaaa.guesssong.controller;

import com.utawaaa.guesssong.model.Song;
import com.utawaaa.guesssong.service.SongService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class CoverController {
    private final SongService songService;

    public CoverController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping("/cover/{id}")
    public ResponseEntity<byte[]> cover(@PathVariable("id") long id) {
        Optional<Song> songOpt = songService.findById(id);
        if (songOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Song song = songOpt.get();
        byte[] coverBytes = song.getCoverBytes();
        if (coverBytes == null || coverBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        String mimeType = song.getCoverMimeType() == null ? MediaType.IMAGE_JPEG_VALUE : song.getCoverMimeType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .contentLength(coverBytes.length)
                .body(coverBytes);
    }
}
