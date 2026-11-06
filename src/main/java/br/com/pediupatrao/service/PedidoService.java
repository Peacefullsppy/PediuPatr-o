package br.com.pediupatrao.service;
import br.com.pediupatrao.dto.Formularios.*;
import br.com.pediupatrao.model.*;
import br.com.pediupatrao.repository.*;
import br.com.pediupatrao.security.UsuarioAtual;
import br.com.pediupatrao.exception.*;
import java.math.*;
import java.time.Clock;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
public class PedidoService {
    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final ProdutoRepository produtos;
    private final AuditoriaService auditoria;
    private final UsuarioAtual atual;
    private final Clock clock;
    public PedidoService(PedidoRepository pedidos, ClienteRepository clientes, ProdutoRepository produtos,
            AuditoriaService auditoria, UsuarioAtual atual, Clock clock) {
        this.pedidos=pedidos; this.clientes=clientes; this.produtos=produtos;
        this.auditoria=auditoria; this.atual=atual; this.clock=clock;
    }
    public Page<Pedido> listar(int pagina) { return pedidos.findAllByOrderByEntradaEmDesc(PageRequest.of(Math.max(0,pagina),20)); }
    public Pedido buscar(String id) { return pedidos.findById(id).orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado.")); }
    @Transactional @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido criar(PedidoForm dados) {
        var cliente=clientes.findById(dados.clienteId()).filter(Cliente::ativo)
            .orElseThrow(() -> new RegraNegocioException("Selecione um cliente ativo."));
        var itens=montarItens(dados.itens());
        var subtotal=somar(itens);
        var pedido=new Pedido(null,null,cliente.id(),cliente.nome(),cliente.telefone(),
            dados.tipoAtendimento()==TipoAtendimento.ENTREGA ? cliente.endereco() : null,
            dados.tipoAtendimento(),itens,subtotal,new BigDecimal("0.00"),new BigDecimal("0.00"),subtotal,
            StatusPedido.RECEBIDO,atual.obter(),clock.instant(),null,null,null,null,null);
        var salvo=pedidos.insert(pedido);
        auditoria.registrar("INCLUSAO_PEDIDO","PEDIDO",salvo.id(),Map.of(),RetratosAuditoria.pedido(salvo),null);
        return salvo;
    }
    @Transactional @PreAuthorize("hasRole('GERENTE')")
    public Pedido alterarItens(String id, ItensForm dados) {
        var antes=buscar(id);
        if (antes.status()!=StatusPedido.RECEBIDO)
            throw new RegraNegocioException("Os itens só podem ser alterados enquanto o pedido está recebido.");
        var itens=montarItens(dados.itens());
        return salvarAlteracao(antes,recalcular(antes,itens,antes.percentualDesconto()),"ALTERACAO_ITENS",null);
    }
    @Transactional @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, DescontoForm dados) {
        var antes=buscar(id);
        exigirAntesDaSaida(antes);
        var percentual=dados.percentual();
        if (percentual==null || percentual.signum()<0 || percentual.compareTo(new BigDecimal("20"))>0 || percentual.scale()>2)
            throw new RegraNegocioException("O desconto deve ficar entre 0% e 20%, com até duas casas decimais.");
        // O percentual substitui o desconto anterior; descontos sucessivos não se acumulam.
        return salvarAlteracao(antes,recalcular(antes,antes.itens(),percentual.setScale(2)),"DESCONTO_PEDIDO",null);
    }
    @Transactional @PreAuthorize("hasRole('GERENTE')")
    public Pedido mudarStatus(String id, StatusForm dados) {
        var antes=buscar(id); var destino=dados.status();
        if (destino==null || !antes.status().proximos(antes.tipoAtendimento()).contains(destino))
            throw new RegraNegocioException("Transição de status não permitida.");
        boolean saiu=destino==StatusPedido.SAIU_PARA_ENTREGA || destino==StatusPedido.RETIRADO;
        var depois=new Pedido(antes.id(),antes.versao(),antes.clienteId(),antes.clienteNome(),antes.telefoneCliente(),
            antes.enderecoEntrega(),antes.tipoAtendimento(),antes.itens(),antes.subtotal(),antes.percentualDesconto(),
            antes.valorDesconto(),antes.total(),destino,antes.registradoPor(),antes.entradaEm(),
            saiu ? atual.obter() : antes.saidaPor(),saiu ? clock.instant() : antes.saidaEm(),
            destino==StatusPedido.FINALIZADO ? clock.instant() : antes.finalizadoEm(),null,null);
        return salvarAlteracao(antes,depois,"STATUS_PEDIDO",null);
    }
    @Transactional @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, CancelamentoForm dados) {
        var antes=buscar(id); exigirAntesDaSaida(antes);
        var motivo=dados.justificativa()==null ? "" : dados.justificativa().trim();
        if (motivo.length()<5 || motivo.length()>300)
            throw new RegraNegocioException("Explique o cancelamento em 5 a 300 caracteres.");
        var depois=new Pedido(antes.id(),antes.versao(),antes.clienteId(),antes.clienteNome(),antes.telefoneCliente(),
            antes.enderecoEntrega(),antes.tipoAtendimento(),antes.itens(),antes.subtotal(),antes.percentualDesconto(),
            antes.valorDesconto(),antes.total(),StatusPedido.CANCELADO,antes.registradoPor(),antes.entradaEm(),
            null,null,null,motivo,clock.instant());
        return salvarAlteracao(antes,depois,"CANCELAMENTO_PEDIDO",motivo);
    }
    private void exigirAntesDaSaida(Pedido p) {
        if (!p.status().antesDaSaida()) throw new RegraNegocioException("Operação permitida apenas antes da saída e em pedido não cancelado.");
    }
    private List<ItemPedido> montarItens(List<ItemForm> dados) {
        if (dados==null || dados.isEmpty() || dados.size()>30) throw new RegraNegocioException("Informe de 1 a 30 itens.");
        Set<String> usados=new HashSet<>(); List<ItemPedido> itens=new ArrayList<>();
        for (var item : dados) {
            if (item==null || item.quantidade()<1 || item.quantidade()>99 || !usados.add(item.produtoId()))
                throw new RegraNegocioException("Verifique as quantidades e não repita o mesmo produto.");
            var produto=produtos.findById(item.produtoId()).filter(Produto::ativo)
                .orElseThrow(() -> new RegraNegocioException("Produto indisponível."));
            itens.add(new ItemPedido(produto.id(),produto.nome(),item.quantidade(),produto.preco()));
        }
        return List.copyOf(itens);
    }
    private BigDecimal somar(List<ItemPedido> itens) {
        return itens.stream().map(ItemPedido::getSubtotal).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
    }
    private Pedido recalcular(Pedido p, List<ItemPedido> itens, BigDecimal percentual) {
        var subtotal=somar(itens);
        var desconto=subtotal.multiply(percentual).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
        return new Pedido(p.id(),p.versao(),p.clienteId(),p.clienteNome(),p.telefoneCliente(),p.enderecoEntrega(),
            p.tipoAtendimento(),itens,subtotal,percentual,desconto,subtotal.subtract(desconto),p.status(),
            p.registradoPor(),p.entradaEm(),p.saidaPor(),p.saidaEm(),p.finalizadoEm(),p.justificativaCancelamento(),p.canceladoEm());
    }
    private Pedido salvarAlteracao(Pedido antes, Pedido depois, String operacao, String motivo) {
        var salvo=pedidos.save(depois);
        auditoria.registrar(operacao,"PEDIDO",salvo.id(),RetratosAuditoria.pedido(antes),RetratosAuditoria.pedido(salvo),motivo);
        return salvo;
    }
}
