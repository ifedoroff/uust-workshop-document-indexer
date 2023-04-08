package ru.tinkoff.indexershard.index;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class PlainTextExtractor implements TextExtractor {
    @Override
    @SneakyThrows
    public String extract(InputStream is) {
        return IOUtils.toString(is, StandardCharsets.UTF_8);
    }

    @Override
    public boolean supports(String contentType) {
        return List.of("application/json", "text/plain")
                .contains(contentType);
    }
}
