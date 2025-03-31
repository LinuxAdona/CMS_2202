/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services;

import utils.PassUtil;
import utils.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.swing.JOptionPane;
import models.User;

public class UserService {

    public void registerUser(String username, String password, String role, String fName, String lName, String email) {
        String hashedPassword = PassUtil.hashPassword(password);

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO users (username, password, hashed_password, role, first_name, last_name, email) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.setString(3, hashedPassword);
                pstmt.setString(4, role);
                pstmt.setString(5, fName);
                pstmt.setString(6, lName);
                pstmt.setString(7, email);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null, "Registration Successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public int authenticateUser(String user, String password) {
        String hashedPassword = null;

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT hashed_password FROM users WHERE username = ? OR email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, user);
                pstmt.setString(2, user);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        hashedPassword = rs.getString("hashed_password");
                    } else {
                        return -1;
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database Error: " + e.getMessage(), "Authentication Error", JOptionPane.ERROR_MESSAGE);
        }

        return hashedPassword != null && PassUtil.checkPassword(password, hashedPassword) ? 1 : 0;
    }
    
    public User getUserDetails(String user) {
        if (user == null) {
            return null;
        }

        User userDetails = new User();
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM users WHERE username = ? OR email = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user);
                ps.setString(2, user);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        userDetails.setUserId(rs.getInt("user_id"));
                        userDetails.setUsername(rs.getString("username"));
                        userDetails.setEmail(rs.getString("email"));
                        userDetails.setRole(rs.getString("role"));
                        userDetails.setfName(rs.getString("first_name"));
                        userDetails.setlName(rs.getString("last_name"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database Error: " + e.getMessage());
        }

        return userDetails;
    }
}
