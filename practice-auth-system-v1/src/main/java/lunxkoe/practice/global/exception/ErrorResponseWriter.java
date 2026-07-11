package lunxkoe.practice.global.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

public final class ErrorResponseWriter {

    private ErrorResponseWriter() {}

    public static void write(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode errorCode) throws IOException {
        write(response, objectMapper, errorCode.getStatus().value(), ErrorResponse.of(errorCode));
    }

    public static void write(HttpServletResponse response, ObjectMapper objectMapper, CustomException e) throws IOException {
        write(response, objectMapper, e.getErrorCode().getStatus().value(), ErrorResponse.of(e));
    }

    private static void write(HttpServletResponse response, ObjectMapper objectMapper, int status, ErrorResponse body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
