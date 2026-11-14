package main

import (
	"encoding/binary"
	"errors"
	"fmt"
	"io"
	"strings"
)

type flacMetadata struct {
	title    string
	artist   string
	duration float64
	cover    []byte
}

// Read native FLAC metadata blocks as specified in RFC 9639, section 8.
// Audio frames are served directly from disk and decoded by the browser.
func readFLACMetadata(r io.Reader) (flacMetadata, error) {
	var header [4]byte
	if _, err := io.ReadFull(r, header[:]); err != nil {
		return flacMetadata{}, err
	}
	if string(header[:]) != "fLaC" {
		return flacMetadata{}, errors.New("missing FLAC signature")
	}

	var metadata flacMetadata
	for first := true; ; first = false {
		if _, err := io.ReadFull(r, header[:]); err != nil {
			return flacMetadata{}, fmt.Errorf("read metadata header: %w", err)
		}
		kind := header[0] & 0x7f
		size := int(header[1])<<16 | int(header[2])<<8 | int(header[3])
		if first && (kind != 0 || size != 34) {
			return flacMetadata{}, errors.New("missing or invalid FLAC STREAMINFO block")
		}
		if kind == 0 || kind == 4 || (kind == 6 && len(metadata.cover) == 0) {
			block := make([]byte, size)
			if _, err := io.ReadFull(r, block); err != nil {
				return flacMetadata{}, fmt.Errorf("read metadata block %d: %w", kind, err)
			}
			switch kind {
			case 0:
				if !first {
					return flacMetadata{}, errors.New("duplicate FLAC STREAMINFO block")
				}
				packed := binary.BigEndian.Uint64(block[10:18])
				sampleRate := packed >> 44
				if sampleRate == 0 {
					return flacMetadata{}, errors.New("invalid FLAC sample rate")
				}
				samples := packed & ((1 << 36) - 1)
				metadata.duration = float64(samples) / float64(sampleRate)
			case 4:
				metadata.readComments(block)
			case 6:
				metadata.cover = readPicture(block)
			}
		} else if _, err := io.CopyN(io.Discard, r, int64(size)); err != nil {
			return flacMetadata{}, fmt.Errorf("skip metadata block %d: %w", kind, err)
		}
		if header[0]&0x80 != 0 {
			return metadata, nil
		}
	}
}

func (m *flacMetadata) readComments(block []byte) {
	_, rest := readSizedField(block, binary.LittleEndian) // Vendor string.
	if len(rest) < 4 {
		return
	}
	count := binary.LittleEndian.Uint32(rest)
	rest = rest[4:]
	for i := uint32(0); i < count; i++ {
		var field []byte
		field, rest = readSizedField(rest, binary.LittleEndian)
		if field == nil {
			return
		}
		key, value, ok := strings.Cut(string(field), "=")
		if !ok {
			continue
		}
		switch strings.ToUpper(key) {
		case "TITLE":
			if m.title == "" {
				m.title = strings.TrimSpace(value)
			}
		case "ARTIST":
			if m.artist == "" {
				m.artist = strings.TrimSpace(value)
			}
		}
	}
}

func readPicture(block []byte) []byte {
	if len(block) < 4 {
		return nil
	}
	mime, rest := readSizedField(block[4:], binary.BigEndian)
	if string(mime) == "-->" { // Linked picture, with no embedded image bytes.
		return nil
	}
	_, rest = readSizedField(rest, binary.BigEndian) // Description.
	if len(rest) < 16 {
		return nil
	}
	picture, _ := readSizedField(rest[16:], binary.BigEndian) // Skip dimensions, depth and palette size.
	return picture
}

func readSizedField(block []byte, order binary.ByteOrder) (field, rest []byte) {
	if len(block) < 4 {
		return nil, nil
	}
	size := order.Uint32(block)
	block = block[4:]
	if uint64(size) > uint64(len(block)) {
		return nil, nil
	}
	return block[:size], block[size:]
}
