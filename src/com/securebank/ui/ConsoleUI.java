package com.securebank.ui;

import com.securebank.exception.AuthenticationException;
import com.securebank.exception.BankException;
import com.securebank.exception.StorageException;
import com.securebank.exception.ValidationException;
import com.securebank.model.Account;
import com.securebank.model.AccountType;
import com.securebank.model.Transaction;
import com.securebank.service.AuthService;
import com.securebank.service.BankService;
import com.securebank.util.InputValidator;

import java.io.Console;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/** Text menu interface. Contains no business rules - it only talks to the services. */
public class ConsoleUI {

    private static final int MAX_INPUT_LENGTH = 100;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final AuthService auth;
    private final BankService bank;
    private final Scanner scanner = new Scanner(System.in);
    private final Console console = System.console();

    /** Thrown when standard input is closed (Ctrl+D / Ctrl+Z) so the app can exit cleanly. */
    private static final class InputClosedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public ConsoleUI(AuthService auth, BankService bank) {
        this.auth = auth;
        this.bank = bank;
    }

    public void run() {
        System.out.println("==========================================");
        System.out.println("        SECURE BANKING APPLICATION");
        System.out.println("==========================================");
        try {
            mainMenu();
        } catch (InputClosedException e) {
            System.out.println("\nInput closed. Goodbye.");
        }
    }

    // ---------------------------------------------------------------- menus

    private void mainMenu() {
        while (true) {
            System.out.println("\n--- MAIN MENU ---");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            switch (prompt("Choose an option: ")) {
                case "1":
                    handleRegister();
                    break;
                case "2":
                    String user = handleLogin();
                    if (user != null) {
                        userMenu(user);
                    }
                    break;
                case "3":
                    System.out.println("Thank you for banking with us. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private void userMenu(String username) {
        System.out.println("\nWelcome, " + username + "!");
        while (true) {
            System.out.println("\n--- ACCOUNT MENU (" + username + ") ---");
            System.out.println("1. Create new account");
            System.out.println("2. View all my accounts and balances");
            System.out.println("3. View balance of one account");
            System.out.println("4. Deposit");
            System.out.println("5. Withdraw");
            System.out.println("6. Transaction history");
            System.out.println("7. Logout");
            String choice = prompt("Choose an option: ");
            try {
                switch (choice) {
                    case "1": handleCreateAccount(username); break;
                    case "2": handleListAccounts(username); break;
                    case "3": handleViewBalance(username); break;
                    case "4": handleDeposit(username); break;
                    case "5": handleWithdraw(username); break;
                    case "6": handleHistory(username); break;
                    case "7":
                        System.out.println("You have been logged out.");
                        return;
                    default:
                        System.out.println("Invalid option.");
                }
            } catch (StorageException e) {
                System.out.println("A storage error occurred. Nothing was changed. Please try again.");
            } catch (BankException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------- handlers

    private void handleRegister() {
        System.out.println("\n--- REGISTER ---");
        System.out.println("Username: 3-20 letters, digits or _");
        System.out.println("Password: 8-64 characters with upper case, lower case, a digit and a");
        System.out.println("          special character (e.g. Proud@2026), no spaces.");
        System.out.println("(Nothing appears while you type a password - that is normal.)");
        System.out.println("(Leave the username empty to go back.)");

        // 1. username - ask again until valid
        String username;
        while (true) {
            String input = prompt("Choose a username: ");
            if (input.isEmpty()) {
                return;
            }
            try {
                username = InputValidator.validateUsername(input);
                break;
            } catch (ValidationException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        // 2. password - ask again until it meets every rule and is confirmed
        char[] password = null;
        char[] confirm = null;
        try {
            while (true) {
                clear(password);
                clear(confirm);
                password = readPassword("Choose a password: ");
                System.out.println("  -> " + password.length + " characters received");
                try {
                    InputValidator.validatePassword(password, username);
                } catch (ValidationException e) {
                    System.out.println("Error: " + e.getMessage());
                    System.out.println("Please try again.");
                    continue;
                }
                confirm = readPassword("Confirm password: ");
                if (!Arrays.equals(password, confirm)) {
                    System.out.println("Error: The two passwords do not match. Please try again.");
                    continue;
                }
                break;
            }
            auth.register(username, password);
            System.out.println("Registration successful. You can now log in as '" + username + "'.");
        } catch (StorageException e) {
            System.out.println("A storage error occurred. Registration failed.");
        } catch (BankException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            clear(password);
            clear(confirm);
        }
    }

    private static void clear(char[] chars) {
        if (chars != null) {
            Arrays.fill(chars, '\0');
        }
    }

    /** Returns the username on success, or null if login failed. */
    private String handleLogin() {
        System.out.println("\n--- LOGIN ---");
        String username = prompt("Username: ");
        char[] password = readPassword("Password: ");
        try {
            String user = auth.login(username, password);
            System.out.println("Login successful.");
            return user;
        } catch (AuthenticationException e) {
            System.out.println("Login failed: " + e.getMessage());
            System.out.println("(New here? Choose Register from the main menu first.)");
        } catch (StorageException e) {
            System.out.println("A storage error occurred. Please try again.");
        } finally {
            Arrays.fill(password, '\0');
        }
        return null;
    }

    private void handleCreateAccount(String username) throws BankException {
        System.out.println("\n--- CREATE ACCOUNT ---");
        System.out.println("1. Savings");
        System.out.println("2. Current");
        AccountType type;
        switch (prompt("Account type: ")) {
            case "1": type = AccountType.SAVINGS; break;
            case "2": type = AccountType.CURRENT; break;
            default: throw new ValidationException("Invalid account type.");
        }
        BigDecimal initial = InputValidator.parseAmount(
                prompt("Initial deposit (0 for none): "));
        Account account = bank.createAccount(username, type, initial);
        System.out.println("Account created successfully!");
        System.out.println("Account number: " + account.getAccountNumber());
        System.out.println("Type          : " + account.getType().getLabel());
        System.out.println("Balance       : " + money(account.getBalance()));
    }

    private void handleListAccounts(String username) {
        List<Account> list = bank.getAccounts(username);
        if (list.isEmpty()) {
            System.out.println("You have no accounts yet. Choose option 1 to create one.");
            return;
        }
        System.out.println("\n--- MY ACCOUNTS ---");
        System.out.printf("%-12s %-10s %15s%n", "Account No.", "Type", "Balance");
        for (Account a : list) {
            System.out.printf("%-12s %-10s %15s%n", a.getAccountNumber(),
                    a.getType().getLabel(), money(a.getBalance()));
        }
    }

    private void handleViewBalance(String username) throws BankException {
        String number = selectAccount(username);
        System.out.println("Current balance: " + money(bank.getBalance(username, number)));
    }

    private void handleDeposit(String username) throws BankException {
        String number = selectAccount(username);
        BigDecimal amount = InputValidator.parseAmount(prompt("Amount to deposit: "));
        Transaction tx = bank.deposit(username, number, amount);
        System.out.println("Deposit successful. New balance: " + money(tx.getBalanceAfter()));
    }

    private void handleWithdraw(String username) throws BankException {
        String number = selectAccount(username);
        BigDecimal amount = InputValidator.parseAmount(prompt("Amount to withdraw: "));
        Transaction tx = bank.withdraw(username, number, amount);
        System.out.println("Withdrawal successful. New balance: " + money(tx.getBalanceAfter()));
    }

    private void handleHistory(String username) throws BankException {
        String number = selectAccount(username);
        List<Transaction> history = bank.getHistory(username, number);
        System.out.println("\n--- TRANSACTION HISTORY (" + number + ") ---");
        if (history.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }
        System.out.printf("%-19s %-16s %14s %14s%n", "Date/Time", "Type", "Amount", "Balance");
        for (Transaction t : history) {
            System.out.printf("%-19s %-16s %14s %14s%n", TIME_FORMAT.format(t.getTimestamp()),
                    t.getType().name(), money(t.getAmount()), money(t.getBalanceAfter()));
        }
    }

    // -------------------------------------------------------------- helpers

    /** Lets the user pick one of their accounts (auto-selects if they only have one). */
    private String selectAccount(String username) throws BankException {
        List<Account> list = bank.getAccounts(username);
        if (list.isEmpty()) {
            throw new ValidationException("You have no accounts yet. Create one first.");
        }
        if (list.size() == 1) {
            String only = list.get(0).getAccountNumber();
            System.out.println("Using account " + only + " (" + list.get(0).getType().getLabel() + ")");
            return only;
        }
        System.out.println("Your accounts:");
        for (Account a : list) {
            System.out.println("  " + a.getAccountNumber() + "  " + a.getType().getLabel());
        }
        return prompt("Enter account number: ");
    }

    private static String money(BigDecimal value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    private String prompt(String message) {
        System.out.print(message);
        if (!scanner.hasNextLine()) {
            throw new InputClosedException();
        }
        String line = scanner.nextLine().trim();
        return line.length() > MAX_INPUT_LENGTH ? line.substring(0, MAX_INPUT_LENGTH) : line;
    }

    /** Reads a password without echoing it when a real console is available. */
    private char[] readPassword(String message) {
        if (console != null) {
            char[] pw = console.readPassword("%s", message);
            if (pw == null) {
                throw new InputClosedException();
            }
            return pw;
        }
        System.out.print(message + "(input will be visible - no secure console) ");
        if (!scanner.hasNextLine()) {
            throw new InputClosedException();
        }
        return scanner.nextLine().trim().toCharArray();
    }
}
