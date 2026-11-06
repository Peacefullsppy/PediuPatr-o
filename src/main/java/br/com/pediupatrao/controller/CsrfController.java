package br.com.pediupatrao.controller;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
@RestController
public class CsrfController {
    @GetMapping("/api/csrf") public Map<String,String> token(CsrfToken token) {
        return Map.of("headerName",token.getHeaderName(),"parameterName",token.getParameterName(),"token",token.getToken());
    }
}
