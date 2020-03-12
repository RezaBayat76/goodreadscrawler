package ir.rezabayat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class Crawler implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    private static final String GOOD_READS_KEY = "YOUR_KEY";
    private static final String REQUEST_FORMAT = "https://www.goodreads.com/book/review_counts.json?isbns=%s&key=" +
            GOOD_READS_KEY;

    private static final int NUM_BOOKS = 10_000_000;
    private static final int NUM_CRAWLER_THREADS = 10;
    private volatile boolean stop = false;
    private final Thread[] crawlerThreads = new Thread[NUM_CRAWLER_THREADS];
    private static final int NUM_IDS_FOR_EACH_CRAWLER_THREAD = NUM_BOOKS / NUM_CRAWLER_THREADS;
    private static final int STEP = 500;
    private final String isbnFormat;

    public BookTable bookTable;

    public Crawler(String isbnPrefix) {
        this.isbnFormat = isbnPrefix + "%07d";
        this.bookTable = new BookTable();
    }

    public void start() {
        for (int threadNumber = 0; threadNumber < NUM_CRAWLER_THREADS; threadNumber++) {
            int finalThreadNumber = threadNumber;
            crawlerThreads[threadNumber] = new Thread(() -> {
                int startId = finalThreadNumber * NUM_IDS_FOR_EACH_CRAWLER_THREAD;
                int endId = startId + STEP;
                int crawledBooks = 0;

                while (!stop && crawledBooks < NUM_IDS_FOR_EACH_CRAWLER_THREAD) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = startId; i < endId; i++) {
                        String isbn = String.format(isbnFormat, i);
                        sb.append(isbn).append(",");
                    }

                    try {
                        String requestPath = String.format(REQUEST_FORMAT, sb.toString());
                        URL url = new URL(requestPath);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.connect();

                        int responseCode = conn.getResponseCode();
                        if (responseCode == 200) {
                            StringBuilder result = new StringBuilder();
                            Scanner sc = new Scanner(url.openStream());
                            while (sc.hasNext()) {
                                result.append(sc.nextLine());
                            }
                            sc.close();

                            JSONParser parser = new JSONParser();
                            JSONObject json = (JSONObject) parser.parse(result.toString());
                            JSONArray books = (JSONArray) json.get("books");

                            bookTable.put(books);
                        }
                    } catch (IOException e) {
                        logger.error("Could not retrieve books or put in table.", e);
                    } catch (ParseException e) {
                        logger.error("Could not convert to json format.", e);
                    }

                    startId = endId;
                    endId = endId + STEP;
                    crawledBooks = crawledBooks + STEP;
                }
            });

            crawlerThreads[threadNumber].setName("Crawler-" + threadNumber);
        }

        for (Thread crawlerThread : crawlerThreads) {
            crawlerThread.start();
        }
    }


    @Override
    public void close() throws IOException {
        stop = true;

        for (Thread crawlerThread : crawlerThreads) {
            crawlerThread.interrupt();
        }

        for (Thread crawlerThread : crawlerThreads) {
            try {
                crawlerThread.join();
            } catch (InterruptedException e) {
                logger.error("Could not close crawler threads.", e);
            }
        }
    }
}