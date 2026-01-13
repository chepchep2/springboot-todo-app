package com.chep.demo.todo.domain.user.event;

import com.chep.demo.todo.domain.user.User;

public record UserRegisteredEvent(User user) {
}
