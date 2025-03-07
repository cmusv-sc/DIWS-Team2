package search;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import preprocess.Article;
import preprocess.Parser;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SimpleLucene {
    // 0. Specify the analyzer for tokenizing text.
    // The same analyzer should be used for indexing and searching
    StandardAnalyzer analyzer = new StandardAnalyzer();
    // 1. create the index
    Directory index = new RAMDirectory();
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    IndexWriter w;

    public SimpleLucene() {
        try {
            w = new IndexWriter(index, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() throws Exception {
        Parser p = new Parser(Parser.class.getClassLoader().getResource("small_dblp.xml").getPath());
        for (Article article : p.getAricles())
            addArticle(article);
    }

    public void commit() throws IOException {
        w.commit();
    }

    private void addArticle(Article article) throws Exception {
        Document doc = new Document();
        doc.add(new TextField("title", val(article.title), Field.Store.YES));
        doc.add(new TextField("fullText", val(article.toString()), Field.Store.YES));
        doc.add(new TextField("author", val(article.getAuthors().toString().replaceAll("\\[","").replaceAll("\\]","")), Field.Store.YES));
        w.addDocument(doc);
    }

    private String val(String s) {
        if (s == null) return "";
        else return s;
    }


    public List<String> search(String keyword, String field) throws ParseException, IOException {
        List<String> result = new ArrayList<>();
        // 1. get query string according to mode
        String querystr = getQuery(keyword);
        // 2. Set field to query
        Query q;
        q = new QueryParser(field, analyzer).parse(querystr);
        // 3. search
        int hitsPerPage = 100000;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;
        // 4. display results
        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document d = searcher.doc(docId);
            result.add(d.get("title"));
        }
        // reader can only be closed when there
        // is no need to access the documents any more.
        reader.close();
        return result;
    }

    public Map<String, Integer> searchResearcher(String keyword, String field) throws ParseException, IOException {
        Map<String, Integer> map = new HashMap<>();
        List<String> result = new ArrayList<>();
        // 1. get query string according to mode
        String querystr = getQuery(keyword);
        // 2. Set field to query
        Query q;
        q = new QueryParser(field, analyzer).parse(querystr);
        // 3. search
        int hitsPerPage = 100000;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;
        // 4. display results
        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document d = searcher.doc(docId);
            String[] authors = d.get("author").split(",");
            for(String a: authors){
                if(map.containsKey(a)){
                    map.put(a, map.get(a)+1);
                }else{
                    map.put(a, 1);
                }
            }
        }
        // reader can only be closed when there
        // is no need to access the documents any more.
        reader.close();
        return map;
    }

    private String getQuery(String query) {
        String[] tokon = query.split(" ");
        String r = "";
        for (int i = 0; i < tokon.length; i++) {
            if (i != tokon.length - 1)
                r += tokon[i] + " || ";
            else
                r += tokon[i];
        }
        return r;
    }

    public static void main(String[] args) throws Exception {
        SimpleLucene simpleLucene = new SimpleLucene();
        simpleLucene.load();
        simpleLucene.commit();
        for(String title : simpleLucene.search("semantic", "title")){
            System.out.println(title);
        }
    }
}

