package com.openaidatalake.console.web;

import com.openaidatalake.console.service.AccessKeyService;
import com.openaidatalake.console.service.AccessKeyService.AccessKeyView;
import com.openaidatalake.console.service.AccessKeyService.CreatedAccessKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/access-keys")
public class AccessKeyController {

    private final AccessKeyService keys;

    public AccessKeyController(AccessKeyService keys) {
        this.keys = keys;
    }

    @GetMapping
    public List<AccessKeyView> list(@RequestParam(required = false) Long userId) {
        return keys.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedAccessKey create(@Valid @RequestBody CreateAccessKeyRequest request) {
        return keys.create(request.userId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        keys.delete(id);
    }

    public record CreateAccessKeyRequest(@NotNull Long userId) {
    }
}
