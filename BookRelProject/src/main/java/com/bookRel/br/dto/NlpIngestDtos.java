package com.bookRel.br.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)  // 유틸/네임스페이스 용: 인스턴스화 방지
public final class NlpIngestDtos {

    // ↓↓↓ 기존 내부 클래스들은 그대로 두면 됨
    @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor @lombok.AllArgsConstructor @lombok.Builder
    public static class IngestUrlReq {
        private Long bookId;
        private String url;
    }

    @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor @lombok.AllArgsConstructor @lombok.Builder
    public static class NlpGraphResponse {
        private java.util.List<Node> nodes;
        private java.util.List<Edge> edges;

        @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor @lombok.AllArgsConstructor @lombok.Builder
        public static class Node { private String id; private String name; }

        @lombok.Getter @lombok.Setter @lombok.NoArgsConstructor @lombok.AllArgsConstructor @lombok.Builder
        public static class Edge {
            private String src; private String dst;
            private String type; private Double weight;
            private Integer fromChapter; private Integer toChapter;
        }
    }
}
