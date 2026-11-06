package br.com.pediupatrao.controller;
import br.com.pediupatrao.model.RegistroAuditoria;
import br.com.pediupatrao.service.AuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auditoria")
public class AuditoriaController {
    private final AuditoriaService auditoria;
    public AuditoriaController(AuditoriaService auditoria) { this.auditoria=auditoria; }
    @GetMapping public Page<RegistroAuditoria> listar(@RequestParam(required=false) String entidade,
        @RequestParam(required=false) String entidadeId,@RequestParam(defaultValue="0") int pagina) {
        return auditoria.consultar(entidade,entidadeId,pagina);
    }
}
