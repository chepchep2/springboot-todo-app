package com.chep.demo.todo.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_id_gen")
    @SequenceGenerator(name = "users_id_gen", sequenceName = "user_id_seq", allocationSize = 1)
    private Long id;

    @Size(max = 200)
    @NotNull
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Size(max = 200)
    @NotNull
    @Column(name = "email", nullable = false, unique = true, length = 200)
    private String email;

    @NotNull
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Builder
    private User(String name, String email, String password) {
        if (name == null || email == null || password == null) {
            throw new IllegalArgumentException("name, email, password must not be null");
        }
        this.name = name;
        this.email = email;
        this.password = password;
    }
}
