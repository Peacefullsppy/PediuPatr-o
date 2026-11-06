package br.com.pediupatrao.service;
import br.com.pediupatrao.model.*;
import java.util.*;
final class RetratosAuditoria {
    private RetratosAuditoria() {}
    private static void campo(Map<String,String> mapa, String nome, Object valor) {
        mapa.put(nome, valor == null ? null : valor.toString());
    }
    static Map<String,String> cliente(Cliente c) {
        var m = new LinkedHashMap<String,String>();
        campo(m,"nome",c.nome()); campo(m,"telefone",c.telefone()); campo(m,"ativo",c.ativo());
        campo(m,"cep",c.endereco().cep()); campo(m,"logradouro",c.endereco().logradouro());
        campo(m,"numero",c.endereco().numero()); campo(m,"complemento",c.endereco().complemento());
        campo(m,"bairro",c.endereco().bairro()); campo(m,"cidade",c.endereco().cidade());
        return m;
    }
    static Map<String,String> pedido(Pedido p) {
        var m = new LinkedHashMap<String,String>();
        campo(m,"clienteId",p.clienteId()); campo(m,"clienteNome",p.clienteNome());
        campo(m,"telefoneCliente",p.telefoneCliente());
        campo(m,"enderecoEntrega",p.enderecoEntrega() == null ? null : p.enderecoEntrega().getResumo());
        campo(m,"tipoAtendimento",p.tipoAtendimento());
        for (int i=0;i<p.itens().size();i++) {
            var item = p.itens().get(i); String prefixo="itens["+i+"].";
            campo(m,prefixo+"produtoId",item.produtoId()); campo(m,prefixo+"descricao",item.descricao());
            campo(m,prefixo+"quantidade",item.quantidade()); campo(m,prefixo+"precoUnitario",item.precoUnitario());
        }
        campo(m,"subtotal",p.subtotal()); campo(m,"percentualDesconto",p.percentualDesconto());
        campo(m,"valorDesconto",p.valorDesconto()); campo(m,"total",p.total()); campo(m,"status",p.status());
        campo(m,"registradoPor",p.registradoPor()); campo(m,"entradaEm",p.entradaEm());
        campo(m,"saidaPor",p.saidaPor()); campo(m,"saidaEm",p.saidaEm()); campo(m,"finalizadoEm",p.finalizadoEm());
        campo(m,"justificativaCancelamento",p.justificativaCancelamento()); campo(m,"canceladoEm",p.canceladoEm());
        return m;
    }
    static Map<String,String> usuario(Usuario u) {
        // Lista explícita: senhaHash nunca entra na auditoria.
        return Map.of("login",u.login(),"nome",u.nome(),"perfil",u.perfil().name(),"ativo",String.valueOf(u.ativo()));
    }
}
