package app;

import accounts.AccountManager;
import config.DBconnection;
import accounts.Account;
import User.User;
import transactions.Transactions;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;


public class BankingApp {
    static void main(String[] args) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        Connection connection = DBconnection.getConnection();
        if (connection == null) {
            System.out.println(" DataBase Connection Failed! ");
            return;
        }
        User user = new User(connection, scanner);
        Account account = new Account(connection, scanner);
        AccountManager accountManager = new AccountManager(connection, scanner);
        Transactions transactions = new Transactions(connection);
        String email;
        long account_Number;
        while (true) {
            try {
                System.out.println("\n*** WELCOME TO BANKING SYSTEM ***");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Enter choice: ");
                int choice1 = scanner.nextInt();

                switch (choice1) {
                    case 1:
                        User.register();
                        break;
                    case 2:
                        email = User.login();
                        if (email != null) {
                            System.out.println("Logged in as: " + email + " Successfully logged in");
                            if (!Account.account_exist(email)) {
                                System.out.println("1. Open a new  Account");
                                System.out.println("2. Exit");

                                if (scanner.nextInt() == 1) {
                                    account_Number = Account.open_account(email);

                                    System.out.println("Account opened successfully  your  Account number : " + account_Number);
                                } else {
                                    break;
                                }
                            }
                            account_Number = Account.getAccount_number(email);
                            int choice2 = 0;
                            while (choice2 != 6) {
                                System.out.println("\n1. Debit");
                                System.out.println("2. Credit");
                                System.out.println("3. Transfer");
                                System.out.println("4. Check Balance");
                                System.out.println("5. Transaction statement");
                                System.out.println("6. Logout");

                                System.out.print("Choice: ");
                                choice2 = scanner.nextInt();
                                switch (choice2) {
                                    case 1:
                                        AccountManager.debitMoney(account_Number);
                                        break;
                                    case 2:
                                        AccountManager.creditMoney(account_Number);
                                        break;
                                    case 3:
                                        AccountManager.TransferMoney(account_Number);
                                        break;
                                    case 4:
                                        AccountManager.getBalance(account_Number);
                                        break;
                                    case 5:
                                        AccountManager.showTransactions(account_Number);
                                        break;
                                    case 6:
                                        System.out.println("Logged out 🔒");
                                        break;

                                    default:
                                        System.out.println("Invalid choice");
                                }
                            }


                        } else {
                            System.out.println("Invalid email or password");
                        }
                        break;

                    case 3:
                        System.out.println("Thankyou for using Banking System");
                        return;
                    default:
                        System.out.println("Invalid choice");
                }
            }
            catch (Exception e) {
                System.out.println("1" +
                        " Invalid input! Please enter numbers only.");
                scanner.nextLine();
            }
        }
    }

}


