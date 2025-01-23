package org.example;
import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:C:/Users/rbucz/base_words/words.db";

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public void insertWord(String englishWord) throws SQLException {
        String sql = "INSERT INTO words (english_word) VALUES (?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, englishWord);
            pstmt.executeUpdate();
        }
    }

    public void updateTranslation(String englishWord, String polishTranslation) throws SQLException {
        String sql = "UPDATE words SET polish_translation = ? WHERE english_word = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, polishTranslation);
            pstmt.setString(2, englishWord);
            pstmt.executeUpdate();
        }
    }

    public void listWords() throws SQLException {
        String sql = "SELECT english_word, polish_translation FROM words";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println("English: " + rs.getString("english_word") +
                        ", Polish: " + rs.getString("polish_translation"));
            }
        }
    }
}
