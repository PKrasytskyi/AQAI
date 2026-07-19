package ua.demo.agentlab.ui.discovery.spa;

import java.util.Locale;

/** Tracks authentication state across requirement-owned actions in one live discovery session. */
public final class LiveAuthenticationState {

    private boolean authenticated;

    public boolean authenticated() {
        return authenticated;
    }

    public void markAuthenticated() {
        authenticated = true;
    }

    public void observe(String intent, boolean verified, boolean authenticatedRouteReached,
                        boolean sessionEndingActionExecuted) {
        if (!verified) {
            return;
        }
        String normalized = intent == null ? "" : intent.trim().toUpperCase(Locale.ROOT);
        if ("SUBMIT_FORM".equals(normalized) && authenticatedRouteReached) {
            authenticated = true;
        } else if ("LOGOUT".equals(normalized) && sessionEndingActionExecuted) {
            authenticated = false;
        }
    }
}
