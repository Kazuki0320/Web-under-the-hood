package chapter13.repository;

import chapter13.model.User;

public class UserRepository {
    // userRepositoryの責務を決める
    // 「id/passwordを受け取り、ユーザーがいれば返す。いなければ、nullを返す」
    public User findByCredentials(String id, String password) {
        if (id == null && password == null) {
            return null;
        }
        if ("demo".equals(id) && "password123".equals(password)) {
            return new User("1", "Demo User");
        }
        return null;
    }
} 
