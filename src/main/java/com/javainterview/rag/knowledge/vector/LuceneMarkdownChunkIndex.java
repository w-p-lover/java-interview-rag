package com.javainterview.rag.knowledge.vector;

import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownChunkIndexer;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 使用 Lucene 倒排索引提供 BM25 稀疏检索。 */
public final class LuceneMarkdownChunkIndex implements MarkdownChunkIndexer, AutoCloseable {

    private static final String ID_FIELD = "id";
    private static final String SOURCE_FIELD = "source";
    private static final String VERSION_FIELD = "document_version";
    private static final String CHUNK_ORDER_FIELD = "chunk_order";
    private static final String SECTION_ORDER_FIELD = "section_order";
    private static final String HEADING_LEVEL_FIELD = "heading_level";
    private static final String HEADING_PATH_FIELD = "heading_path";
    private static final String HEADING_TEXT_FIELD = "heading_text";
    private static final String CONTENT_FIELD = "content";
    private static final String CODE_FIELD = "code";
    private static final String JAVA_IDENTIFIER_FIELD = "java_identifier";
    private static final String PAGE_NUMBER_FIELD = "page_number";
    private static final String BLOCK_TYPE_FIELD = "block_type";
    private static final String HEADING_SEPARATOR = "\u001f";
    private static final Pattern JAVA_IDENTIFIER_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9_$])[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)*(?![A-Za-z0-9_$])");
    private static final Map<String, Float> FIELD_BOOSTS = Map.of(
            CONTENT_FIELD, 1.0f,
            HEADING_TEXT_FIELD, 2.0f,
            CODE_FIELD, 2.5f,
            JAVA_IDENTIFIER_FIELD, 3.0f);
    public static final String FIELD_WEIGHT_POLICY =
            "content=1.0, heading=2.0, code=2.5, java_identifier=3.0";

    private final Directory directory;
    private final Analyzer analyzer;
    private final IndexWriter writer;

    public LuceneMarkdownChunkIndex(Directory directory) {
        this.directory = Objects.requireNonNull(directory, "Lucene 索引目录不能为空");
        this.analyzer = new StandardAnalyzer();
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer)
                    .setSimilarity(new BM25Similarity());
            this.writer = new IndexWriter(directory, config);
        } catch (IOException exception) {
            closeQuietly();
            throw new IllegalStateException("初始化 BM25 索引失败", exception);
        }
    }

    @Override
    public synchronized void index(MarkdownDocument document, List<MarkdownChunk> chunks) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");
        try {
            for (MarkdownChunk chunk : chunks) {
                writer.updateDocument(new Term(ID_FIELD, stableDocumentId(document, chunk)),
                        toLuceneDocument(document, chunk));
            }
            writer.commit();
        } catch (IOException exception) {
            throw new IllegalStateException("写入 BM25 索引失败", exception);
        }
    }

    @Override
    public synchronized void rebuild(MarkdownDocument document, List<MarkdownChunk> chunks) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");
        try {
            writer.deleteDocuments(new Term(SOURCE_FIELD, document.source()));
            for (MarkdownChunk chunk : chunks) {
                writer.addDocument(toLuceneDocument(document, chunk));
            }
            writer.commit();
            deleteOtherVersionsInternal(document);
            writer.commit();
        } catch (IOException exception) {
            throw new IllegalStateException("重建 BM25 索引失败", exception);
        }
    }

    @Override
    public synchronized void deleteOtherVersions(MarkdownDocument document) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        try {
            deleteOtherVersionsInternal(document);
            writer.commit();
        } catch (IOException exception) {
            throw new IllegalStateException("清理 BM25 历史版本失败", exception);
        }
    }

    @Override
    public synchronized void deleteBySource(String source) {
        Objects.requireNonNull(source, "文档来源不能为空");
        try {
            writer.deleteDocuments(new Term(SOURCE_FIELD, source));
            writer.commit();
        } catch (IOException exception) {
            throw new IllegalStateException("删除 BM25 来源失败", exception);
        }
    }

    public synchronized List<MarkdownSearchResult> search(String query, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("检索问题不能为空");
        }
        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException("topK 必须在 1 到 20 之间");
        }
        try (DirectoryReader reader = DirectoryReader.open(writer)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());
            MultiFieldQueryParser parser = new MultiFieldQueryParser(
                    FIELD_BOOSTS.keySet().toArray(String[]::new), analyzer, FIELD_BOOSTS);
            parser.setDefaultOperator(MultiFieldQueryParser.Operator.OR);
            Query parsedQuery;
            try {
                parsedQuery = parser.parse(query);
            } catch (ParseException exception) {
                throw new IllegalArgumentException("检索问题包含无法解析的关键词", exception);
            }
            TopDocs topDocs = searcher.search(parsedQuery, topK);
            List<MarkdownSearchResult> results = new ArrayList<>(topDocs.scoreDocs.length);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                results.add(toSearchResult(searcher.doc(scoreDoc.doc), scoreDoc.score));
            }
            return List.copyOf(results);
        } catch (IOException exception) {
            throw new IllegalStateException("执行 BM25 检索失败", exception);
        }
    }

    @Override
    public synchronized void close() {
        try {
            writer.close();
        } catch (IOException exception) {
            throw new IllegalStateException("关闭 BM25 索引失败", exception);
        } finally {
            try {
                analyzer.close();
                directory.close();
            } catch (IOException exception) {
                throw new IllegalStateException("关闭 BM25 索引目录失败", exception);
            }
        }
    }

    private void deleteOtherVersionsInternal(MarkdownDocument document) throws IOException {
        writer.deleteDocuments(new org.apache.lucene.search.BooleanQuery.Builder()
                .add(new org.apache.lucene.search.TermQuery(new Term(SOURCE_FIELD, document.source())),
                        org.apache.lucene.search.BooleanClause.Occur.MUST)
                .add(new org.apache.lucene.search.TermQuery(new Term(VERSION_FIELD, document.version())),
                        org.apache.lucene.search.BooleanClause.Occur.MUST_NOT)
                .build());
    }

    private Document toLuceneDocument(MarkdownDocument document, MarkdownChunk chunk) {
        Document result = new Document();
        result.add(new StringField(ID_FIELD, stableDocumentId(document, chunk), Field.Store.YES));
        result.add(new StringField(SOURCE_FIELD, document.source(), Field.Store.YES));
        result.add(new StringField(VERSION_FIELD, document.version(), Field.Store.YES));
        result.add(new IntField(CHUNK_ORDER_FIELD, chunk.order(), Field.Store.YES));
        result.add(new StoredField(SECTION_ORDER_FIELD, chunk.sectionOrder()));
        result.add(new StoredField(HEADING_LEVEL_FIELD, chunk.headingLevel()));
        result.add(new TextField(HEADING_TEXT_FIELD, String.join(" ", chunk.headingPath()), Field.Store.NO));
        result.add(new StoredField(HEADING_PATH_FIELD, String.join(HEADING_SEPARATOR, chunk.headingPath())));
        result.add(new TextField(CONTENT_FIELD, chunk.content(), Field.Store.YES));
        result.add(new TextField(CODE_FIELD, codeText(chunk), Field.Store.NO));
        result.add(new TextField(JAVA_IDENTIFIER_FIELD,
                extractJavaIdentifiers(chunk), Field.Store.NO));
        if (chunk.pageNumber() != null) {
            result.add(new StoredField(PAGE_NUMBER_FIELD, chunk.pageNumber()));
        }
        result.add(new StoredField(BLOCK_TYPE_FIELD, chunk.blockType().name()));
        return result;
    }

    private MarkdownSearchResult toSearchResult(Document document, float score) {
        Integer pageNumber = optionalIntegerValue(document, PAGE_NUMBER_FIELD);
        return new MarkdownSearchResult(
                Objects.requireNonNull(document.get(CONTENT_FIELD), "BM25 结果正文不能为空"),
                required(document, SOURCE_FIELD),
                required(document, VERSION_FIELD),
                integerValue(document, CHUNK_ORDER_FIELD),
                splitHeadingPath(required(document, HEADING_PATH_FIELD)),
                (double) score,
                pageNumber,
                com.javainterview.rag.knowledge.document.KnowledgeBlockType.valueOf(
                        required(document, BLOCK_TYPE_FIELD)));
    }

    private String required(Document document, String field) {
        return Objects.requireNonNull(document.get(field), "BM25 结果缺少元数据：" + field);
    }

    private int integerValue(Document document, String field) {
        org.apache.lucene.index.IndexableField value = document.getField(field);
        if (value == null || value.numericValue() == null) {
            throw new IllegalStateException("BM25 结果缺少数值元数据：" + field);
        }
        return value.numericValue().intValue();
    }

    private Integer optionalIntegerValue(Document document, String field) {
        org.apache.lucene.index.IndexableField value = document.getField(field);
        return value == null || value.numericValue() == null ? null : value.numericValue().intValue();
    }

    private List<String> splitHeadingPath(String value) {
        return value.isEmpty() ? List.of() : List.of(value.split(HEADING_SEPARATOR, -1));
    }

    private String stableDocumentId(MarkdownDocument document, MarkdownChunk chunk) {
        return document.source() + "\u0000" + document.version() + "\u0000" + chunk.order();
    }

    private String codeText(MarkdownChunk chunk) {
        return chunk.blockType() == com.javainterview.rag.knowledge.document.KnowledgeBlockType.CODE
                ? chunk.content() : "";
    }

    private String extractJavaIdentifiers(MarkdownChunk chunk) {
        String searchableText = String.join(" ", chunk.headingPath()) + " " + chunk.content();
        Matcher matcher = JAVA_IDENTIFIER_PATTERN.matcher(searchableText);
        StringBuilder identifiers = new StringBuilder();
        while (matcher.find()) {
            if (identifiers.length() > 0) {
                identifiers.append(' ');
            }
            identifiers.append(matcher.group());
        }
        return identifiers.toString();
    }

    private void closeQuietly() {
        try {
            analyzer.close();
            directory.close();
        } catch (IOException ignored) {
            // 初始化失败时不覆盖原始异常。
        }
    }
}
