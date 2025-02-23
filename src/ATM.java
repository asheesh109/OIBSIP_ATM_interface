import java.sql.*;
import java.util.*;

class BankOperations {
    private String loggedInUser;


    public boolean authenticate(String userId, String pin) {
        String query = "SELECT * FROM users WHERE user_id = ? AND pin = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ATM", "root", "Ashish030406");
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, userId);
            ps.setString(2, pin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    loggedInUser = userId;
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error executing query: " + e.getMessage());
        }
        return false;
    }

    void showTransactionHistory() {
        String query = "SELECT * FROM transactions WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ATM", "root", "Ashish030406");
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, loggedInUser);
            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println("\nTransaction History:");
                while (rs.next()) {
                    System.out.println(rs.getString("date") + " - " + rs.getString("type") + " - " + rs.getDouble("amount"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    void checkBalance() {
        String query = "SELECT balance FROM users WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ATM", "root", "Ashish030406");
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, loggedInUser);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Current Balance: " + rs.getDouble("balance"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void deposit() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter amount to deposit: ");
        double amount = scanner.nextDouble();

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ATM", "root", "Ashish030406")) {
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE user_id = ?")) {
                stmt.setDouble(1, amount);
                stmt.setString(2, loggedInUser);
                stmt.executeUpdate();
            }
            recordTransaction(conn, loggedInUser, "Deposit", amount);
            System.out.println("Deposit successful!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void withdraw() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter amount to withdraw: ");
        double amount = scanner.nextDouble();

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ATM", "root", "Ashish030406")) {
            conn.setAutoCommit(false);

            String balanceQuery = "SELECT balance FROM users WHERE user_id = ?";
            try (PreparedStatement balanceStmt = conn.prepareStatement(balanceQuery)) {
                balanceStmt.setString(1, loggedInUser);
                try (ResultSet rs = balanceStmt.executeQuery()) {
                    if (rs.next() && rs.getDouble("balance") >= amount) {
                        try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET balance = balance - ? WHERE user_id = ?")) {
                            updateStmt.setDouble(1, amount);
                            updateStmt.setString(2, loggedInUser);
                            updateStmt.executeUpdate();
                        }
                        recordTransaction(conn, loggedInUser, "Withdraw", amount);
                        conn.commit();
                        System.out.println("Withdrawal successful!");
                    } else {
                        System.out.println("Insufficient balance!");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void transfer() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter recipient user ID: ");
        String recipient = scanner.next();
        System.out.print("Enter amount to transfer: ");
        double amount = scanner.nextDouble();

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ATM", "root", "Ashish030406")) {
            conn.setAutoCommit(false);

            String balanceQuery = "SELECT balance FROM users WHERE user_id = ?";
            try (PreparedStatement balanceStmt = conn.prepareStatement(balanceQuery)) {
                balanceStmt.setString(1, loggedInUser);
                try (ResultSet rs = balanceStmt.executeQuery()) {
                    if (rs.next() && rs.getDouble("balance") >= amount) {
                        try (PreparedStatement withdrawStmt = conn.prepareStatement("UPDATE users SET balance = balance - ? WHERE user_id = ?")) {
                            withdrawStmt.setDouble(1, amount);
                            withdrawStmt.setString(2, loggedInUser);
                            withdrawStmt.executeUpdate();
                        }
                        try (PreparedStatement depositStmt = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE user_id = ?")) {
                            depositStmt.setDouble(1, amount);
                            depositStmt.setString(2, recipient);
                            depositStmt.executeUpdate();
                        }
                        recordTransaction(conn, loggedInUser, "Transfer to " + recipient, amount);
                        recordTransaction(conn, recipient, "Transfer from " + loggedInUser, amount);
                        conn.commit();
                        System.out.println("Transfer successful!");
                    } else {
                        System.out.println("Insufficient balance!");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void recordTransaction(Connection conn, String userId, String type, double amount) throws SQLException {
        String query = "INSERT INTO transactions (user_id, type, amount) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userId);
            stmt.setString(2, type);
            stmt.setDouble(3, amount);
            stmt.executeUpdate();
        }
    }
}

class ATM {
    private Scanner scanner = new Scanner(System.in);
    private BankOperations bankOperations = new BankOperations();

    void start() {
        System.out.println("Welcome to ATM System!");
        System.out.print("Enter User ID: ");
        String userId = scanner.next();
        System.out.print("Enter PIN: ");
        String pin = scanner.next();

        if (bankOperations.authenticate(userId, pin)) {
            showMenu();
        } else {
            System.out.println("Invalid credentials. Exiting...");
        }
    }

    void showMenu() {
        while (true) {
            System.out.println("\n1. Transactions History\n2. Withdraw\n3. Deposit\n4. Transfer\n5. Check Balance\n6. Quit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1 -> bankOperations.showTransactionHistory();
                case 2 -> bankOperations.withdraw();
                case 3 -> bankOperations.deposit();
                case 4 -> bankOperations.transfer();
                case 5 -> bankOperations.checkBalance();
                case 6 -> {
                    System.out.println("Thank you for using ATM!");
                    System.exit(0);
                }
                default -> System.out.println("Invalid choice! Try again.");
            }
        }
    }

    public static void main(String[] args) {
        new ATM().start();
    }
}
