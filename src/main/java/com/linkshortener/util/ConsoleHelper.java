package com.linkshortener.util;

import java.util.Scanner;

/**
 * Хелпер для работы с консолью.
 */
public class ConsoleHelper {
    private static final Scanner scanner = new Scanner(System.in);

    private ConsoleHelper() {}

    /**
     * Читает строку с приглашением.
     */
    public static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /**
     * Читает целое число с приглашением.
     */
    public static int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: введите целое число");
            }
        }
    }

    /**
     * Очищает консоль (кроссплатформенно).
     */
    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Игнорируем ошибки очистки
            System.out.println("\n".repeat(50));
        }
    }

    /**
     * Выводит разделитель.
     */
    public static void printSeparator() {
        System.out.println("=".repeat(60));
    }

    /**
     * Выводит заголовок.
     */
    public static void printTitle(String title) {
        printSeparator();
        System.out.println(" ".repeat((60 - title.length()) / 2) + title);
        printSeparator();
    }

    /**
     * Ожидает нажатия Enter.
     */
    public static void waitForEnter() {
        System.out.println("\nНажмите Enter для продолжения...");
        scanner.nextLine();
    }
}