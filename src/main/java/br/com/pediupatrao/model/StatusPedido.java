package br.com.pediupatrao.model;
import java.util.List;
public enum StatusPedido {
    RECEBIDO("Recebido"), EM_PREPARACAO("Em preparação"), PRONTO("Pronto"),
    SAIU_PARA_ENTREGA("Saiu para entrega"), RETIRADO("Retirado"),
    FINALIZADO("Finalizado"), CANCELADO("Cancelado");
    private final String descricao;
    StatusPedido(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
    public boolean antesDaSaida() { return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO; }
    public List<StatusPedido> proximos(TipoAtendimento tipo) {
        return switch (this) {
            case RECEBIDO -> List.of(EM_PREPARACAO);
            case EM_PREPARACAO -> List.of(PRONTO);
            case PRONTO -> List.of(tipo == TipoAtendimento.ENTREGA ? SAIU_PARA_ENTREGA : RETIRADO);
            case SAIU_PARA_ENTREGA, RETIRADO -> List.of(FINALIZADO);
            default -> List.of();
        };
    }
}
