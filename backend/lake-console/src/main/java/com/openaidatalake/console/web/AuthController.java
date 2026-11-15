package com.openaidatalake.console.web;

import com.openaidatalake.domain.user.UserAccount;
import com.openaidatalake.domain.user.UserAccountRepository;
import com.openaidatalake.domain.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserAccountRepository users;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          UserAccountRepository users) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.users = users;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CSRF 令牌不可用");
        }
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            request.getSession(true);
            request.changeSessionId();
            securityContextRepository.saveContext(context, request, response);
            return UserResponse.from(requireUser(authentication.getName()));
        } catch (AuthenticationException ex) {
            throw ApiException.unauthorized("用户名或密码错误");
        }
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return UserResponse.from(requireUser(authentication.getName()));
    }

    private UserAccount requireUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> ApiException.unauthorized("未登录"));
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {
    }

    public record UserResponse(long id, String username, UserRole role) {
        static UserResponse from(UserAccount user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getRole());
        }
    }
}
