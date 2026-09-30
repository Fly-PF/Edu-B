package com.edu.problemsolving.socket;

import java.security.Principal;

public record ProblemSocketPrincipal(String clientSessionId) implements Principal {
    @Override
    public String getName() {
        return clientSessionId;
    }
}
