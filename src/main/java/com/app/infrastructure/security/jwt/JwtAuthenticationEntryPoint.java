package com.app.infrastructure.security.jwt;

import com.app.common.enums.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a 401 in the application's standard {@code Response} JSON shape when an
 * unauthenticated request hits a protected endpoint (no/invalid/expired/blacklisted token).
 *
 * <p>The JSON is built by hand to stay independent of the active Jackson version
 * (the module mixes Jackson 2 and Jackson 3 mappers).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String message = "Authentication is required to access this resource.";
        String body = "{"
                + "\"status\":" + HttpStatus.UNAUTHORIZED.value() + ","
                + "\"code\":\"" + ResponseCode.ACCESS_DENIED.name() + "\","
                + "\"errors\":[\"" + message + "\"],"
                + "\"path\":\"" + escape(request.getRequestURI()) + "\""
                + "}";

        response.getWriter().write(body);
        response.getWriter().flush();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
