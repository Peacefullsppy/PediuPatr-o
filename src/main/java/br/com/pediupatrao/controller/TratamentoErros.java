package br.com.pediupatrao.controller;
import br.com.pediupatrao.exception.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestControllerAdvice(annotations=RestController.class)
public class TratamentoErros {
    private ResponseEntity<Map<String,Object>> resposta(HttpStatus status,String mensagem) {
        return ResponseEntity.status(status).body(Map.of("status",status.value(),"mensagem",mensagem));
    }
    @ExceptionHandler(RegraNegocioException.class) ResponseEntity<?> regra(RegraNegocioException e) { return resposta(HttpStatus.UNPROCESSABLE_ENTITY,e.getMessage()); }
    @ExceptionHandler(NaoEncontradoException.class) ResponseEntity<?> ausente(NaoEncontradoException e) { return resposta(HttpStatus.NOT_FOUND,e.getMessage()); }
    @ExceptionHandler(AccessDeniedException.class) ResponseEntity<?> acesso() { return resposta(HttpStatus.FORBIDDEN,"Seu perfil não pode executar esta operação."); }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validacao(MethodArgumentNotValidException e) {
        var erros=e.getBindingResult().getFieldErrors().stream().map(f -> f.getField()+": "+f.getDefaultMessage()).distinct().sorted().toList();
        return ResponseEntity.badRequest().body(Map.of("status",400,"mensagem","Confira os campos informados.","erros",erros));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, BindException.class, MissingServletRequestParameterException.class})
    ResponseEntity<?> parametros() { return resposta(HttpStatus.BAD_REQUEST,"Verifique os filtros: datas no formato AAAA-MM-DD e valores válidos para status, tipo e agrupamento."); }
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<?> formato() {
        return resposta(HttpStatus.BAD_REQUEST,"Dados inválidos ou campos não permitidos. Envie somente os campos previstos na operação.");
    }
    @ExceptionHandler(OptimisticLockingFailureException.class) ResponseEntity<?> concorrencia() {
        return resposta(HttpStatus.CONFLICT,"O registro mudou durante a operação. Atualize a página e tente novamente.");
    }
    @ExceptionHandler(DuplicateKeyException.class) ResponseEntity<?> duplicado() { return resposta(HttpStatus.CONFLICT,"Já existe um registro com este identificador."); }
    @ExceptionHandler(DataAccessException.class) ResponseEntity<?> banco() {
        return resposta(HttpStatus.SERVICE_UNAVAILABLE,"Não foi possível concluir a gravação. Atualize a página antes de repetir a operação.");
    }
}
