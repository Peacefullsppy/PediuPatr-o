package br.com.pediupatrao.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
@Document("usuarios")
public record Usuario(@Id String id, @Version Long versao, @Indexed(unique=true) String login,
                      String nome, @JsonIgnore String senhaHash, Perfil perfil, boolean ativo) {}
