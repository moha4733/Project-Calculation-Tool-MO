package com.example.pkveksamen.dto;

import com.example.pkveksamen.model.AlphaRole;
import com.example.pkveksamen.model.EmployeeRole;

public record RegisterEmployeeRequest(
        String username,
        String password,
        String email,
        EmployeeRole role,
        AlphaRole alphaRole
) {
}
