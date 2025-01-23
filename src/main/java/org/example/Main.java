//package org.example;
//
//import java.util.Scanner;
//
//public class Main {
//    public static void main(String[] args) {
//        DatabaseManager db = new DatabaseManager();
//        Translator translator = new Translator();
//        Scanner scanner = new Scanner(System.in);
//
//        while (true) {
//            System.out.println("Choose an option: [1] Add Word, [2] Translate, [3] List Words, [0] Exit");
//            int choice = scanner.nextInt();
//            scanner.nextLine(); // Clear buffer
//
//            try {
//                switch (choice) {
//                    case 1 -> {
//                        System.out.print("Enter English word: ");
//                        String englishWord = scanner.nextLine();
//                        db.insertWord(englishWord);
//                    }
//                    case 2 -> {
//                        System.out.print("Enter English word to translate: ");
//                        String word = scanner.nextLine();
//                        String translation = translator.translateToPolish(word);
//                        db.updateTranslation(word, translation);
//                        System.out.println("Polish Translation: " + translation);
//                    }
//                    case 3 -> db.listWords();
//                    case 0 -> System.exit(0);
//                    default -> System.out.println("Invalid option.");
//                }
//            } catch (Exception e) {
//                System.err.println("Error: " + e.getMessage());
//            }
//        }
//    }
//}
