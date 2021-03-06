package pl.edu.agh.ki.frazeusz.model.crawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import pl.edu.agh.ki.frazeusz.model.parser.IParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by matwoosh on 14/01/2017.
 */
public class Downloader implements Runnable {

    private Crawler crawler;
    private IParser parser;

    private String content;
    private String httpHeader;

    Downloader(Crawler crawler, IParser parser) {
        this.crawler = crawler;
        this.parser = parser;
    }

    @Override
    public void run() {
        while (true) {
            Url newUrl = crawler.getUrlToCrawl();
            if (newUrl != null) {
                fetchUrl(newUrl);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void fetchUrl(Url baseUrl) {
        extractUrlData(baseUrl);
        System.out.println("> Fetched ! (" + baseUrl + ")");

        parseUrlContent(baseUrl);
        System.out.printf("> Parsed !");
    }

    private void parseUrlContent(Url baseUrl) {
        List<String> urlsFromParser = null;

        try {
            System.out.println("> Parsing...");
            urlsFromParser = parser.parseContent(httpHeader, content, baseUrl.getAbsoluteUrl());

        } catch (Exception e) {
            System.out.println("  (!) WARNING: Parser couldn't parse baseUrl: " + baseUrl.getAbsoluteUrl());
            e.printStackTrace();
        }

        List<Url> urlsToProcess = new ArrayList<>();
        if (urlsFromParser != null) {
            for (String url : urlsFromParser) {
                urlsToProcess.add(new Url(url, baseUrl.getNestingDepth() + 1));
                System.out.println("  +" + url);
            }
            crawler.addUrlsToProcess(urlsToProcess);
        }
    }

    private void extractUrlData(Url url) {
        System.out.println("> Started fetching: " + url);

        Connection.Response response = null;
        Document document = null;

        try {
            response = Jsoup.connect(url.getAbsoluteUrl()).execute();
            document = response.parse();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (document != null) {
            this.content = document.data();
        } else
            this.content = "No data !";

        if (response != null) {
            String resType = response.contentType();
            if (resType.contains(";"))
                resType = resType.substring(0, resType.indexOf(";"));
            this.httpHeader = resType;

            System.out.println("  + Type: " + resType);
        } else
            this.httpHeader = "No header...";

        assert document != null;
        int pageSizeInBytes = 36 + document.toString().length() * 2;

        crawler.incrementStats(pageSizeInBytes);
    }

}
