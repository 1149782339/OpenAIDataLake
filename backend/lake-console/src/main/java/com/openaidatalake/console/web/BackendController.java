package com.openaidatalake.console.web;

import com.openaidatalake.console.service.BackendService;
import com.openaidatalake.console.service.BackendService.BackendCommand;
import com.openaidatalake.console.service.BackendService.BackendView;
import com.openaidatalake.domain.storage.StorageProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/backends")
public class BackendController {

    private final BackendService backends;

    public BackendController(BackendService backends) {
        this.backends = backends;
    }

    @GetMapping
    public List<BackendView> list() {
        return backends.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BackendView create(@Valid @RequestBody BackendRequest request) {
        return backends.create(request.toCommand());
    }

    @PutMapping("/{id}")
    public BackendView update(@PathVariable long id, @Valid @RequestBody BackendRequest request) {
        return backends.update(id, request.toCommand());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        backends.delete(id);
    }

    public record BackendRequest(
            @NotBlank String name,
            @NotNull StorageProvider provider,
            @NotBlank String endpoint,
            @NotBlank String region,
            Boolean pathStyle,
            @NotBlank String accessKey,
            String secretKey,
            Boolean enabled
    ) {
        BackendCommand toCommand() {
            return new BackendCommand(name, provider, endpoint, region, pathStyle, accessKey, secretKey, enabled);
        }
    }
}
