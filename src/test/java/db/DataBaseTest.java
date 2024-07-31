package db;

import model.User;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class DataBaseTest {
    @After
    public void clear() {
        DataBase.clear();
    }

    @Test
    public void addUser() {
        User user = new User("userId", "password", "name", "email@eemail.com");
        DataBase.addUser(user);

        assertThat(DataBase.getUserCount(), is(1));
    }

    @Test
    public void findUserById() {
        String userId = "userId";
        User user = new User(userId, "password", "name", "email@eemail.com");
        DataBase.addUser(user);

        User findUser = DataBase.findUserById(userId);
        assertThat(findUser, is(user));
    }
}
