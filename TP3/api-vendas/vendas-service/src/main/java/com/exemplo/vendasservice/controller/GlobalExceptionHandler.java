package com.exemplo.vendasservice.controller;

import com.exemplo.vendasservice.client.ProdutoNaoEncontradoException;
import com.exemplo.vendasservice.client.ProdutosServiceIndisponivelException;
import com.exemplo.vendasservice.dto.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** O produto informado nao existe no catalogo: a venda nao pode ser registrada. */
    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse produtoNaoEncontrado(ProdutoNaoEncontradoException e) {
        return new ErroResponse(422, "Unprocessable Entity", e.getMessage());
    }

    /** produtos-service fora do ar, lento (timeout) ou com erro interno. */
    @ExceptionHandler(ProdutosServiceIndisponivelException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErroResponse produtosIndisponivel(ProdutosServiceIndisponivelException e) {
        return new ErroResponse(503, "Service Unavailable", e.getMessage());
    }

    @ExceptionHandler({WebExchangeBindException.class, ServerWebInputException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResponse requisicaoInvalida(Exception e) {
        return new ErroResponse(400, "Bad Request",
                "Corpo da requisicao invalido: informe idProduto e quantidade (>= 1)");
    }
}
