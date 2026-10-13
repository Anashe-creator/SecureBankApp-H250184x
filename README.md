# Secure Banking Application

## Description
A console-based banking application written in Java that demonstrates Object-Oriented Programming
(abstraction, inheritance, polymorphism, encapsulation) together with secure coding practices.
Customers can register, log in, open accounts, deposit and withdraw money, and view their balance
and transaction history. All data is saved in plain text files and reloaded when the app starts.

## Student Details
- **Name:** CHERYL CHARINGENO
- **Registration Number:** H250184X

## Features
- **User authentication** - registration and login with a strong password policy
- **Account management** - create Savings or Current accounts (up to 5 per user) and view balances
- **Transactions** - deposit and withdraw funds, with a per-transaction limit per account type
- **Transaction history** - every account opening, deposit and withdrawal is recorded with date/time
- **Data persistence** - users, accounts and transactions are stored in text files in `data/` and loaded on start-up
- **Secure coding** - see the section below

## Security Measures
| Area | Practice used |

| Password storage | Passwords are never stored. Each is hashed with **PBKDF2-HMAC-SHA256** (600,000 iterations) and a unique random 16-byte salt |
| Password checking | Constant-time comparison (`MessageDigest.isEqual`) |
| Brute force | Account is locked for 5 minutes after 3 failed logins (the lock survives restarts) |
| User enumeration | One generic message, "Invalid username or password"; a dummy hash is computed for unknown users so response time is the same |
| Password handling | Passwords are held as `char[]` and wiped after use; hidden input when run in a real terminal |
| Input validation | Allow-list regular expressions for usernames, account numbers and amounts; length limit on all input |
| Money handling | `BigDecimal` with exactly 2 decimal places (no floating point errors); no negative, zero or over-limit amounts |
| Access control | Every banking operation re-checks that the account belongs to the logged-in user |
| Safe errors | Users see friendly messages only - no stack traces or file paths |
| Data integrity | Files are rewritten atomically (temp file + move); failed saves are rolled back in memory; delimiter/line-break injection is blocked; corrupted files stop start-up instead of loading bad data |
| File permissions | Data files are restricted to the owner on Linux/macOS |
| Secure randomness | `SecureRandom` for salts and account numbers |

## Project Structure
SecureBankApp-REGNUMBER/
├── README.md
├── .gitignore
├── data/            users.txt, accounts.txt, transactions.txt
└── src/             12 .java files, all in one folder

The code is split into layers (UI -> service -> repository -> files) so each class has one job.

## How to Install and Run
**Requirement:** Java Development Kit (JDK) 11 or newer (tested with JDK 21). Check with `java -version` and `javac -version`.

### Linux / macOS
```bash
git clone https://github.com/YOUR-USERNAME/SecureBankApp-REGNUMBER.git
cd SecureBankApp-REGNUMBER
./build.sh
./run.sh

### Windows (Command Prompt)
bat
git clone https://github.com/YOUR-USERNAME/SecureBankApp-REGNUMBER.git
cd SecureBankApp-REGNUMBER
build.bat
run.bat

### Manual commands (any system)
```bash
javac -d out src/com/securebank/*.java src/com/securebank/*/*.java
java -cp out com.securebank.Main

Run the program from the project folder so it finds the `data/` folder, or choose another folder with
`java -Dsecurebank.data=/path/to/folder -cp out com.securebank.Main`.

> Tip: run from a real terminal so password typing is hidden. Some IDE consoles do not support this;
> the app then warns that input will be visible.

## Usage
1. Choose **Register**, then pick a username (3-20 letters, digits or `_`) and a password
   (8-64 characters with upper case, lower case, a digit and a special character, no spaces).
2. Choose **Login**, then **Create new account**.
3. Use **Deposit**, **Withdraw**, **View balance** and **Transaction history**.

Account limits: max 1,000,000.00 per transaction; Savings withdrawals up to 5,000.00 and Current
withdrawals up to 20,000.00 per transaction.

## Known Limitations
The text files are not encrypted, so someone with access to the computer could read or edit them
(passwords are safe because only salted hashes are stored, but balances could be changed by editing
`accounts.txt`). A production system would use an encrypted database with access control.
