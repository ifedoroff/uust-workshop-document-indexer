package ru.tinkoff.indexershard.index;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StandardDirectoryReader;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FSLockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.tinkoff.common.SearchResult;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class Storage {

    private IndexWriter index;
    private String shardName;
    private Map<String, TextExtractor> textExtractors;
    private PlainTextExtractor defaultTextExtractor;

    @Autowired
    public Storage(@Value("${application.shard.name:default_shard_#{T(java.util.UUID).randomUUID().toString()}}") String shardName,
                   Map<String, TextExtractor> textExtractors,
                   PlainTextExtractor defaultTextExtractor) {
        this.shardName = shardName;
        this.textExtractors = textExtractors;
        this.defaultTextExtractor = defaultTextExtractor;
    }

    @PostConstruct
    void open() throws IOException {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setCodec(new SimpleTextCodec());
        index = new IndexWriter(FSDirectory.open(Paths.get("document_index_" + shardName), FSLockFactory.getDefault()), config);
    }

    @PreDestroy()
    void close() {
        if(index != null) {
            try {
                index.close();
            } catch (IOException e) {
                log.error("Unable to close index", e);
            }
        }
    }

    @SneakyThrows
    public int documentCount() {
        return StandardDirectoryReader.open(index).numDocs();
    }

    @SneakyThrows
    public void indexDocument(MultipartFile file) {
        Document document = new Document();
        document.add(new StringField("name", file.getOriginalFilename(), Field.Store.YES));
        document.add(new StringField("mime_type", file.getContentType(), Field.Store.YES));
        document.add(new StringField("shard_name", shardName, Field.Store.YES));
        TextExtractor textExtractor = textExtractors.values()
                .stream()
                .filter(extractor -> extractor.supports(file.getContentType()))
                .findFirst()
                .orElse(defaultTextExtractor);
        String text = textExtractor.extract(file.getInputStream());
        StandardTokenizer tokenizer = new StandardTokenizer();
        tokenizer.setReader(new StringReader(text));
        LowerCaseFilter tokenFilter = new LowerCaseFilter(tokenizer);
        document.add(new TextField("content", tokenFilter));
        index.addDocument(document);
        index.commit();
    }

    @SneakyThrows
    public SearchResult query(String queryString, int limit) {
        Query query = new StandardQueryParser(new StandardAnalyzer()).parse(queryString.toLowerCase(), "content");
        IndexSearcher searcher = new IndexSearcher(StandardDirectoryReader.open(index, true, true));
        TopDocs search = searcher.search(query, limit);
        List<SearchResult.Document> documents = Arrays.stream(search.scoreDocs)
                .map(topDocument -> {
                    try {
                        Document luceneDocument = searcher.getIndexReader().storedFields().document(topDocument.doc);
                        return SearchResult.Document.builder()
                                .documentName(luceneDocument.get("name"))
                                .contentType(luceneDocument.get("mime_type"))
                                .shardName(shardName)
                                .build();
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to read documents", e);
                    }
                })
                .collect(Collectors.toList());
        return new SearchResult(documents);
    }
}
