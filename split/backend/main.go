package main

import (
	"bytes"
	"encoding/json"
	"flag"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"
)

func main() {
	music := flag.String("music", "./music", "FLAC library directory")
	port := flag.String("port", "8080", "local HTTP port")
	flag.Parse()

	library := scanLibrary(*music)
	server := &http.Server{
		Addr:              "127.0.0.1:" + *port,
		Handler:           library.handler(),
		ReadHeaderTimeout: 5 * time.Second,
	}
	log.Printf("Loaded %d song(s). Listening on http://%s", len(library.songs), server.Addr)
	log.Fatal(server.ListenAndServe())
}

func (l library) handler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /api/songs", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		if err := json.NewEncoder(w).Encode(l.songs); err != nil {
			log.Printf("Write song list: %v", err)
		}
	})
	mux.HandleFunc("GET /audio/{id}", func(w http.ResponseWriter, r *http.Request) {
		track, ok := l.lookup(r)
		if !ok {
			http.NotFound(w, r)
			return
		}
		file, err := os.Open(track.path)
		if err != nil {
			if os.IsNotExist(err) {
				http.NotFound(w, r)
				return
			}
			log.Printf("Open audio %q: %v", track.path, err)
			http.Error(w, "Unable to open audio", http.StatusInternalServerError)
			return
		}
		defer file.Close()
		info, err := file.Stat()
		if err != nil || !info.Mode().IsRegular() {
			http.NotFound(w, r)
			return
		}
		w.Header().Set("Content-Type", "audio/flac")
		http.ServeContent(w, r, info.Name(), info.ModTime(), file)
	})
	mux.HandleFunc("GET /cover/{id}", func(w http.ResponseWriter, r *http.Request) {
		track, ok := l.lookup(r)
		if !ok || len(track.cover) == 0 {
			http.NotFound(w, r)
			return
		}
		w.Header().Set("Content-Type", http.DetectContentType(track.cover))
		http.ServeContent(w, r, "cover", time.Time{}, bytes.NewReader(track.cover))
	})
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Add("Vary", "Origin")
		switch origin := r.Header.Get("Origin"); origin {
		case "http://127.0.0.1:5173", "http://localhost:5173":
			w.Header().Set("Access-Control-Allow-Origin", origin)
		}
		mux.ServeHTTP(w, r)
	})
}

func (l library) lookup(r *http.Request) (song, bool) {
	id, err := strconv.Atoi(r.PathValue("id"))
	if err != nil || id < 1 || id > len(l.songs) {
		return song{}, false
	}
	return l.songs[id-1], true
}
