package com.openaidatalake.console.web;

import com.openaidatalake.console.service.DirectoryService;
import com.openaidatalake.console.service.DirectoryService.DirectoryItem;
import com.openaidatalake.console.service.DirectoryService.GrantInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/directories")
public class DirectoryController {

    private final DirectoryService directories;

    public DirectoryController(DirectoryService directories) {
        this.directories = directories;
    }

    @GetMapping
    public List<DirectoryItem> list() {
        return directories.list();
    }

    @PostMapping("/probe")
    public ProbeResponse probe(@Valid @RequestBody ProbeRequest request) {
        return new ProbeResponse(directories.probe(request.endpoint(), request.accessKey(), request.secretKey()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DirectoryItem create(@Valid @RequestBody CreateDirectoryRequest request) {
        return directories.create(
                request.logicalName(),
                request.endpoint(),
                request.accessKey(),
                request.secretKey(),
                request.directory(),
                request.grants());
    }

    public record ProbeRequest(@NotBlank String endpoint, @NotBlank String accessKey, @NotBlank String secretKey) {
    }

    public record ProbeResponse(List<String> directories) {
    }

    public record CreateDirectoryRequest(
            @NotBlank String logicalName,
            @NotBlank String endpoint,
            @NotBlank String accessKey,
            @NotBlank String secretKey,
            @NotBlank String directory,
            List<GrantInput> grants
    ) {
    }
}
