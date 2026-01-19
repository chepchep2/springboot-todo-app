package com.chep.demo.todo.domain.user;

import com.chep.demo.todo.domain.user.event.UserRegisteredEvent;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.util.ArrayList;
import java.util.List;

@Entity
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

    protected User() {}

    private User(String name, String email, String password) {
        if (name == null || email == null || password == null) {
            throw new IllegalArgumentException("name, email, password must not be null");
        }
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public static class Builder {
        private String name;
        private String email;
        private String password;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public User build() {
            return new User(name, email, password);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Transient
    private final List<UserRegisteredEvent> domainEvents = new ArrayList<>();

    public static User register(String name, String email, String encodedPassword) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password(encodedPassword)
                .build();

        user.domainEvents.add(new UserRegisteredEvent(user));
        return user;
    }

    @DomainEvents
    public List<UserRegisteredEvent> getDomainEvents() {
        return domainEvents;
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
