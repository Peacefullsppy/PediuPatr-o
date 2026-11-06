package br.com.pediupatrao.service;
import br.com.pediupatrao.dto.Formularios.*;
import br.com.pediupatrao.model.*;
import br.com.pediupatrao.repository.UsuarioRepository;
import br.com.pediupatrao.security.UsuarioAtual;
import br.com.pediupatrao.exception.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final UsuarioAtual atual;
    private final AuditoriaService auditoria;
    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder, UsuarioAtual atual, AuditoriaService auditoria) {
        this.usuarios=usuarios; this.encoder=encoder; this.atual=atual; this.auditoria=auditoria;
    }
    public List<UsuarioResumo> listar() { return usuarios.findAll().stream().map(UsuarioResumo::de).toList(); }
    @Transactional
    public UsuarioResumo criar(UsuarioForm dados) {
        if (usuarios.existsByLogin(dados.login())) throw new RegraNegocioException("Este login já está em uso.");
        if (dados.senha().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new RegraNegocioException("A senha excede o limite de 72 bytes. Use menos caracteres.");
        var salvo=usuarios.insert(new Usuario(null,null,dados.login(),dados.nome().trim(),encoder.encode(dados.senha()),dados.perfil(),true));
        auditoria.registrar("INCLUSAO_USUARIO","USUARIO",salvo.id(),Map.of(),RetratosAuditoria.usuario(salvo),null);
        return UsuarioResumo.de(salvo);
    }
    @Transactional
    public UsuarioResumo alterarPerfil(String id, PerfilForm dados) {
        var antes=usuarios.findById(id).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
        if (antes.login().equals(atual.obter().usuario()))
            throw new RegraNegocioException("Você não pode alterar o próprio perfil nem desativar sua conta por esta tela.");
        var salvo=usuarios.save(new Usuario(id,antes.versao(),antes.login(),antes.nome(),antes.senhaHash(),dados.perfil(),dados.ativo()));
        auditoria.registrar("ALTERACAO_USUARIO","USUARIO",id,RetratosAuditoria.usuario(antes),RetratosAuditoria.usuario(salvo),null);
        return UsuarioResumo.de(salvo);
    }
}
