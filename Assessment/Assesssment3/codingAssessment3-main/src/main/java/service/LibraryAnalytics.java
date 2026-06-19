package service;

import entity.Book;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LibraryAnalytics {

    private Map<String, Book> books = new HashMap<>();

    public void loadBooks(List<String> records) {

        if(records == null || records.size() == 0)
            return;

        for(String record : records)
        {
            if(record == null || record.trim().isEmpty())
                continue;

            String[] arr = record.split("\\|");

            if(arr.length != 6)
                continue;

            String bookId = arr[0].trim();
            String title = arr[1].trim();
            String author = arr[2].trim();
            String category = arr[3].trim();

            int borrowCount;
            double rating;

            try
            {
                borrowCount =
                        Integer.parseInt(arr[4].trim());

                rating =
                        Double.parseDouble(arr[5].trim());
            }
            catch(Exception e)
            {
                continue;
            }

            if(bookId.isEmpty()
                    || title.isEmpty()
                    || author.isEmpty()
                    || category.isEmpty()
                    || borrowCount < 0
                    || rating < 0
                    || rating > 5)
                continue;

            Book current =
                    new Book(
                            bookId,
                            title,
                            author,
                            category,
                            borrowCount,
                            rating
                    );

            Book existing =
                    books.get(bookId);

            if(existing == null)
            {
                books.put(bookId,current);
            }
            else
            {
                if(current.getRating() >
                        existing.getRating())
                {
                    books.put(bookId,current);
                }
                else if(current.getRating() ==
                        existing.getRating())
                {
                    if(current.getBorrowCount() >
                            existing.getBorrowCount())
                    {
                        books.put(bookId,current);
                    }
                    else if(current.getBorrowCount() ==
                            existing.getBorrowCount())
                    {
                        if(current.getTitle()
                                .compareTo(
                                        existing.getTitle()) < 0)
                        {
                            books.put(bookId,current);
                        }
                    }
                }
            }
        }
    }

    public List<Book> topRatedBooks(int n) {

        if(n <= 0)
            return new ArrayList<>();

        return books.values()
                .stream()
                .sorted(
                        Comparator
                                .comparingDouble(Book::getRating)
                                .reversed()
                                .thenComparing(
                                        Comparator
                                                .comparingInt(
                                                        Book::getBorrowCount)
                                                .reversed()
                                )
                                .thenComparing(
                                        Book::getTitle,
                                        Comparator.reverseOrder()
                                )
                )
                .limit(n)
                .toList();
    }

    public Map<String, Double> averageRatingByCategory() {

        return books.values()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Book::getCategory,
                                TreeMap::new,
                                Collectors.collectingAndThen(
                                        Collectors.averagingDouble(
                                                Book::getRating
                                        ),
                                        avg ->
                                                Math.round(avg * 100.0)
                                                        / 100.0
                                )
                        )
                );
    }

    public Optional<Book> mostBorrowedBook() {

        return books.values()
                .stream()
                .max(
                        Comparator
                                .comparingInt(
                                        Book::getBorrowCount
                                )
                                .thenComparing(
                                        Book::getRating
                                )
                                .thenComparing(
                                        Book::getBookId,
                                        Comparator.reverseOrder()
                                )
                );
    }

    public Set<String> authorsWithMultipleCategories() {

        return books.values()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Book::getAuthor,
                                Collectors.mapping(
                                        Book::getCategory,
                                        Collectors.toSet()
                                )
                        )
                )
                .entrySet()
                .stream()
                .filter(
                        e -> e.getValue().size() > 1
                )
                .map(Map.Entry::getKey)
                .collect(
                        Collectors.toCollection(
                                TreeSet::new
                        )
                );
    }

    public Map<String,List<Book>> groupBooksByAuthor() {

        return books.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                Book::getAuthor
                        )
                )
                .collect(
                        Collectors.groupingBy(
                                Book::getAuthor,
                                LinkedHashMap::new,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> {

                                            list.sort(
                                                    Comparator
                                                            .comparingDouble(
                                                                    Book::getRating
                                                            )
                                                            .reversed()
                                                            .thenComparing(
                                                                    Comparator
                                                                            .comparingInt(
                                                                                    Book::getBorrowCount
                                                                            )
                                                                            .reversed()
                                                            )
                                            );

                                            return list;
                                        }
                                )
                        )
                );
    }

    public List<String> suspiciousBooks()
    {
        return books.values()
                .stream()
                .filter(book -> {

                    String[] words =
                            book.getTitle()
                                    .toLowerCase()
                                    .split("\\s+");

                    boolean condition1 =
                            IntStream.range(
                                            0,
                                            words.length - 1
                                    )
                                    .anyMatch(
                                            i ->
                                                    words[i]
                                                            .equals(
                                                                    words[i + 1]
                                                            )
                                    );

                    boolean condition2 = false;

                    String author =
                            book.getAuthor()
                                    .trim()
                                    .toLowerCase();

                    if(author.length() > 1)
                    {
                        condition2 =
                                book.getTitle()
                                        .toLowerCase()
                                        .contains(author);
                    }

                    List<Book> sameCategory =
                            books.values()
                                    .stream()
                                    .filter(
                                            b ->
                                                    b != book &&
                                                            b.getCategory()
                                                                    .equals(
                                                                            book.getCategory()
                                                                    )
                                    )
                                    .toList();

                    double avgBorrow =
                            sameCategory
                                    .stream()
                                    .mapToInt(Book::getBorrowCount)
                                    .average()
                                    .orElse(0);

                    double avgRating =
                            sameCategory
                                    .stream()
                                    .mapToDouble(Book::getRating)
                                    .average()
                                    .orElse(0);

                    boolean condition3 =
                            avgBorrow > 0
                                    &&
                                    book.getBorrowCount()
                                            >
                                            avgBorrow * 4;

                    boolean condition4 =
                            avgBorrow > 0
                                    &&
                                    book.getBorrowCount()
                                            >
                                            avgBorrow
                                    &&
                                    book.getRating()
                                            <
                                            avgRating;

                    return condition1
                            || condition2
                            || condition3
                            || condition4;
                })
                .map(Book::getTitle)
                .distinct()
                .sorted()
                .toList();
    }

    public Map<String, Map<String, Book>>
    categoryWiseTopRatedBookByEachAuthor() {

        return books.values()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Book::getCategory,
                                TreeMap::new,
                                Collectors.toMap(
                                        Book::getAuthor,
                                        book -> book,
                                        (b1, b2) -> {

                                            if(b1.getRating() >
                                                    b2.getRating())
                                                return b1;

                                            if(b2.getRating() >
                                                    b1.getRating())
                                                return b2;

                                            if(b1.getBorrowCount()
                                                    >=
                                                    b2.getBorrowCount())
                                                return b1;

                                            return b2;
                                        }
                                )
                        )
                );
    }
}