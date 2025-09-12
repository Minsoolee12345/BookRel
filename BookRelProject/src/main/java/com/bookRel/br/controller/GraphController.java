package com.bookRel.br.controller;

import com.bookRel.br.dto.GraphResponse;
import com.bookRel.br.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    /**
     * 샘플 데이터 시드
     * POST /api/graph/seed
     */
    @PostMapping("/seed")
    public Map<String, Object> seed() {
        return graphService.seed();
    }

    /**
     * 그래프 조회
     * GET /api/graph/{bookId}?fromChapter=&toChapter=
     * 예) /api/graph/1
     *     /api/graph/1?fromChapter=1&toChapter=10
     */
    @GetMapping("/{bookId}")
    public GraphResponse getGraph(
            @PathVariable("bookId") Long bookId,                                  // 이름 명시 + Wrapper 타입
            @RequestParam(name = "fromChapter", required = false) Integer fromChapter,
            @RequestParam(name = "toChapter",   required = false) Integer toChapter,
            @RequestParam(name = "minWeight",   required = false) Double minWeight, // ✅ 추가
            @RequestParam(name = "limit",       required = false) Integer limit     // ✅ 추가
    ) {
        return graphService.getGraph(bookId, fromChapter, toChapter);
    }
    
    /**
     * 진행도 기반 스냅샷
     * 예) /api/graph/snapshot?bookId=1&progress=0.42&totalChapters=50&window=10
     * - progress: 0.0 ~ 1.0 (null이면 1.0으로 간주)
     * - totalChapters: 전체 장 수(필수)
     * - window: 최근 몇 장을 보여줄지(기본 10)
     */
    @GetMapping("/snapshot")
    public GraphResponse snapshot(
            @RequestParam("bookId") Long bookId,
            @RequestParam("progress") Double progress,
            @RequestParam("totalChapters") Integer totalChapters,
            @RequestParam(value = "window", required = false) Integer window
    ) {
        return graphService.snapshot(bookId, progress, totalChapters, window);
    }
}