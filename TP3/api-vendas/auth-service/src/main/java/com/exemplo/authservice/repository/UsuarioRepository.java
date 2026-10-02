package com.exemplo.authservice.repository;

import com.exemplo.authservice.model.Usuario;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Repositorio Spring Data JDBC (sem JPA/Hibernate). */
@Repository
public interface UsuarioRepository extends ListCrudRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);
}
