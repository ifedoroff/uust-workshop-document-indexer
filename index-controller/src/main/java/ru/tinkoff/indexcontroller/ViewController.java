package ru.tinkoff.indexcontroller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import reactor.core.publisher.Mono;
import ru.tinkoff.common.SearchResult;
import ru.tinkoff.common.ShardInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class ViewController {

    private ClusterManager clusterManager;

    @Autowired
    public ViewController(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    @GetMapping("/main")
    public Mono<String> main(Model model) {
        if(model.getAttribute("result") == null) {
            model.addAttribute("result", new SearchResult());
        }
        model.addAttribute("shards", clusterManager.allShards());
        return Mono.just("main");
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/main";
    }

    @PostMapping(path = "/index")
    public Mono<String> indexDocument(@RequestParam("file") MultipartFile file, RedirectAttributes model) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder
                .part("file", file.getResource())
                .filename(file.getOriginalFilename())
                .contentType(MediaType.parseMediaType(file.getContentType()));
        ShardInfo shard = clusterManager.randomShard();
        return WebClient.create()
                .post()
                .uri("http://" + shard.getIp() + ":" + shard.getPort() + "/index")
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> {
                    recordException(model, e);
                })
                .onErrorResume((t) -> Mono.just("redirect:/main"))
                .map((responseEntity) -> "redirect:/main");
    }

    @GetMapping(path = "/search", produces = "application/json")
    public Mono<String> search(@RequestParam("query") String query, RedirectAttributes model) {
        return Mono.zip(clusterManager.allShards().stream()
                .map(shard -> querySingleShard(shard, query).doOnError(e -> recordException(model, e)))
                .collect(Collectors.toList()), array -> {
            List<SearchResult.Document> documents = Arrays.stream(array)
                    .map(o -> (SearchResult) o)
                    .map(SearchResult::getDocuments)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            model.addFlashAttribute("result", new SearchResult(documents));
            return "redirect:/main";
        })
        .doOnError(e -> {
            recordException(model, e);
        })
        .onErrorResume((t) -> Mono.just("redirect:/main"));
    }

    private void recordException(RedirectAttributes attributes, Throwable e) {
        List exceptions = (List) attributes.getFlashAttributes().get("exceptions");
        if(exceptions == null) {
            exceptions = new ArrayList<>();
        }
        if(exceptions.size() <= 5) {
            exceptions.add(e);
        }
        attributes.addFlashAttribute("exceptions", exceptions);
    }

    private Mono<SearchResult> querySingleShard(ShardInfo shard, String query) {
        return
                WebClient.create("http://" + shard.getIp() + ":" + shard.getPort())
                        .get()
                        .uri(uriBuilder -> uriBuilder.path("search").queryParam("query", "{query}")
                                .build(query))
                        .exchangeToMono(clientResponse -> clientResponse.bodyToMono(SearchResult.class));
    }

    @ExceptionHandler
    public String handleError(HttpServletRequest req, Exception ex, RedirectAttributes attributes) {
        log.error("Request: " + req.getRequestURL() + " raised " + ex);

        attributes.addFlashAttribute("exception", ex);
        return "redirect:/main";
    }

}
