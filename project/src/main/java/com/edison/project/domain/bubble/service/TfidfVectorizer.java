package com.edison.project.domain.bubble.service;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TfidfVectorizer {

    public Map<Integer, Map<String, Double>> calculateTfIdf(List<String> documents) throws Exception {
        RAMDirectory directory = new RAMDirectory();
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(directory, config);

        // Index documents
        for (int i = 0; i < documents.size(); i++) {
            Document doc = new Document();
            doc.add(new org.apache.lucene.document.TextField("content", documents.get(i), org.apache.lucene.document.Field.Store.YES));
            writer.addDocument(doc);
        }
        writer.close();

        // Calculate TF-IDF
        IndexReader reader = DirectoryReader.open(directory);
        Map<Integer, Map<String, Double>> tfIdfMap = new HashMap<>();

        for (int docId = 0; docId < reader.maxDoc(); docId++) {
            Map<String, Double> tfIdfValues = new HashMap<>();
            Terms terms = reader.getTermVector(docId, "content");
            TermsEnum termsEnum = terms.iterator();

            BytesRef term;
            while ((term = termsEnum.next()) != null) {
                String termText = term.utf8ToString();
                double tf = termsEnum.totalTermFreq();
                double idf = Math.log(reader.maxDoc() / (double) reader.docFreq(new Term("content", termText)));
                tfIdfValues.put(termText, tf * idf);
            }
            tfIdfMap.put(docId, tfIdfValues);
        }
        reader.close();

        return tfIdfMap;
    }
}
