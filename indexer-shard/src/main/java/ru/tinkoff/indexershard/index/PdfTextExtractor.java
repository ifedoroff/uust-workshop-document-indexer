package ru.tinkoff.indexershard.index;

import lombok.SneakyThrows;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class PdfTextExtractor implements TextExtractor {

    @Override
    @SneakyThrows
    public String extract(InputStream is) {
        return new PDFTextStripper().getText(PDDocument.load(is));
    }

    @Override
    public boolean supports(String contentType) {
        return "application/pdf".equals(contentType);
    }
}
