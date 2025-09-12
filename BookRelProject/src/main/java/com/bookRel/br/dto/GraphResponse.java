package com.bookRel.br.dto;

import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GraphResponse {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Node {
        private String id;    // elementId() -> String
        private String name;  // Character.name
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Edge {
        private String src;       // elementId(a)
        private String dst;       // elementId(b)
        private String type;      // RELATES.type
        private Double weight;    // RELATES.weight
        private Integer fromChapter; // RELATES.fromChapter
        private Integer toChapter;   // RELATES.toChapter (null == 현재까지)
    }

    @Builder.Default
    private List<Node> nodes = new ArrayList<>();

    @Builder.Default
    private List<Edge> edges = new ArrayList<>();
}
