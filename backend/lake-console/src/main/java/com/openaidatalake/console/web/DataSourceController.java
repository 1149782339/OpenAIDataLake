package com.openaidatalake.console.web;

import com.openaidatalake.console.service.DataSourceService;
import com.openaidatalake.console.service.DataSourceService.DataSourceView;
import com.openaidatalake.console.service.DataSourceService.GrantInput;
import com.openaidatalake.console.service.DataSourceService.GrantView;
import com.openaidatalake.console.service.DataSourceService.SourceCatalog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data-sources")
public class DataSourceController {

    private final DataSourceService sources;

    public DataSourceController(DataSourceService sources) {
        this.sources = sources;
    }

    @GetMapping
    public List<DataSourceView> list() {
        return sources.list();
    }

    @GetMapping("/catalog")
    public List<SourceCatalog> catalog(Authentication authentication) {
        return sources.catalog(authentication.getName());
    }

    @GetMapping("/{id}/grants")
    public List<GrantView> grants(@PathVariable long id) {
        return sources.listGrants(id);
    }

    @PutMapping("/{id}/grants")
    public List<GrantView> replaceGrants(@PathVariable long id, @Valid @RequestBody GrantUpdateRequest request) {
        return sources.replaceGrants(id, inputs(request.grants()));
    }

    @PostMapping("/probe")
    public Map<String, Boolean> probe(@Valid @RequestBody ConnectionRequest request) {
        sources.probe(request.engine(), request.host(), request.port(), request.user(), request.password(), request.database());
        return Map.of("ok", true);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataSourceView create(@Valid @RequestBody CreateSourceRequest request) {
        return sources.create(
                request.name(),
                request.engine(),
                request.host(),
                request.port(),
                request.user(),
                request.password(),
                request.database(),
                inputs(request.grants()));
    }

    private static List<GrantInput> inputs(List<GrantRequest> grants) {
        if (grants == null || grants.isEmpty()) {
            return List.of();
        }
        return grants.stream().map(GrantRequest::toInput).toList();
    }

    public record ConnectionRequest(
            @NotBlank(message = "请填写数据源类型") String engine,
            @NotBlank(message = "请填写 Host") String host,
            @NotNull(message = "请填写 Port") @Min(value = 1, message = "端口不正确") @Max(value = 65535, message = "端口不正确") Integer port,
            @NotBlank(message = "请填写用户") String user,
            @NotBlank(message = "请填写密码") String password,
            @NotBlank(message = "请填写数据库名称") String database
    ) {
    }

    public record CreateSourceRequest(
            @NotBlank(message = "请填写数据源名称") String name,
            @NotBlank(message = "请填写数据源类型") String engine,
            @NotBlank(message = "请填写 Host") String host,
            @NotNull(message = "请填写 Port") @Min(value = 1, message = "端口不正确") @Max(value = 65535, message = "端口不正确") Integer port,
            @NotBlank(message = "请填写用户") String user,
            @NotBlank(message = "请填写密码") String password,
            @NotBlank(message = "请填写数据库名称") String database,
            List<@Valid GrantRequest> grants
    ) {
    }

    public record GrantRequest(
            @NotNull(message = "请选择用户") @Positive(message = "请选择用户") Long userId,
            boolean writable
    ) {
        public GrantInput toInput() {
            return new GrantInput(userId, writable);
        }
    }

    public record GrantUpdateRequest(List<@Valid GrantRequest> grants) {
    }
}
