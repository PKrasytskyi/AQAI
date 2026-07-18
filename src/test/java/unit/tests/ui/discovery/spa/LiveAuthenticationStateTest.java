package unit.tests.ui.discovery.spa;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ui.discovery.spa.LiveAuthenticationState;

public class LiveAuthenticationStateTest {

    @Test
    public void preservesAuthenticationAfterConfirmedSubmitTransition() {
        LiveAuthenticationState state = new LiveAuthenticationState();

        state.observe("SUBMIT_FORM", true, true, false);

        Assert.assertTrue(state.authenticated());
    }

    @Test
    public void clearsAuthenticationOnlyWhenLogoutWasExecuted() {
        LiveAuthenticationState state = new LiveAuthenticationState();
        state.markAuthenticated();

        state.observe("LOGOUT", true, false, false);
        Assert.assertTrue(state.authenticated());

        state.observe("LOGOUT", true, false, true);
        Assert.assertFalse(state.authenticated());
    }
}
