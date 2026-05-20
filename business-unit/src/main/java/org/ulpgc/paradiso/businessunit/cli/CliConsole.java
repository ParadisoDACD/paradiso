package org.ulpgc.paradiso.businessunit.cli;

import java.util.OptionalInt;
import java.util.Scanner;

public class CliConsole {

    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String CYAN = "\033[36m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String RED = "\033[31m";
    private static final String DIM = "\033[2m";

    private final Scanner scanner;

    public CliConsole() {
        scanner = new Scanner(System.in);
    }

    public void printBanner() {
        System.out.println();
        System.out.println(CYAN + BOLD + "╔═════════════════════════════════════════════════╗" + RESET);
        System.out.println(CYAN + BOLD + "║      Paradiso — Planificador de conciertos      ║" + RESET);
        System.out.println(CYAN + BOLD + "║       Eventos en Londres + Transporte TfL       ║" + RESET);
        System.out.println(CYAN + BOLD + "╚═════════════════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    public void printTitle(String title) {
        System.out.println();
        System.out.println(BOLD + CYAN + "── " + title + " ──" + RESET);
        System.out.println();
    }

    public void printLine(String text) {
        System.out.println(text);
    }

    public void printSuccess(String text) {
        System.out.println(GREEN + "✓ " + text + RESET);
    }

    public void printWarning(String text) {
        System.out.println(YELLOW + "⚠ " + text + RESET);
    }

    public void printDim(String text) {
        System.out.println(DIM + text + RESET);
    }

    public void printNumbered(int number, String text) {
        System.out.printf("  " + BOLD + "[%d]" + RESET + " %s%n", number, text);
    }

    public void printSeparator() {
        System.out.println(DIM + "──────────────────────────────────────────────────" + RESET);
    }

    public void printEmpty() {
        System.out.println();
    }

    public String readLine(String prompt) {
        System.out.print(BOLD + GREEN + prompt + RESET + " ");
        if (!scanner.hasNextLine()) throw new IllegalStateException("Entrada estándar cerrada");
        return scanner.nextLine().trim();
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            OptionalInt value = parsedInt(readLine(prompt), min, max);
            if (value.isPresent()) return value.getAsInt();
        }
    }

    public void close() {
        scanner.close();
    }

    private OptionalInt parsedInt(String input, int min, int max) {
        try {
            return validInt(Integer.parseInt(input), min, max);
        } catch (NumberFormatException e) {
            return invalidInt("Eso no es un número válido. Inténtalo de nuevo.");
        }
    }

    private OptionalInt validInt(int value, int min, int max) {
        if (value >= min && value <= max) return OptionalInt.of(value);
        return invalidInt("Introduce un número entre " + min + " y " + max + ".");
    }

    private OptionalInt invalidInt(String message) {
        System.out.println(RED + "✗ " + message + RESET);
        return OptionalInt.empty();
    }
}