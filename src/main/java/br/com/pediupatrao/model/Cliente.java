package br.com.pediupatrao.model;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;
@Document("clientes")
public record Cliente(@Id String id, @Version Long versao, String nome, String telefone, Endereco endereco, boolean ativo) {}
