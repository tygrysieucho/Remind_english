package org.example;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class WordReview {


    public static void initializeDatabase(Connection connection) {
        String createTableQuery = """
                CREATE TABLE IF NOT EXISTS words (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    word TEXT NOT NULL,
                    translation TEXT NOT NULL,
                    added_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_review_date TIMESTAMP,
                    review_stage INTEGER DEFAULT 0
                );
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableQuery);
            System.out.println("Zainicjalizowano strukturę tabeli w bazie danych.");
        } catch (SQLException e) {
            System.out.println("Błąd podczas inicjalizacji bazy danych: " + e.getMessage());
        }
    }

    public static void createDatabaseIfNotExists(String dbFileName) {
        String dbUrl = "jdbc:sqlite:" + dbFileName;
        boolean isNewDatabase = false;

        try (Connection connection = DriverManager.getConnection(dbUrl)) {
            if (connection != null) {
                DatabaseMetaData metaData = connection.getMetaData();
                isNewDatabase = metaData.getDriverName() != null;
                initializeDatabase(connection);
            }
        } catch (SQLException e) {
            System.out.println("Błąd podczas tworzenia bazy danych: " + e.getMessage());
        }

        if (isNewDatabase) {
            System.out.println("Utworzono nową bazę danych: " + dbFileName);
        }
    }


    // Metoda do obliczania daty powtórzenia
    public static LocalDateTime calculateNextReview(LocalDateTime baseDate, int daysToAdd) {
        return baseDate.plusDays(daysToAdd);
    }


    // Metoda do weryfikacji tłumaczenia z uwzględnieniem wielu opcji
    public static boolean checkTranslation(String userTranslation, String correctTranslations) {
        // Rozdziel poprawne tłumaczenia i usuń zbędne spacje
        String[] translationsArray = correctTranslations.split(",");
        List<String> correctList = new ArrayList<>();
        for (String translation : translationsArray) {
            correctList.add(translation.trim().toLowerCase());
        }

        // Jeśli użytkownik podał pojedyncze tłumaczenie, sprawdź, czy znajduje się na liście poprawnych
        if (!userTranslation.contains(",")) {
            return correctList.contains(userTranslation.trim().toLowerCase());
        }

        // Rozdziel tłumaczenia użytkownika i usuń zbędne spacje
        String[] userTranslationsArray = userTranslation.split(",");
        List<String> userList = new ArrayList<>();
        for (String translation : userTranslationsArray) {
            userList.add(translation.trim().toLowerCase());
        }

        // Posortuj obie listy i porównaj
        Collections.sort(correctList);
        Collections.sort(userList);

        return correctList.equals(userList);
    }

    // Metoda do zaplanowania kolejnych powtórek
    public static void scheduleNextReview(Connection connection, int wordId, int currentStage) {
        String updateQuery = "UPDATE words SET last_review_date = ?, review_stage = ? WHERE id = ?";
        LocalDateTime nextReviewDate = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int nextStage = currentStage;

        switch (currentStage) {
            case 0:
                nextReviewDate = calculateNextReview(nextReviewDate, 1);
                nextStage = 1;
                break;
            case 1:
                nextReviewDate = calculateNextReview(nextReviewDate, 3);
                nextStage = 2;
                break;
            case 2:
                nextReviewDate = calculateNextReview(nextReviewDate, 6);
                nextStage = 3;
                break;
        }

        try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
            stmt.setTimestamp(1, Timestamp.valueOf(nextReviewDate));
            stmt.setInt(2, nextStage);
            stmt.setInt(3, wordId);
            stmt.executeUpdate();
            System.out.println("Kolejna powtórka " + nextReviewDate.format(formatter));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Metoda do wyszukiwania tłumaczenia z tolerancją na literówki
    public static void findTranslationWithTypos(Connection connection, String searchWord) {
        String query = "SELECT word, translation FROM words";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            boolean found = false;

            while (rs.next()) {
                String word = rs.getString("word");
                String translation = rs.getString("translation");

                if (isSimilar(word, searchWord)) {
                    System.out.println("Znaleziono słowo: " + word + " - Tłumaczenie: " + translation);
                    found = true;
                }
            }

            if (!found) {
                System.out.println("Nie znaleziono podobnych słów dla: " + searchWord);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isSimilar(String word1, String word2) {
        int maxDistance = 2; // Tolerancja błędów
        return levenshteinDistance(word1.toLowerCase(), word2.toLowerCase()) <= maxDistance;
    }

    // Algorytm Levenshteina do obliczania odległości między dwoma ciągami
    public static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    public static List<Integer> getWordsDueForReview(Connection connection) {
        String query = "SELECT id FROM words " +
                "WHERE last_review_date IS NULL " +
                "   OR (DATE(last_review_date, " +
                "        CASE review_stage " +
                "            WHEN 0 THEN '+1 day' " +  // First review: +1 day
                "            WHEN 1 THEN '+3 days' " + // Second review: +3 days
                "            WHEN 2 THEN '+6 days' " + // Third review: +6 days
                "            ELSE '+0 day' " +
                "        END) <= DATE('now'))";
        List<Integer> wordIds = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                wordIds.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return wordIds;
    }


//    public static void reviewWordsWithTranslation(Connection connection) {
//        List<Integer> dueWords = getWordsDueForReview(connection);
//        Scanner scanner = new Scanner(System.in);
//
//        for (int wordId : dueWords) {
//            String fetchQuery = "SELECT word, translation, review_stage FROM words WHERE id = ?";
//            try (PreparedStatement stmt = connection.prepareStatement(fetchQuery)) {
//                stmt.setInt(1, wordId);
//                ResultSet rs = stmt.executeQuery();
//
//                if (rs.next()) {
//                    String word = rs.getString("word");
//                    String correctTranslation = rs.getString("translation");
//                    int currentStage = rs.getInt("review_stage");
//
//                    System.out.print("Podaj tłumaczenie dla frazy '" + word + "': ");
//                    String userTranslation = scanner.nextLine();
//
//                    // Split poprawnych tłumaczeń
//                    String[] correctTranslations = correctTranslation.split(",");
//
//                    boolean isCorrect = false;
//                    for (String correct : correctTranslations) {
//                        correct = correct.trim().toLowerCase();
//                        if (levenshteinDistance(userTranslation.toLowerCase().trim(), correct) <= 2) {
//                            isCorrect = true;
//                            break;
//                        }
//                    }
//
//                    if (isCorrect) {
//                        System.out.println("Poprawne tłumaczenie. Inne możliwe: " + correctTranslation);
//                        scheduleNextReview(connection, wordId, currentStage);
//                    } else {
//                        System.out.println("Niepoprawne tłumaczenie. Poprawne tłumaczenia to: " + correctTranslation);
//                    }
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public static void reviewWordsWithTranslation(Connection connection) {
        List<Integer> dueWords = getWordsDueForReview(connection);
        Scanner scanner = new Scanner(System.in);

        for (int wordId : dueWords) {
            String fetchQuery = "SELECT word, translation, review_stage FROM words WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(fetchQuery)) {
                stmt.setInt(1, wordId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String word = rs.getString("word");
                    String correctTranslation = rs.getString("translation");
                    int currentStage = rs.getInt("review_stage");

                    System.out.print("Podaj tłumaczenie dla frazy '" + word + "': ");
                    String userTranslation = scanner.nextLine();

                    // Split poprawnych tłumaczeń
                    String[] correctTranslations = correctTranslation.split(",");

                    boolean isCorrect = false;
                    for (String correct : correctTranslations) {
                        correct = correct.trim().toLowerCase();
                        if (areTranslationsSimilar(userTranslation.toLowerCase().trim(), correct)) {
                            isCorrect = true;
                            break;
                        }
                    }

                    if (isCorrect) {
                        System.out.println("Poprawne tłumaczenie. Inne możliwe: " + correctTranslation);
                        scheduleNextReview(connection, wordId, currentStage);
                    } else {
                        System.out.println("Niepoprawne tłumaczenie. Poprawne tłumaczenia to: " + correctTranslation);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Nowa metoda porównująca tłumaczenia z uwzględnieniem słów kluczowych
    public static boolean areTranslationsSimilar(String userTranslation, String correctTranslation) {
        // Podziel tłumaczenia na poszczególne słowa
        String[] userWords = userTranslation.split(" ");
        String[] correctWords = correctTranslation.split(" ");

        // Przechowuj licznik dopasowanych słów
        int matchingWords = 0;

        for (String userWord : userWords) {
            userWord = userWord.trim();
            for (String correctWord : correctWords) {
                correctWord = correctWord.trim();
                // Sprawdź, czy słowa są podobne, ignorując literówki
                if (levenshteinDistance(userWord, correctWord) <= 2) {
                    matchingWords++;
                    break;
                }
            }
        }

        // Jeśli większość słów użytkownika jest podobna do poprawnych, uznaj tłumaczenie za poprawne
        return (double) matchingWords / correctWords.length >= 0.7; // 70% dopasowania
    }


    public static void runProgram(String dbUrl) {

        try (Connection connection = DriverManager.getConnection(dbUrl)) {
            Scanner scanner = new Scanner(System.in);
            int option = -1;

            while (option != 0) {
                System.out.println("\nWybierz opcję:");
                System.out.println("1. Dodaj słowo i tłumaczenie");
                System.out.println("2. Znajdź tłumaczenie angielskiego słowa");
                System.out.println("3. Sprawdź słowa do powtórki");
                System.out.println("0. Zakończ");
                System.out.print("Wybór: ");

                try {
                    if (scanner.hasNextInt()) {
                        option = scanner.nextInt();
                        scanner.nextLine();

                        switch (option) {
                            case 1:
                                System.out.print("Podaj słowo po angielsku: ");
                                String word = scanner.nextLine();
                                System.out.print("Podaj tłumaczenie: ");
                                String translation = scanner.nextLine();
                                addWordToDatabase(word, translation, connection);
                                break;

                            case 2:
                                System.out.print("Podaj słowo do wyszukania: ");
                                String searchWord = scanner.nextLine();
                                findTranslationWithTypos(connection, searchWord);
                                break;

                            case 3:
                                reviewWordsWithTranslation(connection);
                                break;

                            case 0:
                                System.out.println("Zakończono program.");
                                break;

                            default:
                                System.out.println("Niepoprawny wybór, spróbuj ponownie.");
                                break;
                        }
                    } else {
                        System.out.println("Nieprawidłowy wybór! Proszę wpisać liczbę.");
                        scanner.next();
                    }
                } catch (Exception e) {
                    System.out.println("Błąd: " + e.getMessage());
                    scanner.nextLine();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String dbFileName = "words.db";
        createDatabaseIfNotExists(dbFileName);
        runProgram("jdbc:sqlite:" + dbFileName);
    }

    // Metoda do dodawania słowa do bazy
    public static void addWordToDatabase(String word, String translation, Connection connection) {
        String insertQuery = "INSERT INTO words (word, translation, added_date) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
            LocalDateTime currentDate = LocalDateTime.now();
            stmt.setString(1, word);
            stmt.setString(2, translation);
            stmt.setTimestamp(3, Timestamp.valueOf(currentDate));
            stmt.executeUpdate();
            System.out.println("Dodano słowo: " + word + " z tłumaczeniem: " + translation);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
