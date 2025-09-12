package com.bookRel.br;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Runner implements CommandLineRunner {
    private final Neo4jClient neo4jClient;

    @Override
    public void run(String... args) {
        String version = neo4jClient.query("""
                CALL dbms.components() YIELD name, versions
                RETURN head(versions) AS v
                LIMIT 1
                """)
            .fetchAs(String.class)
            .one()   // 이제 1행만 돌아옴
            .orElse("unknown");

        System.out.println("[Neo4j] connected: version=" + version);
    }
}
