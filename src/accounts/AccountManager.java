package accounts;

import transactions.Transactions;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Scanner;

public class AccountManager {
    private static Scanner scanner;
    private static Connection connection;
    private static final int MAX_ATTEMPTS = 3;
    private static final double MIN_BALANCE = 500.0;

    public AccountManager(Connection connection, Scanner scanner) {
        this.connection = connection;
        this.scanner = scanner;
    }

    // Central PIN check with lock logic
    private static boolean verifyPin(long account_number, String enteredPin) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "SELECT security_pin, failed_attempts, is_locked FROM Accounts WHERE account_number = ?");
        ps.setLong(1, account_number);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            System.out.println("Account not found!");
            return false;
        }

        if (rs.getBoolean("is_locked")) {
            System.out.println("Account is LOCKED due to multiple failed PIN attempts. Contact support.");
            return false;
        }

        String storedHash = rs.getString("security_pin");
        if (PasswordUtil.verify(enteredPin, storedHash)) {
            PreparedStatement reset = connection.prepareStatement(
                    "UPDATE Accounts SET failed_attempts = 0 WHERE account_number = ?");
            reset.setLong(1, account_number);
            reset.executeUpdate();
            return true;
        } else {
            int attempts = rs.getInt("failed_attempts") + 1;
            if (attempts >= MAX_ATTEMPTS) {
                PreparedStatement lock = connection.prepareStatement(
                        "UPDATE Accounts SET failed_attempts = ?, is_locked = 1 WHERE account_number = ?");
                lock.setInt(1, attempts);
                lock.setLong(2, account_number);
                lock.executeUpdate();
                System.out.println("Invalid PIN! Account locked after " + MAX_ATTEMPTS + " failed attempts.");
            } else {
                PreparedStatement update = connection.prepareStatement(
                        "UPDATE Accounts SET failed_attempts = ? WHERE account_number = ?");
                update.setInt(1, attempts);
                update.setLong(2, account_number);
                update.executeUpdate();
                System.out.println("Invalid Security Pin! Attempts left: " + (MAX_ATTEMPTS - attempts));
            }
            return false;
        }
    }

    // Feature 2: amount validation helper
    private static boolean isValidAmount(double amount) {
        if (amount <= 0) {
            System.out.println("Amount must be greater than zero!");
            return false;
        }
        return true;
    }

    public static void creditMoney(long account_number) throws SQLException {
        scanner.nextLine();
        System.out.println("Enter amount to credit: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();

        if (!isValidAmount(amount)) return;

        System.out.println("Enter Security Pin: ");
        String securityPin = scanner.nextLine();

        try {
            connection.setAutoCommit(false);
            if (account_number != 0 && verifyPin(account_number, securityPin)) {
                PreparedStatement preparedStatement1 = connection.prepareStatement(
                        "UPDATE Accounts SET balance = balance + ? WHERE account_number = ?");
                preparedStatement1.setDouble(1, amount);
                preparedStatement1.setLong(2, account_number);
                int rowsAffected = preparedStatement1.executeUpdate();
                if (rowsAffected > 0) {
                    Transactions.insertTransaction(account_number, "CREDIT", amount);
                    System.out.println("Rs." + amount + " credited Successfully");
                    connection.commit();
                } else {
                    System.out.println("Transaction Failed!");
                    connection.rollback();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connection.setAutoCommit(true);
    }

    public static void debitMoney(long account_number) throws SQLException {
        scanner.nextLine();
        System.out.println("Enter amount to debit: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();

        if (!isValidAmount(amount)) return;

        System.out.println("Enter Security Pin: ");
        String securityPin = scanner.nextLine();

        try {
            connection.setAutoCommit(false);
            if (account_number != 0 && verifyPin(account_number, securityPin)) {
                PreparedStatement balPs = connection.prepareStatement(
                        "SELECT balance FROM Accounts WHERE account_number = ?");
                balPs.setLong(1, account_number);
                ResultSet rs = balPs.executeQuery();
                if (rs.next()) {
                    double current_balance = rs.getDouble("balance");

                    // Feature 1: minimum balance check
                    if (current_balance - amount < MIN_BALANCE) {
                        System.out.println("Minimum balance of Rs." + MIN_BALANCE + " must be maintained!");
                        connection.setAutoCommit(true);
                        return;
                    }

                    PreparedStatement preparedStatement1 = connection.prepareStatement(
                            "update Accounts set balance = balance - ? where account_number = ?");
                    preparedStatement1.setDouble(1, amount);
                    preparedStatement1.setLong(2, account_number);
                    int rowsAffected = preparedStatement1.executeUpdate();
                    if (rowsAffected > 0) {
                        Transactions.insertTransaction(account_number, "DEBIT", amount);
                        System.out.println("Rs." + amount + " debited Successfully");
                        connection.commit();
                    } else {
                        System.out.println("Transaction Failed!");
                        connection.rollback();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connection.setAutoCommit(true);
    }

    public static void TransferMoney(long sender_account_number) throws SQLException {
        scanner.nextLine();
        System.out.println("Enter Receiver Account Number to Transfer: ");
        long reciver_account_number = scanner.nextLong();
        scanner.nextLine();

        // Feature 3: self-transfer block
        if (sender_account_number == reciver_account_number) {
            System.out.println("Cannot transfer money to your own account!");
            return;
        }

        System.out.println("Enter amount to transfer: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();

        if (!isValidAmount(amount)) return;

        System.out.println("Enter Security Pin: ");
        String securityPin = scanner.nextLine();

        try {
            connection.setAutoCommit(false);
            if (sender_account_number != 0 && reciver_account_number != 0
                    && verifyPin(sender_account_number, securityPin)) {

                // check receiver exists
                PreparedStatement checkReceiver = connection.prepareStatement(
                        "SELECT account_number FROM Accounts WHERE account_number = ?");
                checkReceiver.setLong(1, reciver_account_number);
                if (!checkReceiver.executeQuery().next()) {
                    System.out.println("Receiver account does not exist!");
                    connection.setAutoCommit(true);
                    return;
                }

                PreparedStatement balPs = connection.prepareStatement(
                        "SELECT balance FROM Accounts WHERE account_number = ?");
                balPs.setLong(1, sender_account_number);
                ResultSet rs = balPs.executeQuery();

                if (rs.next()) {
                    double current_balance = rs.getDouble("balance");

                    // Feature 1: minimum balance check
                    if (current_balance - amount < MIN_BALANCE) {
                        System.out.println("Minimum balance of Rs." + MIN_BALANCE + " must be maintained!");
                        connection.setAutoCommit(true);
                        return;
                    }

                    PreparedStatement debit = connection.prepareStatement(
                            "update Accounts set balance = balance - ? where account_number = ?");
                    debit.setDouble(1, amount);
                    debit.setLong(2, sender_account_number);

                    PreparedStatement credit = connection.prepareStatement(
                            "update Accounts set balance = balance + ? where account_number = ?");
                    credit.setDouble(1, amount);
                    credit.setLong(2, reciver_account_number);

                    int r1 = debit.executeUpdate();
                    int r2 = credit.executeUpdate();

                    if (r1 > 0 && r2 > 0) {
                        Transactions.insertTransaction(sender_account_number, "TRANSFER", amount);
                        Transactions.insertTransaction(reciver_account_number, "CREDIT", amount);

                        String receiverName = "Receiver";
                        PreparedStatement namePs = connection.prepareStatement(
                                "SELECT full_name FROM Accounts WHERE account_number = ?");
                        namePs.setLong(1, reciver_account_number);
                        ResultSet nameRs = namePs.executeQuery();
                        if (nameRs.next()) receiverName = nameRs.getString("full_name");

                        System.out.println("Transaction Successful!");
                        System.out.println("Rs." + amount + " Transfer Successfully to " + receiverName);
                        connection.commit();
                    } else {
                        System.out.println("Transaction Failed!");
                        connection.rollback();
                    }
                }
            } else if (sender_account_number == 0 || reciver_account_number == 0) {
                System.out.println("Invalid Account Number!");
            }
        } catch (SQLException e) {
            connection.rollback();
            throw new RuntimeException(e);
        }
        connection.setAutoCommit(true);
    }

    public static void getBalance(long account_number) throws SQLException {
        scanner.nextLine();
        System.out.println("Enter Security Pin: ");
        String securityPin = scanner.nextLine();

        if (verifyPin(account_number, securityPin)) {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT balance FROM Accounts WHERE account_number = ?");
            ps.setLong(1, account_number);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("Balance: " + rs.getDouble("balance"));
            }
        }
    }

    // Feature 4: date range filter + Feature 6: export to file (both handled inside Transactions.miniStatement)
    public static void showTransactions(long account_number) throws SQLException {
        scanner.nextLine();
        System.out.print("Enter Security Pin: ");
        String pin = scanner.nextLine();

        if (!verifyPin(account_number, pin)) return;

        System.out.println("1. View All Transactions");
        System.out.println("2. View Last N Days");
        System.out.print("Choice: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        Timestamp fromDate = null;
        if (choice == 2) {
            System.out.print("Enter number of days: ");
            int days = scanner.nextInt();
            scanner.nextLine();
            fromDate = Timestamp.valueOf(java.time.LocalDateTime.now().minusDays(days));
        }

        Transactions.miniStatement(account_number, fromDate);
    }

    // Feature 5: change PIN
    public static void changePin(long account_number) throws SQLException {
        scanner.nextLine();
        System.out.print("Enter Current Security Pin: ");
        String oldPin = scanner.nextLine();

        if (!verifyPin(account_number, oldPin)) {
            System.out.println("Cannot change PIN — current PIN verification failed.");
            return;
        }

        String newPin;
        while (true) {
            System.out.print("Enter New 4 Digit Security Pin: ");
            newPin = scanner.nextLine();
            if (newPin.matches("\\d{4}")) break;
            System.out.println("PIN should be exactly 4 digits, try again.");
        }

        String hashedNewPin = PasswordUtil.hash(newPin);
        PreparedStatement ps = connection.prepareStatement(
                "UPDATE Accounts SET security_pin = ? WHERE account_number = ?");
        ps.setString(1, hashedNewPin);
        ps.setLong(2, account_number);
        int rows = ps.executeUpdate();

        if (rows > 0) {
            System.out.println("PIN changed successfully!");
        } else {
            System.out.println("PIN change failed!");
        }
    }

    public static String maskAccountNumber(long accNo) {
        String s = String.valueOf(accNo);
        return "XXXX-XXXX-" + s.substring(s.length() - 4);
    }
}