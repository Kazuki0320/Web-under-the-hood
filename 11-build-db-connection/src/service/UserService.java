package service;

import java.sql.SQLException;
import java.util.List;

import model.User;
import repository.equals;

public class UserService {
    private final equals userRepository;

    public UserService(equals userRepository) {
        this.userRepository = userRepository;
    }

    public void initialize() throws SQLException {
        userRepository.initializeTable();
    }

    public void registerUser(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is requred");
        }
        userRepository.insert(name.trim());
    }

    public List<User> getAllUsers() throws SQLException {
        return userRepository.findAll();
    }
}
