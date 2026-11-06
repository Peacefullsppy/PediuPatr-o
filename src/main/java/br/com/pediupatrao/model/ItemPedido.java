package br.com.pediupatrao.model;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.data.mongodb.core.mapping.*;
public record ItemPedido(String produtoId, String descricao, int quantidade,
                         @Field(targetType=FieldType.DECIMAL128) BigDecimal precoUnitario) {
    public BigDecimal getSubtotal() { return precoUnitario.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_UP); }
}
