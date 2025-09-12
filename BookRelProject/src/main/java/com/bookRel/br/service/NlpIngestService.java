package com.bookRel.br.service;

import com.bookRel.br.dto.NlpIngestDtos.IngestUrlReq;
import com.bookRel.br.dto.NlpIngestDtos.NlpGraphResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NlpIngestService {

    private final WebClient nlpClient;   // WebClientConfig 에서 baseUrl=http://localhost:8001
    private final Neo4jClient neo4j;

    /** 1) NLP에 URL을 보내 그래프 생성 결과를 받는다 */
    public NlpGraphResponse fetchFromNlp(Long bookId, String url) {
        IngestUrlReq req = IngestUrlReq.builder().bookId(bookId).url(url).build();

        return nlpClient.post()
                .uri("/ingest/url")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(NlpGraphResponse.class)
                .onErrorResume(e -> Mono.error(new RuntimeException("NLP 호출 실패: " + e.getMessage(), e)))
                .block(); // 간단히 동기 호출
    }

    /** 2) NLP 결과를 Neo4j에 업서트(병합) */
    public Map<String, Object> upsertToNeo4j(Long bookId, NlpGraphResponse gr) {
        if (gr == null || gr.getNodes() == null) {
            throw new IllegalArgumentException("NLP 결과가 비어있음");
        }

        // id -> name 매핑(엣지는 id로 왔으므로 이름으로 변환할 것)
        Map<String, String> idToName = gr.getNodes().stream()
                .collect(Collectors.toMap(NlpGraphResponse.Node::getId, NlpGraphResponse.Node::getName, (a,b)->a, LinkedHashMap::new));

        // 2-1) 노드 이름 리스트
        List<String> names = gr.getNodes().stream()
                .map(NlpGraphResponse.Node::getName)
                .distinct()
                .collect(Collectors.toList());

        // 2-2) 엣지 파라미터 (srcName/dstName으로 변환)
        List<Map<String,Object>> rels = new ArrayList<>();
        if (gr.getEdges() != null) {
            for (var e : gr.getEdges()) {
                String srcName = idToName.get(e.getSrc());
                String dstName = idToName.get(e.getDst());
                if (srcName == null || dstName == null) continue;

                Map<String,Object> m = new LinkedHashMap<>();
                m.put("srcName", srcName);
                m.put("dstName", dstName);
                m.put("type", Optional.ofNullable(e.getType()).orElse("CO_OCCUR"));
                m.put("weight", e.getWeight());
                m.put("fromChapter", e.getFromChapter());
                m.put("toChapter", e.getToChapter());
                rels.add(m);
            }
        }

        // 2-3) Cypher: 노드 업서트
        String cyNodes = """
            UNWIND $names AS name
            MERGE (c:Character {bookId:$bookId, name:name})
              ON CREATE SET c.appId = randomUUID()
            """;

        neo4j.query(cyNodes)
                .bind(bookId).to("bookId")
                .bind(names).to("names")
                .run();

        // 2-4) Cypher: 관계 업서트
        String cyEdges = """
            UNWIND $rels AS rel
            MATCH (a:Character {bookId:$bookId, name: rel.srcName})
            MATCH (b:Character {bookId:$bookId, name: rel.dstName})
            MERGE (a)-[r:RELATES {type: rel.type, fromChapter: rel.fromChapter, toChapter: rel.toChapter}]->(b)
            SET r.weight = rel.weight
            """;

        neo4j.query(cyEdges)
                .bind(bookId).to("bookId")
                .bind(rels).to("rels")
                .run();

        return Map.of(
                "bookId", bookId,
                "nodesUpserted", names.size(),
                "edgesUpserted", rels.size()
        );
    }

    /** URL 하나로 전체 파이프라인: NLP 호출 → Neo4j 업서트 */
    public Map<String,Object> ingestUrl(Long bookId, String url){
        var gr = fetchFromNlp(bookId, url);
        return upsertToNeo4j(bookId, gr);
    }
}
