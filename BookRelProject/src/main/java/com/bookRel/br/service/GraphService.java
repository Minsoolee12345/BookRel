package com.bookRel.br.service;

import com.bookRel.br.dto.GraphResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GraphService
{

    private final Neo4jClient neo4j;

    /** 샘플 데이터 시드 (bookId=1) */
    public Map<String, Object> seed() {
        String cypher = """
            MERGE (a:Character {bookId:1, name:"홍길동"})
              ON CREATE SET a.appId = randomUUID()
            MERGE (b:Character {bookId:1, name:"임꺽정"})
              ON CREATE SET b.appId = randomUUID()
            MERGE (c:Character {bookId:1, name:"전우치"})
              ON CREATE SET c.appId = randomUUID()
            MERGE (a)-[:RELATES {type:"ALLY",  weight:0.7, fromChapter:1, toChapter:10}]->(b)
            MERGE (b)-[:RELATES {type:"ENEMY", weight:0.6, fromChapter:5}]->(c)
            RETURN 'seeded' AS status
            """;

        String status = neo4j.query(cypher)
                .fetchAs(String.class)
                .first()
                .orElse("unknown");

        return Map.of("status", status);
    }
    
    public GraphResponse getGraph(Long bookId, Integer fromChap, Integer toChap, Double minWeight, Integer limit) {
        // 1) 기존 조회 재사용
        GraphResponse base = getGraph(bookId, fromChap, toChap);

        var nodes = new java.util.ArrayList<>(base.getNodes());
        var edges = new java.util.ArrayList<>(base.getEdges());

        // 2) minWeight 필터 (null은 0으로 취급)
        if (minWeight != null) {
            edges.removeIf(e -> ((e.getWeight() == null ? 0d : e.getWeight()) < minWeight));
        }

        // 3) limit 상위 N개 (weight 내림차순 정렬, null=0)
        if (limit != null && limit > 0 && edges.size() > limit) {
            edges.sort((a, b) -> Double.compare(
                    (b.getWeight() == null ? 0d : b.getWeight()),
                    (a.getWeight() == null ? 0d : a.getWeight())
            ));
            edges = new java.util.ArrayList<>(edges.subList(0, limit));

            // 4) 사용되지 않는 노드 제거(선택) → 시각화 깔끔
            var keep = new java.util.HashSet<String>();
            for (var e : edges) { keep.add(e.getSrc()); keep.add(e.getDst()); }
            nodes.removeIf(n -> !keep.contains(n.getId()));
        }

        return GraphResponse.builder()
                .nodes(nodes)
                .edges(edges)
                .build();
    }
    
    /** 그래프 조회 (선택 필터: fromChapter/toChapter) */
    public GraphResponse getGraph(Long bookId, Integer fromChap, Integer toChap) {
        String cypher = """
            MATCH (a:Character {bookId:$bookId})-[r:RELATES]->(b:Character {bookId:$bookId})
            WHERE ($fromChap IS NULL OR r.toChapter IS NULL OR r.toChapter >= $fromChap)
              AND ($toChap   IS NULL OR r.fromChapter <= $toChap)
            RETURN toString(a.appId) AS aId, a.name AS aName,
                   toString(b.appId) AS bId, b.name AS bName,
                   r.type AS type, r.weight AS weight,
                   r.fromChapter AS fromChapter, r.toChapter AS toChapter
            """;

        var rows = neo4j.query(cypher)
                .bind(bookId).to("bookId")
                .bind(fromChap).to("fromChap")
                .bind(toChap).to("toChap")
                .fetch().all();

        Map<String, GraphResponse.Node> nodeMap = new LinkedHashMap<>();
        List<GraphResponse.Edge> edges = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            String aId = (String) row.get("aId");
            String bId = (String) row.get("bId");
            String aName = (String) row.get("aName");
            String bName = (String) row.get("bName");

            nodeMap.putIfAbsent(aId, GraphResponse.Node.builder().id(aId).name(aName).build());
            nodeMap.putIfAbsent(bId, GraphResponse.Node.builder().id(bId).name(bName).build());

            Number wNum = (Number) row.get("weight");
            Double weight = (wNum == null) ? null : wNum.doubleValue();

            Number fromNum = (Number) row.get("fromChapter");
            Integer from = (fromNum == null) ? null : fromNum.intValue();

            Number toNum = (Number) row.get("toChapter");
            Integer to = (toNum == null) ? null : toNum.intValue();

            edges.add(
                GraphResponse.Edge.builder()
                    .src(aId).dst(bId)
                    .type((String) row.get("type"))
                    .weight(weight)
                    .fromChapter(from)
                    .toChapter(to)
                    .build()
            );
        }

        return GraphResponse.builder()
                .nodes(new ArrayList<>(nodeMap.values()))
                .edges(edges)
                .build();
    }
    
    /**
     * 진행도(0~1)와 총 장 수를 받아 from/to 장 범위를 계산하여 그래프를 반환.
     * window: 최근 몇 장을 볼지(기본 10장)
     */
    public GraphResponse snapshot(Long bookId, Double progress, Integer totalChapters, Integer window) {
        if (bookId == null) throw new IllegalArgumentException("bookId is required");
        if (totalChapters == null || totalChapters <= 0) throw new IllegalArgumentException("totalChapters must be > 0");

        // progress 정규화
        double p = (progress == null || Double.isNaN(progress) || Double.isInfinite(progress)) ? 1.0 : progress;
        if (p < 0) p = 0.0;
        if (p > 1) p = 1.0;

        int w = (window == null || window <= 0) ? 10 : window;

        // to = ceil(progress * totalChapters), [1, totalChapters]로 클램프
        int to = (int) Math.ceil(p * totalChapters);
        if (to < 1) to = 1;
        if (to > totalChapters) to = totalChapters;

        // from = max(1, to - (w-1))
        int from = Math.max(1, to - w + 1);

        return getGraph(bookId, from, to);
    }
}