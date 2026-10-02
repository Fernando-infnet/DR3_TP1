package com.exemplo.vendasservice.client;

public class ProdutosServiceIndisponivelException extends RuntimeException {

    public ProdutosServiceIndisponivelException(Throwable causa) {
        super("produtos-service indisponivel no momento, tente novamente mais tarde", causa);
    }
}
