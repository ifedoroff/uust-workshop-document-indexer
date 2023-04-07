package ru.tinkoff.indexershard;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import ru.tinkoff.common.SearchResult;
import ru.tinkoff.common.ShardInfo;
import ru.tinkoff.indexershard.index.Storage;


import java.net.InetAddress;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class Controller {

    private final Storage storage;
    private String shardName;
    private String host;
    private Integer port;

    @Autowired
    @SneakyThrows
    public Controller(Storage storage,
                      @Value("${application.shard.name:default_shard_#{T(java.util.UUID).randomUUID().toString()}}") String shardName,
                      @Value("${server.port}") String port) {
        this.storage = storage;
        this.shardName = shardName;
        this.host = InetAddress.getLocalHost().getHostAddress();
        this.port = Integer.valueOf(port);
    }

    @RequestMapping(path = "/index", method = RequestMethod.POST)
    public String indexDocument(@RequestParam("file") MultipartFile file) {
        storage.indexDocument(file);
        return "Ok";
    }

    @RequestMapping(path = "search", method = GET)
    public Mono<SearchResult> search(@RequestParam("query") String query) {
        return Mono.just(storage.query(query, 20));
    }

    @RequestMapping(path = "info", method = GET)
    Mono<ShardInfo> info() {
        var shardName = this.shardName;
        return Mono.just(new ShardInfo(shardName, host, port, storage.documentCount()));
    }

}
