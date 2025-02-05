package su.foxogram.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import su.foxogram.exceptions.UserEmailNotVerifiedException;
import su.foxogram.exceptions.UserUnauthorizedException;
import su.foxogram.models.User;
import su.foxogram.services.AuthenticationService;
import su.foxogram.services.JwtService;

import java.util.Set;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    final AuthenticationService authenticationService;

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "email/verify",
            "users/@me",
            "email/resend"
    );

    @Autowired
    public AuthenticationInterceptor(AuthenticationService authenticationService, JwtService jwtService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws UserUnauthorizedException, UserEmailNotVerifiedException {
        String requestURI = request.getRequestURI();

        boolean checkIfEmailVerified = ALLOWED_PATHS.stream().anyMatch(requestURI::contains);

        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (accessToken == null || !accessToken.startsWith("Bearer "))
            throw new UserUnauthorizedException();

        User user = authenticationService.getUser(accessToken, checkIfEmailVerified);

        request.setAttribute("user", user);
        request.setAttribute("accessToken", accessToken);

        return true;
    }

    @Override
    public void postHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, ModelAndView modelAndView) {

    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception exception) {

    }
}