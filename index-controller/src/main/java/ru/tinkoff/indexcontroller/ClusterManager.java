package ru.tinkoff.indexcontroller;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.tinkoff.common.ShardInfo;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClusterManager {

    @Component
    @Getter
    @Setter
    @ConfigurationProperties("shard")
    public static class ShardConfiguration {

        private List<String> urls;
    }


    private volatile List<ShardInfo> shards = new ArrayList<>();
    private final List<String> urls;
    private Random random = new Random();

    @Autowired
    public ClusterManager(ShardConfiguration shardConfiguration) {
        this.urls = shardConfiguration.urls;
    }

    @PostConstruct
    public void configure() {
       loadShards();
    }

    @Scheduled(fixedDelay = 2000L)
    public void loadShards() {
        List<Mono<ShardInfo>> results = urls.stream()
                .map(url -> singleShardInfo(url.split(":")[0], Integer.parseInt(url.split(":")[1])))
                .collect(Collectors.toList());
        shards = Mono.zip(results, (array) -> Arrays.stream(array).map(s -> (ShardInfo)s).collect(Collectors.toList()))
                .timeout(Duration.ofSeconds(10))
                .block();
    }

    private Mono<ShardInfo> singleShardInfo(String host, Integer port) {
        return WebClient.create("http://" + host + ":" + port)
                .get()
                .uri(uriBuilder -> uriBuilder.path("info")
                        .build())
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(ShardInfo.class))
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(t -> Mono.just(new ShardInfo("failed", host, port, -1)));
    }

    public List<ShardInfo> allShards() {
        return shards;
    }

    public ShardInfo randomShard() {
        return shards.get(random.nextInt(shards.size()));
    }

}
