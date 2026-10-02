package com.exemplo.authservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Arrays;
import java.util.List;

/**
 * Entidade mapeada com Spring Data JDBC (tabela criada em schema.sql).
 */
@Table("usuario")
public class Usuario {

    @Id
    private Long id;

    private String username;

    /** Senha armazenada com hash BCrypt, nunca em texto puro. */
    private String password;

    /** Perfis separados por virgula, ex.: "USER,ADMIN". */
    private String roles;

    public Usuario() {
    }

    public Usuario(String username, String password, String roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    public List<String> getRolesList() {
        return Arrays.stream(roles.split(",")).map(String::trim).toList();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }
}
