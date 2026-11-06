package br.com.pediupatrao.model;
import jakarta.validation.constraints.*;
public record Endereco(
    @NotBlank @Pattern(regexp="[0-9]{8}", message="informe o CEP com 8 números") String cep,
    @NotBlank @Size(max=120) String logradouro,
    @NotBlank @Size(max=15) String numero,
    @Size(max=80) String complemento,
    @NotBlank @Size(max=80) String bairro,
    @NotBlank @Size(max=80) String cidade) {
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getResumo() { return logradouro + ", " + numero + (complemento == null || complemento.isBlank() ? "" : " — " + complemento) + ", " + bairro + ", " + cidade + " · CEP " + cep; }
}
