package com.securebank;

import com.securebank.exception.StorageException;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.TransactionRepository;
import com.securebank.repository.UserRepository;
import com.securebank.service.AuthService;
import com.securebank.service.BankService;
import com.securebank.ui.ConsoleUI;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application entry point: loads the data files, wires the layers together and
 * starts the console interface. The data folder defaults to ./data and can be
 * changed with -Dsecurebank.data=/some/folder
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        // JDK 22+ uses a different console implementation that can drop hidden password
        // input on some Windows terminals. Use the classic one unless the user chose another.
        if (System.getProperty("jdk.console") == null) {
            System.setProperty("jdk.console", "java.base");
        }
        Path dataDir = Paths.get(System.getProperty("securebank.data", "data"));
        try {
            UserRepository users = new UserRepository(dataDir.resolve("users.txt"));
            AccountRepository accounts = new AccountRepository(dataDir.resolve("accounts.txt"));
            TransactionRepository transactions =
                    new TransactionRepository(dataDir.resolve("transactions.txt"));

            new ConsoleUI(new AuthService(users), new BankService(accounts, transactions)).run();
        } catch (StorageException e) {
            System.err.println("Cannot start: " + e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.println("Unexpected error. The application will now close.");
            System.exit(2);
        }
    }
}
