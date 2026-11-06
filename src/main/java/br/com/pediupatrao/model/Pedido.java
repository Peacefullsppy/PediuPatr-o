package br.com.pediupatrao.model;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.*;
import org.springframework.data.mongodb.core.index.Indexed;
@Document("pedidos")
public record Pedido(
    @Id String id, @Version Long versao,
    String clienteId, String clienteNome, String telefoneCliente, Endereco enderecoEntrega,
    TipoAtendimento tipoAtendimento, List<ItemPedido> itens,
    @Field(targetType=FieldType.DECIMAL128) BigDecimal subtotal,
    @Field(targetType=FieldType.DECIMAL128) BigDecimal percentualDesconto,
    @Field(targetType=FieldType.DECIMAL128) BigDecimal valorDesconto,
    @Field(targetType=FieldType.DECIMAL128) BigDecimal total,
    StatusPedido status, Ator registradoPor, @Indexed Instant entradaEm,
    Ator saidaPor, Instant saidaEm, Instant finalizadoEm,
    String justificativaCancelamento, Instant canceladoEm) {
    public Pedido { itens = List.copyOf(itens); }
    public String getNumero() { return id == null ? "Novo" : id.substring(Math.max(0, id.length() - 6)).toUpperCase(); }
    public List<StatusPedido> getProximosStatus() { return status.proximos(tipoAtendimento); }
}
