package com.javaprojects;

import java.sql.*;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
import java.util.regex.Pattern;

public class LibraryManagementSystem {

    static final String URL =
            "jdbc:mysql://localhost:3306/library_management_db";

    static final String USER = "root";
    static final String PASSWORD = "7200447281@Ss";

    static Connection con;
    static Scanner sc = new Scanner(System.in);

    static final int FINE_PER_DAY = 10;

    public static void main(String[] args) {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            con = DriverManager.getConnection(URL, USER, PASSWORD);

            int choice;

            do {
                System.out.println("\n=== LIBRARY MANAGEMENT SYSTEM ===");
                System.out.println("1. Add book");
                System.out.println("2. View/search books");
                System.out.println("3. Register member");
                System.out.println("4. Issue book");
                System.out.println("5. Return book");
                System.out.println("6. View issue records");
                System.out.println("7. Update book");
                System.out.println("8. Exit");

                choice = readInt("Enter choice: ");

                switch (choice) {
                    case 1:
                        addBook();
                        break;

                    case 2:
                        searchBooks();
                        break;

                    case 3:
                        addMember();
                        break;

                    case 4:
                        issueBook();
                        break;

                    case 5:
                        returnBook();
                        break;

                    case 6:
                        viewIssues();
                        break;

                    case 7:
                        updateBook();
                        break;

                    case 8:
                        System.out.println("Goodbye!");
                        break;

                    default:
                        System.out.println("Invalid choice. Enter 1 to 8.");
                }

            } while (choice != 8);

        } catch (ClassNotFoundException e) {
            System.out.println(
                    "MySQL JDBC driver not found. Add Connector/J to the project.");

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());

        } finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Could not close database connection.");
            }

            sc.close();
        }
    }

    // Read an integer and reject invalid input
    static int readInt(String message) {

        while (true) {
            System.out.print(message);
            String value = sc.nextLine().trim();

            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    // Reject empty input
    static String readRequired(String message) {

        while (true) {
            System.out.print(message);
            String value = sc.nextLine().trim();

            if (!value.isEmpty()) {
                return value;
            }

            System.out.println("This field cannot be empty.");
        }
    }

    // Validate email format
    static String readEmail() {

        while (true) {
            String email = readRequired("Enter email: ");

            if (Pattern.matches(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                    email)) {
                return email;
            }

            System.out.println(
                    "Invalid email format. Example: name@gmail.com");
        }
    }

    // Check whether an ID exists in the specified table
    static boolean exists(String table, int id) throws SQLException {

        String sql = "SELECT id FROM " + table + " WHERE id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Add a new book
    static void addBook() throws SQLException {

        String title = readRequired("Enter book title: ");
        String author = readRequired("Enter author name: ");

        String sql = "INSERT INTO books(title, author) VALUES (?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, author);

            ps.executeUpdate();

            System.out.println("Book added successfully.");
        }
    }

    // View all books or search by title/author
    static void searchBooks() throws SQLException {

        String keyword = readRequired(
                "Enter title or author to search (or type * for all): ");

        String sql;
        PreparedStatement ps;

        if (keyword.equals("*")) {
            sql = "SELECT id, title, author, available "
                    + "FROM books ORDER BY id";

            ps = con.prepareStatement(sql);

        } else {
            sql = "SELECT id, title, author, available "
                    + "FROM books WHERE title LIKE ? OR author LIKE ? "
                    + "ORDER BY id";

            ps = con.prepareStatement(sql);
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
        }

        try (PreparedStatement statement = ps;
             ResultSet rs = statement.executeQuery()) {

            boolean found = false;

            while (rs.next()) {
                found = true;

                System.out.println(
                        rs.getInt("id") + " | "
                        + rs.getString("title") + " | "
                        + rs.getString("author") + " | "
                        + (rs.getBoolean("available")
                                ? "Available" : "Issued"));
            }

            if (!found) {
                System.out.println("No books found.");
            }
        }
    }

    // Update an existing book's title and author
    static void updateBook() throws SQLException {

        int bookId = readInt("Enter book ID to update: ");

        if (bookId <= 0 || !exists("books", bookId)) {
            System.out.println("Book ID not found.");
            return;
        }

        String title = readRequired("Enter new book title: ");
        String author = readRequired("Enter new author name: ");

        String sql = "UPDATE books SET title = ?, author = ? WHERE id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setInt(3, bookId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Book updated successfully.");
            } else {
                System.out.println("Book was not updated.");
            }
        }
    }

    // Register a library member
    static void addMember() throws SQLException {

        String name = readRequired("Enter member name: ");
        String email = readEmail();

        String sql = "INSERT INTO members(name, email) VALUES (?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);

            ps.executeUpdate();

            System.out.println("Member registered successfully.");

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("That email is already registered.");
        }
    }

    // Issue a book using a transaction
    static void issueBook() throws SQLException {

        int bookId = readInt("Enter book ID: ");

        if (bookId <= 0 || !exists("books", bookId)) {
            System.out.println("Book ID not found.");
            return;
        }

        int memberId = readInt("Enter member ID: ");

        if (memberId <= 0 || !exists("members", memberId)) {
            System.out.println("Member ID not found.");
            return;
        }

        con.setAutoCommit(false);

        try {
            String checkSql =
                    "SELECT available FROM books WHERE id = ? FOR UPDATE";

            boolean available;

            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setInt(1, bookId);

                try (ResultSet rs = ps.executeQuery()) {
                    available = rs.next() && rs.getBoolean("available");
                }
            }

            if (!available) {
                con.rollback();
                System.out.println("This book is already issued.");
                return;
            }

            String issueSql =
                    "INSERT INTO book_issues "
                    + "(book_id, member_id, issue_date, due_date) "
                    + "VALUES (?, ?, CURDATE(), "
                    + "DATE_ADD(CURDATE(), INTERVAL 14 DAY))";

            try (PreparedStatement ps = con.prepareStatement(issueSql)) {
                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE books SET available = FALSE WHERE id = ?")) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }

            con.commit();

            System.out.println(
                    "Book issued. Please return it within 14 days.");

        } catch (SQLException e) {
            con.rollback();

            System.out.println(
                    "Issue failed. Changes were cancelled: " + e.getMessage());

        } finally {
            con.setAutoCommit(true);
        }
    }

    // Return a book and calculate the late fine
    static void returnBook() throws SQLException {

        int issueId = readInt("Enter issue record ID: ");

        if (issueId <= 0) {
            System.out.println("Issue ID must be a positive number.");
            return;
        }

        String findSql =
                "SELECT book_id, due_date, return_date "
                + "FROM book_issues WHERE id = ?";

        int bookId;
        Date dueDate;

        try (PreparedStatement ps = con.prepareStatement(findSql)) {
            ps.setInt(1, issueId);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("Issue record not found.");
                    return;
                }

                if (rs.getDate("return_date") != null) {
                    System.out.println("This book has already been returned.");
                    return;
                }

                bookId = rs.getInt("book_id");
                dueDate = rs.getDate("due_date");
            }
        }

        con.setAutoCommit(false);

        try {
            String updateIssue =
                    "UPDATE book_issues SET return_date = CURDATE() "
                    + "WHERE id = ? AND return_date IS NULL";

            try (PreparedStatement ps = con.prepareStatement(updateIssue)) {
                ps.setInt(1, issueId);

                if (ps.executeUpdate() == 0) {
                    con.rollback();

                    System.out.println(
                            "This issue record was already returned.");
                    return;
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE books SET available = TRUE WHERE id = ?")) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }

            con.commit();

            long lateDays = Math.max(
                    0,
                    ChronoUnit.DAYS.between(
                            dueDate.toLocalDate(),
                            java.time.LocalDate.now()));

            long fine = lateDays * FINE_PER_DAY;

            System.out.println("Book returned successfully.");
            System.out.println(
                    "Late days: " + lateDays + " | Fine: Rs. " + fine);

        } catch (SQLException e) {
            con.rollback();

            System.out.println(
                    "Return failed. Changes were cancelled: " + e.getMessage());

        } finally {
            con.setAutoCommit(true);
        }
    }

    // Display issue records using JOIN
    static void viewIssues() throws SQLException {

        String sql =
                "SELECT bi.id, b.title, m.name, "
                + "bi.issue_date, bi.due_date, bi.return_date "
                + "FROM book_issues bi "
                + "JOIN books b ON bi.book_id = b.id "
                + "JOIN members m ON bi.member_id = m.id "
                + "ORDER BY bi.id";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            boolean found = false;

            while (rs.next()) {
                found = true;

                System.out.println(
                        "Issue ID: " + rs.getInt("id")
                        + " | Book: " + rs.getString("title")
                        + " | Member: " + rs.getString("name")
                        + " | Issued: " + rs.getDate("issue_date")
                        + " | Due: " + rs.getDate("due_date")
                        + " | Returned: " + rs.getDate("return_date"));
            }

            if (!found) {
                System.out.println("No issue records found.");
            }
        }
    }
}
