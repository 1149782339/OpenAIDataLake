package com.openaidatalake.console.web;

import com.openaidatalake.console.service.PathService;
import com.openaidatalake.console.service.PathService.GrantView;
import com.openaidatalake.console.service.PathService.PathView;
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
@RequestMapping("/api/paths")
public class PathController {

    private final PathService paths;

    public PathController(PathService paths) {
        this.paths = paths;
    }

    @GetMapping
    public List<PathView> list() {
        return paths.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PathView create(@Valid @RequestBody CreatePathRequest request) {
        return paths.create(request.bucket(), request.keyPrefix(), request.description());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        paths.delete(id);
    }

    @PutMapping("/{id}/route")
    public PathView.RouteView upsertRoute(@PathVariable long id, @Valid @RequestBody RouteRequest request) {
        return paths.upsertRoute(id, request.backendId(), request.physicalBucket(), request.physicalPrefix());
    }

    @DeleteMapping("/{id}/route")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoute(@PathVariable long id) {
        paths.deleteRoute(id);
    }

    @GetMapping("/{id}/grants")
    public List<GrantView> grants(@PathVariable long id) {
        return paths.listGrants(id);
    }

    @PutMapping("/{id}/grants")
    public GrantView upsertGrant(@PathVariable long id, @Valid @RequestBody GrantRequest request) {
        return paths.upsertGrant(id, request.userId(), request.canList(), request.canRead(), request.canWrite(), request.canDelete());
    }

    @DeleteMapping("/{id}/grants/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGrant(@PathVariable long id, @PathVariable long userId) {
        paths.deleteGrant(id, userId);
    }

    public record CreatePathRequest(@NotBlank String bucket, String keyPrefix, String description) {
    }

    public record RouteRequest(@NotNull Long backendId, @NotBlank String physicalBucket, String physicalPrefix) {
    }

    public record GrantRequest(long userId, boolean canList, boolean canRead, boolean canWrite, boolean canDelete) {
    }
}
