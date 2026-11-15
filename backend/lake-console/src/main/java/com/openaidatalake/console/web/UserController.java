package com.openaidatalake.console.web;

import com.openaidatalake.console.service.UserAdminService;
import com.openaidatalake.console.service.UserAdminService.UserView;
import com.openaidatalake.domain.user.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAdminService users;

    public UserController(UserAdminService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserView> list() {
        return users.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@Valid @RequestBody CreateUserRequest request) {
        return users.create(request.username(), request.password(), request.role());
    }

    @PutMapping("/{id}")
    public UserView update(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request, Authentication authentication) {
        return users.update(id, request.role(), request.enabled(), authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, Authentication authentication) {
        users.delete(id, authentication.getName());
    }

    public record CreateUserRequest(@NotBlank String username, @NotBlank String password, UserRole role) {
    }

    public record UpdateUserRequest(UserRole role, boolean enabled) {
    }
}
