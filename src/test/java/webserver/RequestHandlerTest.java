package webserver;

import db.DataBase;
import model.User;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import util.HttpRequestUtils;

import java.util.HashMap;
import java.util.Map;

public class RequestHandlerTest {
    @Before
    public void join() {
        User user = new User("user", "pass", "name", "x@x.com");
        DataBase.addUser(user);
    }

    @After
    public void clear() {
        DataBase.clear();
    }

    @Test
    public void loginSuccess_success() {
        Map<String, String> params = new HashMap<>();
        params.put("userId", "user");
        params.put("password", "pass");

        boolean isSuccess = RequestHandler.loginSuccess(params);
        Assert.assertTrue(isSuccess);
    }

    @Test
    public void loginSuccess_fail() {
        Map<String, String> params = new HashMap<>();
        params.put("userId", "not_user");
        params.put("password", "pass");

        boolean isSuccess = RequestHandler.loginSuccess(params);
        Assert.assertFalse(isSuccess);
    }
}
