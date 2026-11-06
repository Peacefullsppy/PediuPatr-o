package br.com.pediupatrao.dto;
import br.com.pediupatrao.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
public final class Formularios {
    private Formularios() {}
    public record ClienteForm(@NotBlank @Size(max=100) String nome,
        @NotBlank @Pattern(regexp="[0-9]{10,11}", message="informe DDD e telefone, somente números") String telefone,
        @NotNull @Valid Endereco endereco) {}
    public record ProdutoForm(@NotBlank @Size(max=100) String nome,
        @NotNull @DecimalMin("0.01") @DecimalMax("9999.99") @Digits(integer=4, fraction=2) BigDecimal preco,
        @NotNull Boolean ativo) {}
    public record ItemForm(@NotBlank String produtoId, @Min(1) @Max(99) int quantidade) {}
    public record PedidoForm(@NotBlank String clienteId, @NotNull TipoAtendimento tipoAtendimento,
        @NotEmpty @Size(max=30) List<@NotNull @Valid ItemForm> itens) {}
    public record ItensForm(@NotEmpty @Size(max=30) List<@NotNull @Valid ItemForm> itens) {}
    public record StatusForm(@NotNull StatusPedido status) {}
    public record DescontoForm(@NotNull @DecimalMin("0.00") @DecimalMax("20.00")
        @Digits(integer=2, fraction=2) BigDecimal percentual) {}
    public record CancelamentoForm(@NotBlank @Size(min=5, max=300) String justificativa) {}
    public record UsuarioForm(@NotBlank @Pattern(regexp="[a-z0-9._-]{3,40}") String login,
        @NotBlank @Size(max=100) String nome, @NotBlank @Size(min=12,max=64) String senha,
        @NotNull Perfil perfil) {}
    public record PerfilForm(@NotNull Perfil perfil, @NotNull Boolean ativo) {}
    public record UsuarioResumo(String id, String login, String nome, Perfil perfil, boolean ativo) {
        public static UsuarioResumo de(Usuario u) { return new UsuarioResumo(u.id(), u.login(), u.nome(), u.perfil(), u.ativo()); }
    }
}
