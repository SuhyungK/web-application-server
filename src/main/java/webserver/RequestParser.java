package webserver;

import java.util.Random;

public class RequestParser {
    public static String[] parse(String target) {
        return target.split(" ");
    }

    public static String getUrl(String[] line, int index) {
        return line[index];
    }
}
