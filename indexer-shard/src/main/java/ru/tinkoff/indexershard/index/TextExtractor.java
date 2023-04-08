package ru.tinkoff.indexershard.index;

import java.io.InputStream;

public interface TextExtractor {
    String extract(InputStream is);
    boolean supports(String contentType);
}
