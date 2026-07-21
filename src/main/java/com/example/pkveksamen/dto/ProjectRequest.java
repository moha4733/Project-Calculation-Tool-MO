package com.example.pkveksamen.dto;

import java.time.LocalDate;

public record ProjectRequest(
        String projectName,
        String projectDescription,
        LocalDate projectStartDate,
        LocalDate projectDeadline,
        String projectCustomer
) {
}
