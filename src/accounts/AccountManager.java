package accounts;

import transactions.Transactions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class AccountManager {
    private static Scanner scanner;
    private static Connection connection;

    public AccountManager(Connection connection, Scanner scanner) {
        this.connection = connection;
        this.scanner = scanner;
    }

    public static void creditMoney(long account_number) throws SQLException {
        scanner.nextLine();
        System.out.println("Enter amount to credit: ");
        double amount = scanner.nextInt();
        scanner.nextLine();
        System.out.println("Enter Security Pin: ");
        String securityPin = scanner.nextLine();

        try {
            connection.setAutoCommit(false);
            if (account_number != 0) {
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM Accounts WHERE account_number = ? and security_pin = ? ");
                preparedStatement.setLong(1, account_number);
                preparedStatement.setString(2, securityPin);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    String credit_query = "UPDATE Accounts SET balance = balance + ? WHERE account_number = ?";
                    PreparedStatement preparedStatement1 = connection.prepareStatement(credit_query);
                    preparedStatement1.setDouble(1, amount);
                    preparedStatement1.setLong(2, account_number);
                    int rowsAffected = preparedStatement1.executeUpdate();
                    if (rowsAffected > 0) {
                        Transactions.insertTransaction(
                                 account_number, "CREDIT", amount
                        );

                        System.out.println("Rs." + amount + " credited Successfully");
                        connection.commit();
                        connection.setAutoCommit(true);
                        return;
                    } else {
                        System.out.println("Transaction Failed!");
                        connection.rollback();
                        connection.setAutoCommit(true);
                    }
                } else {
                    System.out.println("Invalid Security Pin!");
                }


            }


        } catch (SQLException e) {
           e.printStackTrace();
        }
        connection.setAutoCommit(true);
    }
    public static void debitMoney(long account_number) throws SQLException {
        scanner.nextLine();
        System.out.println("Enter amount to dedit: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();
        System.out.println("Enter Security Pin: ");
        String securityPin = scanner.nextLine();
        try {
            connection.setAutoCommit(false);
            if (account_number != 0) {
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM Accounts WHERE account_number = ? and security_pin = ? ");
                preparedStatement.setLong(1, account_number);
                preparedStatement.setString(2, securityPin);
                ResultSet resultSet = preparedStatement.executeQuery();
                if(resultSet.next()) {
                    double current_balance = resultSet.getDouble("balance");
                    if(current_balance >= amount) {
                        String debit_query="update Accounts set balance = balance - ? where account_number = ?";
                      PreparedStatement preparedStatement1 = connection.prepareStatement(debit_query);
                      preparedStatement1.setDouble(1, amount);
                      preparedStatement1.setLong(2, account_number);
                      int rowsAffected = preparedStatement1.executeUpdate();
                      if (rowsAffected > 0) {
                          Transactions.insertTransaction(
                                   account_number, "DEBIT", amount
                          );
                          System.out.println("Rs." + amount + " debit Successfully");
                          connection.commit();
                          connection.setAutoCommit(true);
                          return;
                      }
                      else  {
                          System.out.println("Transaction Failed!");
                          connection.rollback();
                          connection.setAutoCommit(true);
                      }
                    }
                    else{
                        System.out.println("Insufficient Balance!");
                    }

                }
                else {
                    System.out.println("Invalid Security Pin!");
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        connection.setAutoCommit(true);
    }
    public static void TransferMoney(long sender_account_number) throws SQLException {
        scanner.nextLine();
        System.out.println("Enter Reciver Account Number to Transfer: ");
        long reciver_account_number = scanner.nextLong();
        scanner.nextLine();
        System.out.println("Enter amount to transfer: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();
        System.out.println("Enter Security Pin: ");
        String securityPin = scanner.nextLine();
        try {
        connection.setAutoCommit(false);
        if(sender_account_number != 0&& reciver_account_number != 0) {
            PreparedStatement ps= connection.prepareStatement("select * from Accounts where account_number = ? and security_pin = ? ");
            ps.setLong(1, sender_account_number);
            ps.setString(2, securityPin);
            ResultSet resultSet = ps.executeQuery();
            if(resultSet.next()) {
                double current_balance = resultSet.getDouble("balance");
                if(current_balance >= amount) {
                    String debit_query="update Accounts set balance = balance - ? where account_number = ?";
                    String credit_query="update Accounts set balance = balance + ? where account_number = ?";
                    PreparedStatement preparedStatement1 = connection.prepareStatement(debit_query);
                    preparedStatement1.setDouble(1, amount);
                    preparedStatement1.setLong(2, sender_account_number);


                    PreparedStatement preparedStatement2 = connection.prepareStatement(credit_query);
                    preparedStatement2.setDouble(1, amount);
                    preparedStatement2.setLong(2, reciver_account_number);
                    int rowsAffected1 = preparedStatement1.executeUpdate();
                    int rowsAffected2 = preparedStatement2.executeUpdate();
                    if (rowsAffected1 > 0 &&  rowsAffected2 > 0) {
                        transactions.Transactions.insertTransaction(
                                 sender_account_number, "TRANSFER", amount
                        );

                        transactions.Transactions.insertTransaction(
                                 reciver_account_number, "CREDIT", amount
                        );
                        String nameQuery = "SELECT full_name FROM Accounts WHERE account_number = ?";
                        PreparedStatement namePs = connection.prepareStatement(nameQuery);
                        namePs.setLong(1, reciver_account_number);
                        ResultSet nameRs = namePs.executeQuery();

                        String receiverName = "Receiver";
                        if (nameRs.next()) {
                            receiverName = nameRs.getString("full_name");
                        }

                        System.out.println( "Transaction Successful!");
                        System.out.println("Rs." + amount + " Transfer Successfully");
                        System.out.println(amount + " Transfer  to"+  receiverName);
                        connection.commit();
                        connection.setAutoCommit(true);
                        return;
                    }
                    else  {
                        System.out.println("Transaction Failed!");
                        connection.rollback();
                        connection.setAutoCommit(true);
                    }
                }
                else {
                    System.out.println("Insufficient Balance!");
                }

            }
            else {
                System.out.println("Invalid Security Pin!");
            }

        }
        else {
            System.out.println("Invalid Account  Number!");
        }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        connection.setAutoCommit(true);
    }


   public static void  getBalance(long account_number) throws SQLException {
        scanner.nextLine();
        System.out.println("Enter Security Pin: ");
        String securityPin = scanner.nextLine();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT balance FROM Accounts WHERE account_number = ? AND security_pin = ?");
            preparedStatement.setLong(1, account_number);
            preparedStatement.setString(2, securityPin);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                double balance = resultSet.getDouble("balance");
                System.out.println("Balance: "+balance);
            }else{
                System.out.println("Invalid Pin!");
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void showTransactions(long account_number) throws SQLException {

        scanner.nextLine();
        System.out.print("Enter Security Pin: ");
        String pin = scanner.nextLine();

        PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM Accounts WHERE account_number = ? AND security_pin = ?"
        );
        ps.setLong(1, account_number);
        ps.setString(2, pin);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Transactions.miniStatement(account_number);
        } else {
            System.out.println(" Invalid Security Pin");
        }
    }
}