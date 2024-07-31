package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

import static webserver.RequestParser.*;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            // 입력의 첫 줄이 null 인 경우 HTTP 요청이 없으므로 return
            String line = br.readLine();
            if (line == null) {
                return;
            }

            Map<String, String> header = parseHeader(line);
            String method = header.get("method");
            String url = header.get("url");
            int contentLength = 0;
            Map<String, String> headers = new HashMap<>();
            while (!line.equals("")) {
//                log.info("headers={}", line);
                HttpRequestUtils.Pair pair = HttpRequestUtils.parseHeader(line);
                if (pair != null && pair.getKey().equals("Content-Length")) {
                    contentLength = Integer.parseInt(pair.getValue());
                }

                if (pair != null) {
                    headers.put(pair.getKey(), pair.getValue());
                }
                line = br.readLine();
            }
            // 로그인 여부 판단
            boolean isLogin = false;
            if (headers.get("Cookie") != null) {
                Map<String, String> cookie = HttpRequestUtils.parseCookies(headers.get("Cookie"));
                if (cookie.get("logined").equals("true")) {
                    isLogin = true;
                }
            }
            DataOutputStream dos = new DataOutputStream(out);
            if (url.startsWith("/user/create")) {

                if (method.equals("GET")) {
                    int index = url.indexOf("?");
                    String requestPath = url.substring(0, index);
                    String params = url.substring(index + 1);
                    Map<String, String> queryParams = HttpRequestUtils.parseQueryString(params);
                    User user = new User(queryParams.get("userId"), queryParams.get("password"), queryParams.get("name"), queryParams.get("email"));
                    DataBase.addUser(user);

                    byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                } else if (method.equals("POST")) {
                    String body = IOUtils.readData(br, contentLength);
                    Map<String, String> bodyData = HttpRequestUtils.parseQueryString(body);
                    User user = new User(bodyData.get("userId"), bodyData.get("password"), bodyData.get("name"), bodyData.get("email"));
                    DataBase.addUser(user);

                    response302Header(dos, "/index.html");
                }

            } else if (url.startsWith("/user/login")) {

                if (method.equals("GET")) {
                    byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                    response200Header(dos, body.length);
                    responseBody(dos, body);
                } else if (method.equals("POST")) {
                    // TODO : 로그인
                    String httpBody = IOUtils.readData(br, contentLength);
                    Map<String, String> bodyData = HttpRequestUtils.parseQueryString(httpBody);

                    if (loginSuccess(bodyData)) {
//                        response302Header(dos, "/index.html");
                        response302HeaderWithLoginCookie(dos, "/index.html", true);
//                        response200WithLoginCookie(dos, true);
                    } else {
                        response302HeaderWithLoginCookie(dos, "/user/login_failed.html", false);
                    }
                }
            } else if (headers.get("Accept").startsWith("text/css")) {
                byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                try {
                    dos.writeBytes("HTTP/1.1 200 OK \r\n");
                    dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
                    dos.writeBytes("Content-Length: " + body.length + "\r\n");
                    dos.writeBytes("\r\n");
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
                responseBody(dos, body);
            } else if (url.startsWith("/user/list")) {
                if (method.equals("GET")) {
                    if (isLogin) {
                        Collection<User> users = DataBase.findAll();
                        StringBuilder sb = new StringBuilder();
                        sb.append("<table border='1'>");
                        for (User user : users) {
                            sb.append("<tr>");
                            sb.append("<td>" + user.getUserId() + "</td>");
                            sb.append("<td>" + user.getName() + "</td>");
                            sb.append("<td>" + user.getEmail() + "</td>");
                            sb.append("</tr>");
                        }
                        sb.append("</table>");
                        byte[] body = sb.toString()
                                .getBytes();
//                        byte[] body = Files.readAllBytes(new File("./webapp" + url + ".html").toPath());
                        response200Header(dos, body.length);
                        responseBody(dos, body);
                    } else {
                        byte[] body = Files.readAllBytes(new File("./webapp" + "/user/login.html").toPath());
                        response200Header(dos, body.length);
                        responseBody(dos, body);
                    }
                } else {
                    if (isLogin) {
                        // TODO 사용자 목록 출력
                        byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                        response200Header(dos, body.length);
                        responseBody(dos, body);
                        log.debug("사용자 목록 출력");
                    } else {
                        byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                        response200Header(dos, body.length);
                        responseBody(dos, body);
                    }
                }
            } else {
                byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                response200Header(dos, body.length);
                responseBody(dos, body);
            }

//            byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
//            response200Header(dos, body.length);
//            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String location) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + location + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302HeaderWithLoginCookie(DataOutputStream dos, String location, boolean isLogin) {
        log.info("로그인 쿠키 설정 호출 response302WithLoginCookie {}", true);
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + location + "\r\n");
            dos.writeBytes("Content-Type: text/html; charset=utf-8\r\n");
            dos.writeBytes("Set-Cookie: logined=" + isLogin + "; Domain=localhost; Expires=Session; Path=/; SameSite=None; Secure; HttpOnly; \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public static boolean loginSuccess(Map<String, String> params) {

        String userId = params.get("userId");
        User loginUser = DataBase.findUserById(userId);
        if (loginUser != null && loginUser.getPassword().equals(params.get("password"))) {
            // TODO : 로그인 성공
            log.info("로그인 한 사람 {}", DataBase.findUserById(userId).getEmail());
            return true;
        } else {
            // TODO : 로그인 실패
            return false;
        }
    }
}
