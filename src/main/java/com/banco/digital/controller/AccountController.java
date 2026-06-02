package com.banco.digital.controller;

import com.banco.digital.dto.AccountRequest;
import com.banco.digital.dto.AccountResponse;
import com.banco.digital.dto.MovementResponse;
import com.banco.digital.dto.NotificationResponse;
import com.banco.digital.service.AccountService;
import com.banco.digital.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Contas")
public class AccountController {

    private final AccountService accountService;
    private final NotificationService notificationService;

    public AccountController(AccountService accountService, NotificationService notificationService) {
        this.accountService = accountService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<AccountResponse> listAll() {
        return accountService.listAll();
    }

    @GetMapping("/{id}")
    public AccountResponse findById(@PathVariable Long id) {
        return accountService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return accountService.create(request);
    }

    @GetMapping("/{id}/movements")
    public List<MovementResponse> getMovements(@PathVariable Long id) {
        return accountService.getMovements(id);
    }

    @GetMapping("/{id}/notifications")
    public List<NotificationResponse> getNotifications(@PathVariable Long id) {
        accountService.findById(id);
        return notificationService.listByAccount(id);
    }
}
