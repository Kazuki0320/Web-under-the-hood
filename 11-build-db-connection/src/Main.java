import controller.UserController;
import repository.UserRepository;
import service.UserService;

public class Main {
    public static void main(String[] args) {
        try {
            UserRepository userRepository = new UserRepository();
            UserService userService = new UserService(userRepository);
            UserController userController = new UserController(userService);

            userController.runDemo();
        } catch (Exception e) {
            System.err.println("Failed to run app: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
