package br.com.pediupatrao.model;
import java.math.BigDecimal;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.*;
@Document("produtos")
public record Produto(@Id String id, @Version Long versao, String nome,
                      @Field(targetType=FieldType.DECIMAL128) BigDecimal preco, boolean ativo) {}
