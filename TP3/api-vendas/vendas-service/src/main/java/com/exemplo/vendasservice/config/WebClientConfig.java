package com.exemplo.vendasservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.web.reactive.function.client.ServerBearerExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * WebClient (HTTP reativo, nao bloqueante) para o produtos-service:
     * <ul>
     *   <li>{@link ReactorLoadBalancerExchangeFilterFunction}: resolve o nome logico
     *       "produtos-service" para uma instancia registrada no Eureka (Spring Cloud LoadBalancer);</li>
     *   <li>{@link ServerBearerExchangeFilterFunction}: repassa o token JWT da requisicao
     *       recebida no header Authorization da chamada ao produtos-service (rota protegida).</li>
     * </ul>
     */
    @Bean
    public WebClient produtosWebClient(WebClient.Builder builder,
                                       ReactorLoadBalancerExchangeFilterFunction loadBalancer,
                                       @Value("${produtos-service.url}") String produtosUrl) {
        return builder
                .baseUrl(produtosUrl)
                .filter(loadBalancer)
                .filter(new ServerBearerExchangeFilterFunction())
                .build();
    }
}
