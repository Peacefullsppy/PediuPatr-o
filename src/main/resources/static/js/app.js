'use strict';
document.querySelectorAll('time[datetime]').forEach(el => {
  const data = new Date(el.dateTime);
  if (!Number.isNaN(data.getTime())) el.textContent = new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo'
  }).format(data);
});
document.addEventListener('click', event => {
  if (event.target.matches('[data-add-item]')) {
    const editor = event.target.closest('.itens-editor');
    if (editor.querySelectorAll('[data-item]').length >= 30) return;
    const linha = editor.querySelector('[data-item]').cloneNode(true);
    linha.querySelector('select').value = '';
    linha.querySelector('input').value = '1';
    editor.append(linha);
  }
  if (event.target.matches('[data-remove-item]')) {
    const editor = event.target.closest('.itens-editor');
    if (editor.querySelectorAll('[data-item]').length > 1) event.target.closest('[data-item]').remove();
  }
});
function dadosDoFormulario(form) {
  const dados = {};
  for (const [nome, valor] of new FormData(form)) {
    if (nome === '_csrf') continue;
    let convertido = valor;
    if (nome === 'ativo') convertido = valor === 'true';
    if (['preco','percentual'].includes(nome)) convertido = Number(valor);
    if (nome.startsWith('endereco.')) {
      dados.endereco ??= {};
      dados.endereco[nome.split('.')[1]] = convertido;
    } else dados[nome] = convertido;
  }
  if (['pedido','itens'].includes(form.dataset.kind)) {
    dados.itens = [...form.querySelectorAll('[data-item]')].map(linha => ({
      produtoId: linha.querySelector('[data-produto]').value,
      quantidade: Number(linha.querySelector('[data-quantidade]').value)
    }));
  }
  return dados;
}
document.querySelectorAll('form[data-api]').forEach(form => {
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (form.dataset.confirm && !window.confirm(form.dataset.confirm)) return;
    const aviso = form.querySelector('.feedback');
    const botao = form.querySelector('button[type="submit"]');
    botao.disabled = true; aviso.textContent = '';
    try {
      const method = form.dataset.method || 'POST';
      const resposta = await fetch(form.dataset.api, {
        method, credentials: 'same-origin',
        headers: {'Content-Type':'application/json', [document.querySelector('meta[name="csrf-header"]').content]:document.querySelector('meta[name="csrf-token"]').content},
        body: method === 'DELETE' ? undefined : JSON.stringify(dadosDoFormulario(form))
      });
      const corpo = resposta.status === 204 ? {} : await resposta.json();
      if (!resposta.ok) {
        aviso.textContent = [corpo.mensagem || 'Não foi possível concluir.', ...(corpo.erros || [])].join(' ');
        return;
      }
      if (form.dataset.success === 'reload') window.location.reload();
      else window.location.assign(form.dataset.success.replace('{id}', encodeURIComponent(corpo.id || '')));
    } catch (e) {
      aviso.textContent = 'Não foi possível confirmar a operação. Atualize a página e confira os dados antes de tentar novamente.';
    } finally { botao.disabled = false; }
  });
});
