package main

import (
	"io/fs"
	"log"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
)

type song struct {
	ID       int     `json:"id"`
	Title    string  `json:"title"`
	Artist   string  `json:"artist"`
	Duration float64 `json:"duration"`
	AudioURL string  `json:"audioUrl"`
	CoverURL string  `json:"coverUrl"`
	path     string
	cover    []byte
}

type library struct {
	songs []song
}

func scanLibrary(root string) library {
	l := library{songs: make([]song, 0)}
	var paths []string
	err := filepath.WalkDir(root, func(path string, entry fs.DirEntry, err error) error {
		if err != nil {
			if path == root {
				return err
			}
			log.Printf("Skip %q: %v", path, err)
			return nil
		}
		if entry.Type().IsRegular() && strings.EqualFold(filepath.Ext(path), ".flac") {
			paths = append(paths, path)
		}
		return nil
	})
	if err != nil {
		log.Printf("Scan music library %q: %v", root, err)
	}
	sort.Strings(paths)
	for _, path := range paths {
		track, err := loadSong(path, len(l.songs)+1)
		if err != nil {
			log.Printf("Skip invalid FLAC %q: %v", path, err)
			continue
		}
		l.songs = append(l.songs, track)
		log.Printf("Loaded #%d - %s / %s", track.ID, track.Title, track.Artist)
	}
	return l
}

func loadSong(path string, id int) (song, error) {
	file, err := os.Open(path)
	if err != nil {
		return song{}, err
	}
	defer file.Close()

	metadata, err := readFLACMetadata(file)
	if err != nil {
		return song{}, err
	}
	if metadata.title == "" {
		metadata.title = strings.TrimSuffix(filepath.Base(path), filepath.Ext(path))
	}
	if metadata.artist == "" {
		metadata.artist = "未知歌手"
	}
	track := song{
		ID:       id,
		Title:    metadata.title,
		Artist:   metadata.artist,
		Duration: metadata.duration,
		AudioURL: "/audio/" + strconv.Itoa(id),
		path:     path,
		cover:    metadata.cover,
	}
	if len(track.cover) > 0 {
		track.CoverURL = "/cover/" + strconv.Itoa(id)
	}
	return track, nil
}
