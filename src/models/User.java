/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import utils.DBConnection;
import javax.swing.JOptionPane;
/**
 *
 * @author ADMIN
 */
public class User {
    private int userId;
    private String username;
    private String password;
    private String role;
    private String fName;
    private String lName;
    private String email;
    
    public User() {
        
    }
    
    public User(String username, String password, String role, String email, String fName, String lName) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.fName = fName;
        this.lName = lName;
    }
    
    public User(int userId, String username, String password, String role, String email, String fName, String lName) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.fName = fName;
        this.lName = lName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getfName() {
        return fName;
    }

    public void setfName(String fName) {
        this.fName = fName;
    }

    public String getlName() {
        return lName;
    }

    public void setlName(String lName) {
        this.lName = lName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean isAdmin() {
        return "Admin".equals(role);
    }
    
    public boolean isEvaluator() {
        return "Evaluator".equals(role);
    }
}
