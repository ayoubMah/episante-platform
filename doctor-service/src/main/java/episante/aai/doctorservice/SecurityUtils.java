package episante.aai.doctorservice;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

public class SecurityUtils {

    // 1. Fetch Authentication safely
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    // 2. Extract username (email)
    public static String getCurrentUsername() {
        Authentication auth = getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    // 3. Extract userId from authentication details
    // (The filter must put the ID inside authentication details)
    public static UUID getCurrentUserId() {
        Authentication auth = getAuthentication();
        if (auth == null || auth.getDetails() == null) return null;

        if (auth.getDetails() instanceof UserContext ctx) {
            return ctx.id();
        }
        return null;
    }

    // 4. Extract role
    public static String getCurrentRole() {
        Authentication auth = getAuthentication();
        if (auth == null) return null;

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority) // "ROLE_DOCTOR"
                .findFirst()
                .orElse(null);
    }

    // 5. Role helpers
    public static boolean hasRole(String roleName) {
        Authentication auth = getAuthentication();
        if (auth == null) return false;

        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + roleName));
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public static boolean isDoctor() {
        return hasRole("DOCTOR");
    }

    public static boolean isPatient() {
        return hasRole("PATIENT");
    }
}
