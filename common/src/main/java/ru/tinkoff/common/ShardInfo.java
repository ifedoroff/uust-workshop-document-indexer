package ru.tinkoff.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ShardInfo {

    private String name;
    private String ip;
    private Integer port;
    private Integer documentCount;

    public String print() {
        return "name: " + name + "; " +
         "ip: " + ip + "; " +
         "port: " + port + "; " +
         "document count: " + documentCount + "; ";
    }
}
