package com.exemplo.authservice.dto;

import java.time.Instant;

public record ErroResponse(int status, String erro, String mensagem, Instant timestamp) {

    public ErroResponse(int status, String erro, String mensagem) {
        this(status, erro, mensagem, Instant.now());
    }
}
