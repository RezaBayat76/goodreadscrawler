package ir.rezabayat;

import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Crawler implements Closeable {
    private static final String GOOD_READS_KEY = "YOUR_KEY";
    private static final String REQUEST_FORMAT = "https://www.goodreads.com/book/review_counts.json?isbns=%s&key=" + GOOD_READS_KEY;
    private static final int NUM_BOOKS = 10_000_000;
    private static final int NUM_CRAWLER_THREADS = 10;
    private Thread[] crawlerThreads = new Thread[NUM_CRAWLER_THREADS];
    private static final int NUM_IDS_FOR_EACH_CRAWLER_THREAD = NUM_BOOKS / NUM_CRAWLER_THREADS;
    private static final int STEP = 500;
    private final String isbnFormat;

    private volatile boolean stop = false;

    public Crawler(String isbnPrefix) {
        this.isbnFormat = isbnPrefix + "%07d";
    }

    public void start() {
        startCrawlerThreads();
    }

    private void startCrawlerThreads() {
        for (int threadNumber = 0; threadNumber < NUM_CRAWLER_THREADS; threadNumber++) {
            int finalThreadNumber = threadNumber;
            crawlerThreads[threadNumber] = new Thread(() -> {
                int startId = finalThreadNumber * NUM_IDS_FOR_EACH_CRAWLER_THREAD;
                int endId = startId + STEP;
                int crawlerBooks = 0;

                while (!stop && crawlerBooks <= NUM_IDS_FOR_EACH_CRAWLER_THREAD) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = startId; i < endId; i++) {
                        String isbn = String.format(isbnFormat, i);
                        sb.append(isbn).append(",");
                    }
                    try {
                        String requestPath = String.format(REQUEST_FORMAT, sb.toString());
                        URL url = new URL(requestPath);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.connect();
                        int responseCode = conn.getResponseCode();
                        if (responseCode != 200) {
                            throw new IllegalStateException("Not found any books.");
                        } else {
                            StringBuilder result = new StringBuilder();
                            Scanner sc = new Scanner(url.openStream());
                            while (sc.hasNext()) {
                                result.append(sc.nextLine());
                            }
                            sc.close();
                            System.out.println(result.toString());
                        }
                    } catch (Exception e) {
                        System.out.println("Could not crawl book review.");
                    }

                    startId = endId;
                    endId = endId + STEP;
                    crawlerBooks = crawlerBooks + STEP;
                }
            });

            crawlerThreads[threadNumber].setName("Crawler-" + threadNumber);
        }

        for (Thread crawlerThread : crawlerThreads) {
            crawlerThread.start();
        }
    }

    public static void main(String[] args) {
        List<String> isbnPrefixes = new ArrayList<>();
        isbnPrefixes.add("978964");
        isbnPrefixes.add("978600");
        isbnPrefixes.add("978622");
        for (String isbnPrefix : isbnPrefixes) {
            Crawler crawler = new Crawler(isbnPrefix);
            crawler.start();
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
                e.printStackTrace();
            }
        }
    }
}