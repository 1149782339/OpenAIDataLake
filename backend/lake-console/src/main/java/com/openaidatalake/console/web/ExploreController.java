package com.openaidatalake.console.web;

import com.openaidatalake.console.service.DataSourceService;
import com.openaidatalake.console.service.DataSourceService.QueryView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/explore")
public class ExploreController {

    private final DataSourceService sources;

    public ExploreController(DataSourceService sources) {
        this.sources = sources;
    }

    @PostMapping("/query")
    public QueryView query(@Valid @RequestBody QueryRequest request, Authentication authentication) {
        return sources.query(request.sql(), authentication.getName());
    }

    public record QueryRequest(@NotBlank(message = "请输入要执行的 SQL") String sql) {
    }
}
