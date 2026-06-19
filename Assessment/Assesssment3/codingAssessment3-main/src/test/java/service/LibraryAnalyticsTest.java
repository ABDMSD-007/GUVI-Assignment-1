package service;

import entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LibraryAnalyticsTest {

    private LibraryAnalytics libraryAnalytics;

    // Standard valid baseline records
    private final String r1 = "B101|Clean Code|Robert Martin|Programming|145|4.8";
    private final String r2 = "B102|Effective Java|Joshua Bloch|Programming|180|4.9";
    private final String r3 = "B103|Atomic Habits|James Clear|SelfHelp|200|4.7";

    @BeforeEach
    void setUp() {
        libraryAnalytics = new LibraryAnalytics();
    }

    @Nested
    @DisplayName("Rule 1 & 2: loadBooks Validation and Deduplication Tests")
    class LoadBooksTests {

        @Test
        @DisplayName("Should successfully load valid records and filter out corrupted/out-of-bounds data")
        void testLoadBooksValidation() {
            List<String> records = Arrays.asList(
                    r1,
                    "B201|Bad Book|John Doe|Fiction|100|-0.1",   // Invalid: Rating < 0
                    "B202|God Tier|John Doe|Fiction|100|5.1",    // Invalid: Rating > 5
                    "B203|Neg Borrow|John Doe|Fiction|-5|4.5",   // Invalid: BorrowCount < 0
                    "B204||John Doe|Fiction|50|4.0",             // Invalid: Empty Title field
                    "B205|   |John Doe|Fiction|50|4.0"           // Invalid: Blank space Title field
            );

            libraryAnalytics.loadBooks(records);

            // Only r1 (B101) should pass validation rules
            List<Book> topBooks = libraryAnalytics.topRatedBooks(10);
            assertEquals(1, topBooks.size(), "Only 1 valid book should have been loaded.");
            assertEquals("B101", topBooks.get(0).getBookId());
        }

        @Test
        @DisplayName("Duplicate ID Resolution: Higher Rating Wins")
        void testDuplicateIdHigherRatingWins() {
            List<String> records = Arrays.asList(
                    "B100|Base Title|Author A|Tech|100|4.0",
                    "B100|Base Title|Author A|Tech|100|4.5" // Higher rating
            );
            libraryAnalytics.loadBooks(records);
            List<Book> result = libraryAnalytics.topRatedBooks(1);
            assertEquals(4.5, result.get(0).getRating(), 0.001);
        }

        @Test
        @DisplayName("Duplicate ID Resolution: Tie on Rating, Higher Borrow Count Wins")
        void testDuplicateIdHigherBorrowWins() {
            List<String> records = Arrays.asList(
                    "B100|Base Title|Author A|Tech|100|4.2",
                    "B100|Base Title|Author A|Tech|200|4.2" // Higher borrow
            );
            libraryAnalytics.loadBooks(records);
            List<Book> result = libraryAnalytics.topRatedBooks(1);
            assertEquals(200, result.get(0).getBorrowCount());
        }

        @Test
        @DisplayName("Duplicate ID Resolution: Tie on Rating & Borrow, Lexicographically Smaller Title Wins")
        void testDuplicateIdLexicographicalTitleWins() {
            List<String> records = Arrays.asList(
                    "B100|Zeta Title|Author A|Tech|100|4.2",
                    "B100|Alpha Title|Author A|Tech|100|4.2" // Smaller alphabetically
            );
            libraryAnalytics.loadBooks(records);
            List<Book> result = libraryAnalytics.topRatedBooks(1);
            assertEquals("Alpha Title", result.get(0).getTitle());
        }
    }

    @Nested
    @DisplayName("Rule 3: topRatedBooks(n) Sorting Hierarchy Tests")
    class TopRatedBooksTests {

        @Test
        @DisplayName("Should sort cascadingly by Rating DESC -> Borrow DESC -> Title ASC")
        void testTopRatedBooksSortingPrecedence() {
            List<String> records = Arrays.asList(
                    "B01|Alpha|Auth|Tech|100|4.8",       // Lower rating (4th)
                    "B02|Beta|Auth|Tech|100|4.9",        // Highest rating, lower borrow (3rd)
                    "B03|Gamma|Auth|Tech|200|4.9",       // Highest rating, highest borrow (1st)
                    "B04|Delta|Auth|Tech|200|4.9"        // Highest rating, highest borrow, smaller title (2nd)
            );
            libraryAnalytics.loadBooks(records);

            List<Book> result = libraryAnalytics.topRatedBooks(4);

            assertEquals("B03", result.get(0).getBookId(), "1st should be B03 (Highest borrow tiebreaker)");
            assertEquals("B04", result.get(1).getBookId(), "2nd should be B04 (Alphabetical title tiebreaker)");
            assertEquals("B02", result.get(2).getBookId(), "3rd should be B02");
            assertEquals("B01", result.get(3).getBookId(), "4th should be B01 (Lowest rating)");
        }

        @Test
        @DisplayName("Boundary: Requesting n larger than available collection sizes")
        void testTopRatedBooksLargeN() {
            libraryAnalytics.loadBooks(Arrays.asList(r1, r2));
            List<Book> result = libraryAnalytics.topRatedBooks(100);
            assertEquals(2, result.size(), "Should safely return all available items without blowing up.");
        }

        @Test
        @DisplayName("Boundary: Zero or negative n arguments should return an empty list")
        void testTopRatedBooksZeroOrNegativeN() {
            libraryAnalytics.loadBooks(Arrays.asList(r1, r2));
            assertNotNull(libraryAnalytics.topRatedBooks(0));
            assertTrue(libraryAnalytics.topRatedBooks(0).isEmpty());
            assertTrue(libraryAnalytics.topRatedBooks(-5).isEmpty());
        }
    }

    @Nested
    @DisplayName("Rule 4: averageRatingByCategory Evaluation Tests")
    class AverageRatingByCategoryTests {

        @Test
        @DisplayName("Should return an alphabetically sorted TreeMap with averages rounded to 2 decimal places")
        void testAverageRatingCalculationAndFormatting() {
            List<String> records = Arrays.asList(
                    "B1|Title1|Auth|Tech|10|4.15",
                    "B2|Title2|Auth|Tech|10|4.16", // Tech average = 4.155 -> rounds to 4.16
                    "B3|Title3|Auth|Arts|10|3.84"  // Alphabetically prior category
            );
            libraryAnalytics.loadBooks(records);

            Map<String, Double> result = libraryAnalytics.averageRatingByCategory();

            // Structure Verification
            assertInstanceOf(TreeMap.class, result, "Returned map type must explicitly be a TreeMap.");

            // Order Verification
            Iterator<String> keyIterator = result.keySet().iterator();
            assertEquals("Arts", keyIterator.next());
            assertEquals("Tech", keyIterator.next());

            // Precision Value Rounding Verification
            assertEquals(3.84, result.get("Arts"), 0.001);
            assertEquals(4.16, result.get("Tech"), 0.001);
        }
    }

    @Nested
    @DisplayName("Rule 5: mostBorrowedBook Priority Framework Tests")
    class MostBorrowedBookTests {

        @Test
        @DisplayName("Should determine absolute frontrunner matching Borrow DESC -> Rating DESC -> BookID ASC")
        void testMostBorrowedBookTieBreakers() {
            List<String> records = Arrays.asList(
                    "B99|Book One|A|Tech|500|4.0",   // Base high borrow
                    "B50|Book Two|A|Tech|500|4.5",   // Same borrow, higher rating
                    "B10|Book Three|A|Tech|500|4.5"  // Same borrow, same rating, smaller ID wins
            );
            libraryAnalytics.loadBooks(records);

            Optional<Book> result = libraryAnalytics.mostBorrowedBook();
            assertTrue(result.isPresent());
            assertEquals("B10", result.get().getBookId());
        }

        @Test
        @DisplayName("Should elegantly hand back an Optional.empty structure if the ecosystem lacks data")
        void testMostBorrowedBookEmptyLibrary() {
            Optional<Book> result = libraryAnalytics.mostBorrowedBook();
            assertFalse(result.isPresent(), "Should map to empty optional when zero entries exist.");
        }
    }

    @Nested
    @DisplayName("Rule 6: authorsWithMultipleCategories Group Isolation Tests")
    class AuthorsWithMultipleCategoriesTests {

        @Test
        @DisplayName("Should extract authors operating across multiple non-unique categories via a TreeSet container")
        void testAuthorsWithMultipleCategoriesFiltering() {
            List<String> records = Arrays.asList(
                    "B1|T1|Robert Martin|Programming|10|4.0",
                    "B2|T2|Robert Martin|Management|10|4.0",  // Robert Martin maps to 2 categories
                    "B3|T3|Joshua Bloch|Programming|10|4.0",
                    "B4|T4|Joshua Bloch|Programming|10|4.0",  // Joshua Bloch maps to 1 category
                    "B5|T5|Alpha Author|Z-Category|10|4.0",
                    "B6|T6|Alpha Author|A-Category|10|4.0"    // Alpha Author maps to 2 categories
            );
            libraryAnalytics.loadBooks(records);

            Set<String> result = libraryAnalytics.authorsWithMultipleCategories();

            assertInstanceOf(TreeSet.class, result, "Returned container framework must be a TreeSet.");
            assertEquals(2, result.size());

            // TreeSet guarantees Alphabetical Order verification
            Iterator<String> iterator = result.iterator();
            assertEquals("Alpha Author", iterator.next());
            assertEquals("Robert Martin", iterator.next());
        }
    }

    @Nested
    @DisplayName("Rule 7: groupBooksByAuthor Sorting Matrix Tests")
    class GroupBooksByAuthorTests {

        @Test
        @DisplayName("Should output a LinkedHashMap ordered by Author Name ASC, grouping internal lists by Rating DESC -> Borrow DESC")
        void testGroupBooksByAuthorStructureAndSort() {
            List<String> records = Arrays.asList(
                    "B1|T1|Zeppelin|Tech|100|4.0",
                    "B2|T2|Abacus|Tech|100|4.2",
                    "B3|T3|Abacus|Tech|100|4.8",   // Highest Rating for Abacus -> position 0
                    "B4|T4|Abacus|Tech|200|4.8"    // Same Rating, Higher Borrow -> bumps to position 0
            );
            libraryAnalytics.loadBooks(records);

            Map<String, List<Book>> result = libraryAnalytics.groupBooksByAuthor();

            assertInstanceOf(LinkedHashMap.class, result, "Must preserve insertion/alphabetical key tracking via LinkedHashMap.");

            // Assert core keys sorted tracking sequence
            Iterator<String> authorKeys = result.keySet().iterator();
            assertEquals("Abacus", authorKeys.next());
            assertEquals("Zeppelin", authorKeys.next());

            // Assert custom layout array ranking inside sub list configurations
            List<Book> abacusBooks = result.get("Abacus");
            assertEquals("B4", abacusBooks.get(0).getBookId(), "B4 has top rating and higher borrow.");
            assertEquals("B3", abacusBooks.get(1).getBookId());
            assertEquals("B2", abacusBooks.get(2).getBookId());
        }
    }

    @Nested
    @DisplayName("Rule 8: suspiciousBooks Stream Engine Deep Inspection")
    class SuspiciousBooksTests {

        @Test
        @DisplayName("Condition 1: Flag titles containing consecutive identical words")
        void testConditionOneConsecutiveWords() {
            libraryAnalytics.loadBooks(Collections.singletonList("B01|Java Java Mastery|Robert|Tech|10|4.0"));
            List<String> result = libraryAnalytics.suspiciousBooks();
            assertEquals(Collections.singletonList("Java Java Mastery"), result);
        }

        @Test
        @DisplayName("Condition 2: Flag titles containing the exact name of the author")
        void testConditionTwoAuthorInTitle() {
            libraryAnalytics.loadBooks(Collections.singletonList("B02|The James Clear Method|James Clear|SelfHelp|10|4.0"));
            List<String> result = libraryAnalytics.suspiciousBooks();
            assertEquals(Collections.singletonList("The James Clear Method"), result);
        }

        @Test
        @DisplayName("Condition 3: Flag books where borrow count > 300% of category baseline mean")
        void testConditionThreeBorrowSurge() {
            List<String> records = Arrays.asList(
                    "B1|Base 1|A|Tech|100|4.5",
                    "B2|Base 2|B|Tech|100|4.5",       // Average calculation pool = 100
                    "B3|Anomalous Spike|C|Tech|401|4.5" // 401 is strictly greater than 100 + (300% of 100) = 400
            );
            libraryAnalytics.loadBooks(records);
            List<String> result = libraryAnalytics.suspiciousBooks();
            assertEquals(Collections.singletonList("Anomalous Spike"), result);
        }

        @Test
        @DisplayName("Condition 4: Flag books with ratings below category average while borrows are above category average")
        void testConditionFourUnderperformingHighTraffic() {
            List<String> records = Arrays.asList(
                    "B1|Standard Vol 1|A|Tech|100|4.6",
                    "B2|Standard Vol 2|B|Tech|100|4.0", // Category baseline means: Borrow = 100, Rating = 4.3
                    "B3|Suspicious Trapped|C|Tech|150|4.1" // Borrow 150 > 100 AND Rating 4.1 < 4.3
            );
            libraryAnalytics.loadBooks(records);
            List<String> result = libraryAnalytics.suspiciousBooks();
            assertEquals(Collections.singletonList("Suspicious Trapped"), result);
        }

        @Test
        @DisplayName("Comprehensive Output Rules: Distinct items sorted alphabetically")
        void testSuspiciousBooksFormattingOutput() {
            List<String> records = Arrays.asList(
                    "B1|Clean Clean Code|Robert Martin|Programming|100|4.5",
                    "B2|Clean Clean Code|Robert Martin|Programming|100|4.5", // Duplicate Title matching Rule 1 criteria
                    "B3|Apple Apple Security|Joshua Bloch|Programming|100|4.5"
            );
            libraryAnalytics.loadBooks(records);
            List<String> result = libraryAnalytics.suspiciousBooks();

            assertEquals(2, result.size());
            assertEquals("Apple Apple Security", result.get(0), "Alphabetically smaller output tracking must fall first.");
            assertEquals("Clean Clean Code", result.get(1));
        }
    }

    @Nested
    @DisplayName("Final Challenge: Category-wise Top Rated Book by Each Author using Mockito Records Mocking")
    class FinalChallengeTests {

        @Mock
        private List<String> mockRecordsList;

        @Test
        @DisplayName("Should successfully segment nested mappings processing data entirely upstream via functional Streams rules")
        void testCategoryWiseTopRatedBookByEachAuthor() {
            // Using Mockito to feed mocked input record sequences downstream
            List<String> trainingData = Arrays.asList(
                    "B1|Effective Java|Joshua Bloch|Programming|100|4.9",
                    "B2|Java Deep Dive|Joshua Bloch|Programming|100|4.5", // Lower internal rating fallback
                    "B3|Clean Code|Robert Martin|Programming|100|4.8",
                    "B4|Atomic Habits|James Clear|SelfHelp|200|4.7"
            );

            // Stub mock behavior wrapper
            when(mockRecordsList.iterator()).thenReturn(trainingData.iterator());
            when(mockRecordsList.size()).thenReturn(trainingData.size());

            libraryAnalytics.loadBooks(mockRecordsList);

            Map<String, Map<String, Book>> challengeResult = libraryAnalytics.categoryWiseTopRatedBookByEachAuthor();

            assertNotNull(challengeResult);
            assertTrue(challengeResult.containsKey("Programming"));
            assertTrue(challengeResult.containsKey("SelfHelp"));

            // Drilldown Assertions mapping internal target entities cleanly
            Book joshuaTopBook = challengeResult.get("Programming").get("Joshua Bloch");
            assertEquals("Effective Java", joshuaTopBook.getTitle());
            assertEquals(4.9, joshuaTopBook.getRating(), 0.001);

            Book robertTopBook = challengeResult.get("Programming").get("Robert Martin");
            assertEquals("Clean Code", robertTopBook.getTitle());

            Book jamesTopBook = challengeResult.get("SelfHelp").get("James Clear");
            assertEquals("Atomic Habits", jamesTopBook.getTitle());
        }
    }
}
