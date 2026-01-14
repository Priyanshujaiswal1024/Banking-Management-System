package User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class User {
    private static Connection connection;
    private static Scanner scanner;
    public User(Connection connection, Scanner scanner) {
        this.connection = connection;
        this.scanner = scanner;
    }

    public static void register()
    {
        scanner.nextLine();
        System.out.print("Enter Full name: ");
        String full_name = scanner.nextLine();
        System.out.print("Enter email: ");
        String email = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        if (!password.matches("\\d{8}")) {
            System.out.println(" Password must be exactly 8 digits");
            return;
        }
        if(user_exist(email))
        {
            System.out.println("User already exists for this email Address !!");
            return;
        }
        String register_query = "INSERT INTO User (username, email, password) VALUES (?, ?, ?)";
        try{
            PreparedStatement preparedStatement = connection.prepareStatement(register_query);
            preparedStatement.setString(1, full_name);
            preparedStatement.setString(2, email);
                preparedStatement.setString(3, password);
                int rs=preparedStatement.executeUpdate();
                if (rs > 0)
                {
                    System.out.println("User registered successfully!");
                }
                else
                {
                    System.out.println("User not registered!");
                }

        }
        catch (SQLException e){
            System.out.println(e.getMessage());
        }


    }

    public static String login()
    {
        scanner.nextLine();

        System.out.print("Enter email: ");
        String email = scanner.next();
        System.out.print("Enter password: ");
        String password = scanner.next();
        String login_query = "SELECT * FROM User WHERE email = ? AND password = ?";
        try
        {
            PreparedStatement ps=connection.prepareStatement(login_query);
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs=ps.executeQuery();
            if(rs.next())
            {
                return email;
            }
            else {
                return null;
            }
        }
        catch (SQLException e)
        {
            System.out.println(e.getMessage());
        }

        return null;

    }
    public static boolean user_exist(String email)
    {
        String query = "SELECT * FROM User WHERE email = ?";
        try
        {
            PreparedStatement ps= connection.prepareStatement(query);
            ps.setString(1, email);
            ResultSet rs=ps.executeQuery();
            if(rs.next())
            {
                return true;
            }
            else {
                return false;
            }

        }
        catch (SQLException e)
        {
            System.out.println(e.getMessage());
        }
        return  false;
    }

}
