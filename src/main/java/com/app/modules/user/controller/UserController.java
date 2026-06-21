package com.app.modules.user.controller;

import com.app.common.response.BaseResponse;
import com.app.common.response.Response;
import com.app.modules.user.request.UserRequestDTO;
import com.app.modules.user.service.UserService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/users")
//@Tag(name = "User Management", description = "Create and list users")
public class UserController extends BaseResponse {

    private final UserService userService;

    @PostMapping
//    @Operation(summary = "Create / invite a new user")
    public ResponseEntity<Response> createUser(@Valid @RequestBody UserRequestDTO request) {
        return data(userService.createUser(request));
    }

    @GetMapping
//    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<Response> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return data(userService.getAllUsers(PageRequest.of(page, size, sort)));
    }
}
