package com.exemplo.authservice.repository;

import com.exemplo.authservice.TestcontainersConfiguration;
import com.exemplo.authservice.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.postgresql.util.PSQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa a camada de persistencia (Spring Data JDBC) contra um PostgreSQL real (Testcontainers).
 */
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void limpar() {
        usuarioRepository.deleteAll();
    }

    @Test
    void deveSalvarUsuarioEGerarId() {
        Usuario salvo = usuarioRepository.save(new Usuario("maria", "hash", "USER"));

        assertThat(salvo.getId()).isNotNull();
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void deveBuscarPorUsername() {
        usuarioRepository.save(new Usuario("joao", "hash", "USER,ADMIN"));

        assertThat(usuarioRepository.findByUsername("joao"))
                .hasValueSatisfying(u -> {
                    assertThat(u.getPassword()).isEqualTo("hash");
                    assertThat(u.getRolesList()).containsExactly("USER", "ADMIN");
                });
        assertThat(usuarioRepository.findByUsername("inexistente")).isEmpty();
    }

    @Test
    void deveVerificarExistenciaPorUsername() {
        usuarioRepository.save(new Usuario("ana", "hash", "USER"));

        assertThat(usuarioRepository.existsByUsername("ana")).isTrue();
        assertThat(usuarioRepository.existsByUsername("pedro")).isFalse();
    }

    @Test
    void naoDevePermitirUsernameDuplicado() {
        usuarioRepository.save(new Usuario("duplicado", "hash", "USER"));

        assertThatThrownBy(() -> usuarioRepository.save(new Usuario("duplicado", "outro", "USER")))
                .hasRootCauseInstanceOf(PSQLException.class)
                .rootCause().hasMessageContaining("duplicate key");
    }
}
