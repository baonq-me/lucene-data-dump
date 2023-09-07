package com.blue.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.xcontent.NamedXContentRegistry;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Mapping
// https://stackoverflow.com/questions/58121186/edit-state-file-of-elasticsearch-7-3


public class Main {

    public static void writeFile(String fileName, String contents, boolean append) throws IOException {
        writeFile(fileName, contents.getBytes(), append);
    }

    public static void writeFile(String fileName, String contents) throws IOException {
        writeFile(fileName, contents.getBytes(), false);
    }

    public static void writeFile(String fileName, byte[] contents, boolean append) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(fileName, append);
        outputStream.write(contents);
        outputStream.close();
    }

    // https://stackoverflow.com/questions/880365/any-way-to-invoke-a-private-method
    public static void checkIndex(String indexPath) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = CheckIndex.class.getDeclaredMethod("doMain", String[].class);
        method.setAccessible(true);
        method.invoke(method, (Object) List.of(indexPath).toArray(new String[0]));
    }

    public static String epochToDate(long epoch)
    {
        // Convert the epoch timestamp to an Instant
        Instant instant = Instant.ofEpochSecond(epoch);

        // Define the desired time zone (Vietnam's time zone)
        ZoneId vietnamTimeZone = ZoneId.of("Asia/Ho_Chi_Minh");

        // Define the Vietnamese locale
        Locale vietnameseLocale = new Locale("vi", "VN");

        // Create a formatter with the Vietnamese locale
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm:ss")
                .withLocale(vietnameseLocale)
                .withZone(vietnamTimeZone);

        return formatter.format(instant);
    }

    public static void main(String[] args) throws Exception {

        final String indexFolder = "LH8thEPFTBi_GSgKm8ZOpg/";
        final ObjectMapper objectMapper = new ObjectMapper();
        //System.setOut(new PrintStream("out.log"));

        // Load the existing, possibly corrupt index file
        IndexMetadata indexMetaData = IndexMetadata.FORMAT.read(NamedXContentRegistry.EMPTY, Paths.get( indexFolder,"_state", "state-55.st"));

        String indexName = indexMetaData.getIndex().getName();

        System.out.println("Index creation version: " + indexMetaData.getCreationVersion());
        System.out.println("Creation date: " + epochToDate(indexMetaData.getCreationDate() / 1000));



        writeFile(indexName + "_mappings.json", objectMapper.writeValueAsString(indexMetaData.mapping().getSourceAsMap()));
        writeFile(indexName + "_settings.json", indexMetaData.getSettings().toString());

        // Open the Lucene index reader
        Directory directory = FSDirectory.open(Paths.get(indexFolder + "/0/index/"));

        checkIndex(indexFolder + "/0/index/");

        System.out.println("-------------------------------------------");

        final IndexReader reader = DirectoryReader.open(directory);

        SegmentInfos segments = SegmentInfos.readLatestCommit(directory);
        System.out.println("Segment file name: " + segments.getSegmentsFileName());
        segments.userData.forEach((k,v) -> System.out.println("Segment user data: " + k + " -> " + v ));
        System.out.println("Segment commit lucene version: " + segments.getCommitLuceneVersion().toString());
        System.out.println("Segment min lucene version:    " + segments.getMinSegmentLuceneVersion().toString());
        System.out.println("Segment total max docs: " + segments.totalMaxDoc());
        segments.asList().forEach(segmentCommitInfo -> System.out.println("Segment commit info: " + segmentCommitInfo.toString()));
        System.out.println("-------------------------------------------");

        // Create an index searcher
        IndexSearcher searcher = new IndexSearcher(reader);

        // Create a MatchAllDocsQuery to match all documents
        MatchAllDocsQuery query = new MatchAllDocsQuery();

        // Execute the query to retrieve all documents
        TopDocs topDocs = searcher.search(query, reader.numDocs());

        writeFile(indexName + "_data.json", "");

        // Iterate through the matched documents
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {

            Document document = searcher.doc(scoreDoc.doc);

            Map<String, Object> map = new HashMap<>();

            map.put("_index", indexName);
            map.put("_type", "_doc");
            map.put("_id", objectMapper.writeValueAsString(document.getBinaryValue("_id").bytes).replaceAll("\"", ""));
            map.put("_score", scoreDoc.score);
            map.put("_source", objectMapper.readValue(document.getBinaryValue("_source").utf8ToString(), new TypeReference<Map<String,Object>>(){}));

            writeFile(indexName + "_data.json", objectMapper.writeValueAsString(map) + "\n", true);
        }

        reader.close();


    }
}
