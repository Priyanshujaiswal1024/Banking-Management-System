package transactions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Transactions {

    private static Connection connection;


    public Transactions(Connection connection) {
        Transactions.connection = connection;
    }

    public static void insertTransaction(
            long accountNumber,
            String type,
            double amount
    ) {
        String sql = "INSERT INTO Transactions (account_number, transaction_type, amount) VALUES (?, ?, ?)";

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, accountNumber);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void miniStatement(long accountNumber) {

        String sql = """
                SELECT a.full_name,
               t.transaction_type,
               t.amount,
               t.transaction_time
        FROM Transactions t
        JOIN Accounts a
        ON t.account_number = a.account_number
        WHERE t.account_number = ?
        ORDER BY t.transaction_time DESC
        """;

        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, accountNumber);
            ResultSet rs = ps.executeQuery();

            System.out.println("\n================ TRANSACTION HISTORY ================");
            System.out.printf("| %-15s | %-8s | %-10s | %-10s | %-8s |\n",
                    "NAME", "TYPE", "AMOUNT", "DATE", "TIME");
            System.out.println("----------------------------------------------------");

            boolean found = false;
            while (rs.next()) {
                found = true;
                String name = rs.getString("full_name");
                String type = rs.getString("transaction_type");
                double amount = rs.getDouble("amount");
                var ts = rs.getTimestamp("transaction_time");

                String date = ts.toLocalDateTime().toLocalDate().toString();
                String time = ts.toLocalDateTime().toLocalTime().toString();

                System.out.printf("| %-15s | %-8s | %-10.2f | %-10s | %-8s |\n",
                        name, type, amount, date, time);
            }

            if (!found) {
                System.out.println("| No transactions found                             |");
            }

            System.out.println("=====================================================");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
