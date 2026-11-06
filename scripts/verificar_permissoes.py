#!/usr/bin/env python3
"""Demonstra bloqueios usando sessão real e CSRF, sem depender das telas."""
import getpass
import http.cookiejar
import json
import urllib.error
import urllib.parse
import urllib.request


def main():
    base = input("Endereço [http://localhost:8080]: ").strip() or "http://localhost:8080"
    base = base.rstrip("/")
    login = input("Login (admin, gerente ou atendente cadastrado): ").strip()
    senha = getpass.getpass("Senha: ")
    sessao = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def chamar(metodo, caminho, corpo=None, csrf=None, formulario=False):
        headers = {}
        dados = None
        if corpo is not None:
            dados = (urllib.parse.urlencode(corpo) if formulario else json.dumps(corpo)).encode()
            headers['Content-Type'] = 'application/x-www-form-urlencoded' if formulario else 'application/json'
        if csrf:
            headers[csrf['headerName']] = csrf['token']
        req = urllib.request.Request(base + caminho, data=dados, headers=headers, method=metodo)
        try:
            with sessao.open(req, timeout=20) as res:
                texto = res.read().decode()
                try: conteudo = json.loads(texto)
                except json.JSONDecodeError: conteudo = None
                return res.status, conteudo, res.url
        except urllib.error.HTTPError as erro:
            texto = erro.read().decode()
            try: conteudo = json.loads(texto)
            except json.JSONDecodeError: conteudo = None
            return erro.code, conteudo, erro.url

    _, token, _ = chamar('GET', '/api/csrf')
    _, _, destino = chamar('POST', '/login', {'username': login, 'password': senha, token['parameterName']: token['token']}, formulario=True)
    if '/login' in destino:
        raise SystemExit('Login não realizado. Confira as credenciais e a situação da conta.')
    _, token, _ = chamar('GET', '/api/csrf')
    pedido = input('ID completo de um pedido ainda antes da saída: ').strip()
    pedido = urllib.parse.quote(pedido, safe='')
    status, atual, _ = chamar('GET', '/api/pedidos/' + pedido)
    if status != 200:
        raise SystemExit('Pedido não encontrado ou sem acesso. Informe um ID válido para uma evidência conclusiva.')
    if atual['status'] not in ('RECEBIDO', 'EM_PREPARACAO', 'PRONTO'):
        raise SystemExit('Use um pedido recebido, em preparação ou pronto.')
    # O perfil vem de uma rota protegida: GERENTE pode gerenciar, ADMIN pode consultar usuários.
    admin_status, _, _ = chamar('GET', '/api/usuarios')
    audit_status, _, _ = chamar('GET', '/api/auditoria')
    perfil = 'ADMIN' if admin_status == 200 else 'GERENTE' if audit_status == 200 else 'ATENDENTE'
    print('Perfil identificado:', perfil)
    casos = [
        ('Desconto sem CSRF', 'POST', '/api/pedidos/' + pedido + '/desconto', {'percentual': 10}, None, 403),
        ('Consultar auditoria', 'GET', '/api/auditoria', None, None, 403 if perfil == 'ATENDENTE' else 200),
    ]
    if perfil in ('ADMIN', 'ATENDENTE'):
        casos += [
            ('Desconto com CSRF válido', 'POST', '/api/pedidos/' + pedido + '/desconto', {'percentual': 10}, token, 403),
            ('Cancelamento com CSRF válido', 'POST', '/api/pedidos/' + pedido + '/cancelamento', {'justificativa': 'Tentativa não autorizada'}, token, 403),
        ]
    else:
        casos += [
            ('Desconto acima de 20%', 'POST', '/api/pedidos/' + pedido + '/desconto', {'percentual': 25}, token, 400),
            ('Pular para finalizado', 'POST', '/api/pedidos/' + pedido + '/status', {'status': 'FINALIZADO'}, token, 422),
        ]
    falhas = 0
    for descricao, metodo, caminho, corpo, csrf, esperado in casos:
        status, resultado, _ = chamar(metodo, caminho, corpo, csrf)
        ok = status == esperado
        falhas += not ok
        print(f'{"OK" if ok else "FALHOU"} | {descricao}: HTTP {status} (esperado {esperado})')
        if resultado and 'mensagem' in resultado:
            print('   ' + resultado['mensagem'])
    raise SystemExit(1 if falhas else 0)


if __name__ == '__main__':
    try:
        main()
    except urllib.error.URLError:
        raise SystemExit('Não foi possível acessar o sistema. Confira se a aplicação está em execução.')
