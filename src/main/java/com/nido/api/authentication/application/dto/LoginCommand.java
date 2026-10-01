package com.nido.api.authentication.application.dto;

public record LoginCommand(String identifier, String password) {
    @Override
    public String toString() {
        return "LoginCommand[identifier=***, password=***]";
    }
}
