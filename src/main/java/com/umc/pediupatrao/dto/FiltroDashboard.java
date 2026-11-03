package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.StatusPedido;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record FiltroDashboard(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
        StatusPedido status,
        String produtoId,
        String tipo) {
}
