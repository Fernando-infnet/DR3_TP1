package com.exemplo.vendasservice.client;

import com.exemplo.vendasservice.dto.ProdutoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Cliente reativo do produtos-service. Toda a chamada e nao bloqueante: o resultado
 * e um {@link Mono} que so e executado quando alguem se inscreve (subscribe).
 */
@Component
public class ProdutoClient {

    private final WebClient produtosWebClient;
    private final Duration timeout;

    public ProdutoClient(WebClient produtosWebClient,
                         @Value("${produtos-service.timeout:3s}") Duration timeout) {
        this.produtosWebClient = produtosWebClient;
        this.timeout = timeout;
    }

    public Mono<ProdutoResponse> buscarProduto(Long idProduto) {
        return produtosWebClient.get()
                .uri("/produtos/{id}", idProduto)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        resposta -> Mono.error(new ProdutoNaoEncontradoException(idProduto)))
                .bodyToMono(ProdutoResponse.class)
                .timeout(timeout)
                // Qualquer outra falha (timeout, conexao recusada, 5xx...) vira "servico indisponivel"
                .onErrorMap(e -> !(e instanceof ProdutoNaoEncontradoException),
                        ProdutosServiceIndisponivelException::new);
    }
}
