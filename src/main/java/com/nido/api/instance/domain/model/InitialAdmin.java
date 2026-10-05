package com.nido.api.instance.domain.model;

import com.nido.api.shared.model.Language;

/** The first SUPER_ADMIN. {@code language} may be null (an installation seeded from the environment). */
public record InitialAdmin(String username, String email, String password, Language language) {
    @Override
    public String toString() {
        return "InitialAdmin[username=" + username + ", email=***, password=***, language=" + language + "]";
    }
}
