package ir.rezabayat;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<String> isbnPrefixes = new ArrayList<>();

        // Persian reserved prefix for ISBN
        isbnPrefixes.add("978964");
        isbnPrefixes.add("978600");
        isbnPrefixes.add("978622");
        for (String isbnPrefix : isbnPrefixes) {
            Crawler crawler = new Crawler(isbnPrefix);
            crawler.start();
        }
    }
}
