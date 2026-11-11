package com.bookmuse.app.controller;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
@RestController @RequestMapping("/api/notifications") @RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;
    @Operation(summary="List my notifications") @GetMapping
    public Responses.PageView<Responses.NotificationView> list(@ParameterObject Pageable p) { return service.list(p); }
    @Operation(summary="Get my unread notification count") @GetMapping("/unread-count")
    public Responses.Count unread() { return service.unread(); }
    @Operation(summary="Mark all my notifications as read") @PutMapping("/read-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll() { service.readAll(); }
    @Operation(summary="OWNER: mark a notification as read") @PutMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read(@PathVariable UUID id) { service.read(id); }
}
