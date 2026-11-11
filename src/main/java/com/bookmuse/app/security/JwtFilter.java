package com.bookmuse.app.security;
import com.bookmuse.app.service.AuthService;
import com.bookmuse.app.exception.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
@Component @RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final TokenService tokens;
    private final AuthService auth;
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String header=req.getHeader("Authorization");
        if(header!=null && header.startsWith("Bearer ")) {
            try {
                var user=auth.active(tokens.verify(header.substring(7)));
                tokens.checkVersion(header.substring(7),user);
                var principal=new UsernamePasswordAuthenticationToken(user.getId().toString(),null,List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole())));
                SecurityContextHolder.getContext().setAuthentication(principal);
            } catch(ApiException ex) { res.setStatus(401); res.setContentType("application/problem+json"); res.getWriter().write("{\"status\":401,\"title\":\"Unauthorized\"}"); return; }
        }
        chain.doFilter(req,res);
    }
}
