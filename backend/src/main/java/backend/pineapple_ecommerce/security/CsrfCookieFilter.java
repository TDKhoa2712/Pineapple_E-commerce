package backend.pineapple_ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter này dùng để buộc Spring Security khởi tạo CsrfToken (vì mặc định trong Spring Security 6 nó là Deferred)
 * và lưu CsrfToken đó vào Cookie XSRF-TOKEN trong response.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Gọi getToken() để kích hoạt việc sinh token và lưu vào cookie
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
