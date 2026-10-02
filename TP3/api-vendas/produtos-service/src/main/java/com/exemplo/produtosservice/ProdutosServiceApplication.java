package com.exemplo.produtosservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ProdutosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProdutosServiceApplication.class, args);
    }

}

// CRIAR UM MICROSERVICE DE VENDAS, AO QUAL DEVERÄ SE COMUNICAR COM O MICROSERVICE
// DE PRODUTOS, CAPTURANDO AS INFORMACOES DE PRODUTOS, PARA UTILIZAR NO REGISTRO
// DE VENDAS. O MICROSERVICE DE VENDAS DEVERA TER UM ENDPOINT PARA REGISTRAR UMA VENDA,


// ID_VENDA     |       ID_PRODUTO      |       QUANTIDADE       |      VALOR_PRODUTO   