package com.utawaaa.guesssong.controller;

import com.utawaaa.guesssong.model.SongDto;
import com.utawaaa.guesssong.service.SongService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/songs")
public class SongController {
    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping
    public List<SongDto> songs() {
        return songService.listSongs().stream().map(SongDto::from).toList();
    }
}
