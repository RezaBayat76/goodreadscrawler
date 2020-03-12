package ir.rezabayat;

import org.json.simple.JSONArray;

import java.io.IOException;

public class BookTable {

    int crawledBooks = 0;

    public void put(JSONArray books) throws IOException {
        crawledBooks += books.size();
        System.out.println(books.size());

        if (crawledBooks % 100 == 0) {
            System.out.println("Current crawled books: " + crawledBooks);
        }
    }
}
