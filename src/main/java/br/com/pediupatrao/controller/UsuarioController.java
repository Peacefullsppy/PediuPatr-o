package br.com.pediupatrao.controller;
import br.com.pediupatrao.dto.Formularios.*;
import br.com.pediupatrao.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarios;
    public UsuarioController(UsuarioService usuarios) { this.usuarios=usuarios; }
    @GetMapping public List<UsuarioResumo> listar() { return usuarios.listar(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public UsuarioResumo criar(@Valid @RequestBody UsuarioForm dados) { return usuarios.criar(dados); }
    @PutMapping("/{id}/perfil") public UsuarioResumo perfil(@PathVariable String id,@Valid @RequestBody PerfilForm dados) { return usuarios.alterarPerfil(id,dados); }
}
