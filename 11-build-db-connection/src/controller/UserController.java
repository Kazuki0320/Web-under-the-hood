package controller;

import model.User;
import service.UserService;

import java.sql.SQLException;
import java.util.List;

public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void runDemo() throws SQLException {
        userService.initialize();
        userService.registerUser("Alice");
        userService.registerUser("Bob");

        List<User> users = userService.getAllUsers();
        for (User user : users) {
            System.out.println(user.getId() + ": " + user.getName());
        }
    }
}
