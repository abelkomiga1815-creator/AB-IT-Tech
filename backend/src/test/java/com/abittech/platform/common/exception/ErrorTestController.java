package com.abittech.platform.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Test-only endpoints that raise each kind of error. It carries no stereotype annotation, so it is
 * never picked up by component scanning; tests register it explicitly with {@code @Import}.
 */
@RequestMapping("/test-errors")
@ResponseBody
public class ErrorTestController {

    public record SampleRequest(@NotBlank String name, @Min(1) int age) {
    }

    @GetMapping("/not-found")
    public String notFound() {
        throw new ResourceNotFoundException();
    }

    @GetMapping("/bad-request")
    public String badRequest() {
        throw new BadRequestException("The filter combination is not supported");
    }

    @GetMapping("/unexpected")
    public String unexpected() {
        throw new IllegalStateException("jdbc:postgresql://internal-host/db password=hunter2");
    }

    @GetMapping("/number/{value}")
    public int number(@PathVariable int value) {
        return value;
    }

    @PostMapping("/validate")
    public String validate(@Valid @RequestBody SampleRequest request) {
        return request.name();
    }
}
