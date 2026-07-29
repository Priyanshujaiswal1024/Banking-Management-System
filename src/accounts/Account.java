package accounts;

import util.PasswordUtil;
import java.sql.*;
import java.util.Scanner;

public class Account {
    private static Connection connection;
    private static Scanner scanner;

    public Account(Connection connection, Scanner scanner) {
        this.connection = connection;
        this.scanner = scanner;
    }

    public static long open_account(String email) {
        if (account_exist(email)) {
            throw new RuntimeException("Account Already Exists");
        }

        scanner.nextLine();
        System.out.print("Enter Full Name: ");
        String full_name = scanner.nextLine();
        System.out.print("Enter Initial Amount: ");
        double balance = scanner.nextDouble();
        scanner.nextLine();

        String security_pin;
        // Loop until valid 4-digit PIN entered — earlier version let bad PIN slip through
        while (true) {
            System.out.print("Enter 4 Digit Security Pin: ");
            security_pin = scanner.nextLine();
            if (security_pin.matches("\\d{4}")) {
                break;
            }
            System.out.println("PIN should be exactly 4 digits, try again.");
        }

        String hashedPin = PasswordUtil.hash(security_pin);
        String open_account_query = "Insert into Accounts(account_number, full_name, email, balance, security_pin) values(?,?,?,?,?)";

        try {
            long account_number = generateAccountNumber();
            PreparedStatement ps = connection.prepareStatement(open_account_query);
            ps.setLong(1, account_number);
            ps.setString(2, full_name);
            ps.setString(3, email);
            ps.setDouble(4, balance);
            ps.setString(5, hashedPin);
            int rs = ps.executeUpdate();
            if (rs > 0) {
                return account_number;
            } else {
                throw new RuntimeException("Account Creation Failed");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        throw new RuntimeException("Account Creation Failed");
    }

    public static long getAccount_number(String email) {
        String query = "SELECT account_number from Accounts WHERE email = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("account_number");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Account Not Exists");
    }

    private static long generateAccountNumber() {
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT account_number FROM Accounts order by account_number desc limit 1");
            if (rs.next()) {
                return rs.getLong("account_number") + 1;
            } else {
                return 10000100;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 10000100;
    }

    public static boolean account_exist(String email) {
        String q = "Select account_number from Accounts WHERE email = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(q);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}