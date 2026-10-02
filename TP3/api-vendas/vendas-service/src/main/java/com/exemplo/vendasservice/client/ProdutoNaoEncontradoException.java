package com.exemplo.vendasservice.client;

public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(Long idProduto) {
        super("Produto " + idProduto + " nao encontrado no produtos-service");
    }
}
