package episante.aai.authservice;

public class AuthResponse {
    private String accessToken;
    public AuthResponse(String token) { this.accessToken = token; }
    public String getAccessToken() { return accessToken; }
}
