package repository;

import db.Database;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.crypto.Data;

public class UserRepository {
    public void initializeTable() throws SQLException {
        String sql =
            "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL" + 
            ")";

            try(Connection conn = Database.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
            }
    }

    public void insert(String name) throws SQLException {
        String sql = "INSERT INTO users(name) VALUES(?)";

        try (Connection conn = Database.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
        } 
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, name FROM users ORDER BY id";
        List<User> users = new ArrayList<>();

        try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(new User(rs.getInt("id"), rs.getString("name")));
            }
        } 
        return users;
    }
}