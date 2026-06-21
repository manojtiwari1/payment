package com.app.modules.role.controller;

import com.app.common.response.BaseResponse;
import com.app.common.response.Response;
import com.app.modules.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth/roles")
public class RoleController extends BaseResponse {

    private final RoleService roleService;

    @GetMapping("")
    ResponseEntity<Response> getAllRoles(){
        log.debug("Get All Roles Controller");
        return data(roleService.getAllActiveRoles());
    }

}
