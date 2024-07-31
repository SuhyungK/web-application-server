package webserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RequestParser {
    public static String[] parse(String target) {
        return target.split(" ");
    }

    public static String getUrl(String[] line, int index) {
        return line[index];
    }

    public static Map<String, String> parseHeader(String httpHeader) {
        String[] header = httpHeader.split(" ");
        Map<String, String> result = new HashMap<>();
        result.put("method", header[0]);
        result.put("url", header[1]);
        return result;
    }
}
