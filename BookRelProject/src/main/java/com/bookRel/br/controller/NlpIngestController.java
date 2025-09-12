package com.bookRel.br.controller;

import com.bookRel.br.service.NlpIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/nlp")
@RequiredArgsConstructor
public class NlpIngestController {

    private final NlpIngestService service;

    /** 예: POST /api/nlp/ingestUrl  { "bookId":1, "url":"https://www.gutenberg.org/ebooks/1342.txt.utf-8" } */
    @PostMapping("/ingestUrl")
    public Map<String,Object> ingestUrl(@RequestBody Map<String,Object> req) {
        Long bookId = ((Number)req.get("bookId")).longValue();
        String url   = (String) req.get("url");
        return service.ingestUrl(bookId, url);
    }
}
