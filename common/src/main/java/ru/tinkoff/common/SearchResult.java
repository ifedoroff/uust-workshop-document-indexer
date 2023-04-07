package ru.tinkoff.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchResult {

    private List<Document> documents = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
        private String documentName;
        private String contentType;
        private String shardName;

        public String print() {
            return "document: " + documentName + "; " +
                    "type: " + contentType + "; " +
                    "shard: " + shardName + ";";
        }
    }
}
