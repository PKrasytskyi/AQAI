package core;

/**
 * @deprecated Use {@link ua.demo.agentlab.core.api.ApiManager}. This facade is
 * kept only so older generated API clients that import {@code core.ApiManager}
 * continue to compile.
 */
@Deprecated(since = "0.0.1", forRemoval = false)
public class ApiManager extends ua.demo.agentlab.core.api.ApiManager {

    public ApiManager() {
        super();
    }

    public ApiManager(String baseUrl, String bearerToken) {
        super(baseUrl, bearerToken);
    }
}
