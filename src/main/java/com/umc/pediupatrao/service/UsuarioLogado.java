package com.umc.pediupatrao.service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Identifica o usuário autenticado na requisição atual.
 */
@Component
public class UsuarioLogado {

    private static final String PREFIXO_ROLE = "ROLE_";

    public String getUsername() {
        Authentication auth = autenticacao();
        return auth != null ? auth.getName() : "sistema";
    }

    public String getPerfil() {
        Authentication auth = autenticacao();
        if (auth == null) {
            return "SISTEMA";
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(PREFIXO_ROLE))
                .map(a -> a.substring(PREFIXO_ROLE.length()))
                .findFirst()
                .orElse("SEM_PERFIL");
    }

    private Authentication autenticacao() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken) ? auth : null;
    }
}
