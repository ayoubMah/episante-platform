package episante.aai.appointmentservice;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        // 1. Get the current incoming HTTP request (from Postman/Frontend)
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            // 2. Extract the "Authorization" header (Bearer eyJhbGciOi...)
            String authHeader = attributes.getRequest().getHeader(AUTHORIZATION_HEADER);

            // 3. If it exists, inject it into the Feign outgoing request
            if (authHeader != null) {
                template.header(AUTHORIZATION_HEADER, authHeader);
            }
        }
    }
}