package com.utawaaa.guesssong.controller;

import com.utawaaa.guesssong.model.Song;
import com.utawaaa.guesssong.service.SongService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

@RestController
public class AudioController {
    private final SongService songService;

    public AudioController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping("/audio/{id}")
    public ResponseEntity<Resource> streamAudio(
            @PathVariable("id") long id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader
    ) throws IOException {
        Optional<Song> songOpt = songService.findById(id);
        if (songOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Song song = songOpt.get();
        Resource resource = new FileSystemResource(song.getFilePath());
        long contentLength = resource.contentLength();

        if (rangeHeader == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("audio/flac"))
                    .contentLength(contentLength)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        }

        List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
        if (ranges.isEmpty()) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        }

        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(contentLength);
        long end = range.getRangeEnd(contentLength);
        if (start >= contentLength || start > end) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                    .build();
        }
        long rangeLength = end - start + 1;

        InputStream inputStream = Files.newInputStream(song.getFilePath());
        skipFully(inputStream, start);
        Resource rangedResource = new InputStreamResource(new LimitedInputStream(inputStream, rangeLength));

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.valueOf("audio/flac"))
                .contentLength(rangeLength)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + contentLength)
                .body(rangedResource);
    }

    private void skipFully(InputStream inputStream, long bytesToSkip) throws IOException {
        long remaining = bytesToSkip;
        while (remaining > 0) {
            long skipped = inputStream.skip(remaining);
            if (skipped <= 0) {
                if (inputStream.read() == -1) {
                    throw new IOException("Unexpected EOF while skipping bytes");
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }

    private static final class LimitedInputStream extends InputStream {
        private final InputStream delegate;
        private long remaining;

        private LimitedInputStream(InputStream delegate, long remaining) {
            this.delegate = delegate;
            this.remaining = remaining;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = delegate.read();
            if (value != -1) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int read = delegate.read(b, off, (int) Math.min(len, remaining));
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
