package com.exemplo.fornecedoresservice.config;

import com.exemplo.fornecedoresservice.model.Fornecedor;
import com.exemplo.fornecedoresservice.repository.FornecedorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Popula o banco H2 em memoria com fornecedores de teste assim que a aplicacao sobe.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final FornecedorRepository fornecedorRepository;

    public DataInitializer(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    @Override
    public void run(String... args) {
        fornecedorRepository.save(new Fornecedor("Alfa Insumos Industriais", "12.345.678/0001-90"));
        fornecedorRepository.save(new Fornecedor("Beta Distribuidora Ltda", "23.456.789/0001-01"));
        fornecedorRepository.save(new Fornecedor("Gamma Componentes Eletronicos", "34.567.890/0001-12"));
        fornecedorRepository.save(new Fornecedor("Delta Embalagens S/A", "45.678.901/0001-23"));
        fornecedorRepository.save(new Fornecedor("Epsilon Logistica e Transportes", "56.789.012/0001-34"));
    }
}
