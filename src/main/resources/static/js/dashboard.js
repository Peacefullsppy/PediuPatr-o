'use strict';
(() => {
  const raiz = document.querySelector('[data-dashboard]');
  if (!raiz) return;
  const campo = id => document.getElementById(id);
  const formulario = campo('dash-filtros');
  const moeda = valor => new Intl.NumberFormat('pt-BR', {style:'currency', currency:'BRL'}).format(Number(valor) || 0);
  const numero = valor => new Intl.NumberFormat('pt-BR').format(Number(valor) || 0);
  const dataHora = iso => new Intl.DateTimeFormat('pt-BR', {
    timeZone:'America/Sao_Paulo', day:'2-digit', month:'2-digit',
    year:'numeric', hour:'2-digit', minute:'2-digit'
  }).format(new Date(iso));
  const rotuloData = (data, agrupamento) => {
    const [,mes,dia] = data.split('-');
    return agrupamento === 'MES' ? `${mes}/${data.slice(2,4)}` : `${dia}/${mes}`;
  };
  const cores = {
    RECEBIDO:'#9baebf', EM_PREPARACAO:'#d6ac64', PRONTO:'#75a27a',
    SAIU_PARA_ENTREGA:'#76a5c1', RETIRADO:'#7f8dad', FINALIZADO:'#365b40', CANCELADO:'#d27d70'
  };
  let controlador = null;
  let ultimaRequisicao = 0;

  function criar(tag, classe, texto) {
    const elemento = document.createElement(tag);
    if (classe) elemento.className = classe;
    if (texto !== undefined) elemento.textContent = String(texto);
    return elemento;
  }
  function svgElemento(tag, atributos = {}, texto) {
    const elemento = document.createElementNS('http://www.w3.org/2000/svg', tag);
    for (const [nome, valor] of Object.entries(atributos)) elemento.setAttribute(nome, String(valor));
    if (texto !== undefined) elemento.textContent = texto;
    return elemento;
  }
  function montarGrafico(series, agrupamento) {
    const svg = campo('dash-chart');
    svg.replaceChildren();
    const holder = campo('dash-chart-holder');
    const vazio = campo('dash-chart-empty');
    const max = Math.max(0, ...series.map(item => Number(item.faturamento)));
    vazio.hidden = max > 0;
    holder.hidden = max === 0;
    if (max === 0) return;
    const L = 77, R = 25, T = 20, B = 45, W = 800, H = 294;
    const w = W-L-R, h = H-T-B;
    const teto = Math.max(max * 1.12, 1);
    for (let i = 0; i <= 4; i++) {
      const y = T+h-(i/4)*h;
      svg.append(svgElemento('line',{x1:L, y1:y, x2:W-R, y2:y, stroke:'#e8ece5', 'stroke-width':1}));
      svg.append(svgElemento('text',{x:L-11,y:y+4,'text-anchor':'end','font-size':11,fill:'#829084'},
        (teto*i/4 >= 1000 ? `${(teto*i/4000).toFixed(1)} mil` : `${Math.round(teto*i/4)}`)));
    }
    const coords = series.map((v, i) => ({
      x:L + (series.length === 1 ? w/2 : i*w/(series.length-1)),
      y:T+h-(Number(v.faturamento)/teto)*h,
      ...v
    }));
    let passos = coords.map((c, i) => `${i ? 'L' : 'M'}${c.x.toFixed(2)},${c.y.toFixed(2)}`).join(' ');
    const area = `${passos} L${coords.at(-1).x.toFixed(2)},${T+h} L${coords[0].x.toFixed(2)},${T+h} Z`;
    svg.append(svgElemento('path',{d:area,fill:'#d7e8d9','fill-opacity':.65}));
    svg.append(svgElemento('path',{d:passos,class:'dash-chart-line'}));
    const salto = Math.max(1, Math.ceil(coords.length/7));
    coords.forEach((p,i) => {
      if (i % salto === 0 || i === coords.length-1) {
        svg.append(svgElemento('text',{x:p.x,y:H-14,'text-anchor':'middle','font-size':11,fill:'#829084'},
          rotuloData(p.periodo,agrupamento)));
      }
      const ponto = svgElemento('circle',{cx:p.x,cy:p.y,r:coords.length>75 ? 2 : 3.4,fill:'#356849'});
      ponto.append(svgElemento('title',{},`${rotuloData(p.periodo,agrupamento)}: ${moeda(p.faturamento)}, ${numero(p.pedidos)} pedido(s)`));
      svg.append(ponto);
    });
  }
  function montarSituacao(itens, total) {
    campo('dash-donut-num').textContent = numero(total);
    let acumulado = 0;
    const fatias = [];
    const legenda = campo('dash-legenda');
    legenda.replaceChildren();
    itens.forEach(item => {
      const cor = cores[item.status] || '#cccccc';
      const parte = total ? Number(item.quantidade) / total * 100 : 0;
      if (parte > 0) { fatias.push(`${cor} ${acumulado}% ${acumulado+parte}%`); acumulado += parte; }
      const linha = criar('div');
      const ponto = criar('span','dash-legend-dot'); ponto.style.background = cor;
      const nome = criar('span','dash-legend-name',item.descricao);
      const quantidade = criar('strong','dash-legend-number',numero(item.quantidade));
      linha.append(ponto,nome,quantidade); legenda.append(linha);
    });
    campo('dash-donut').style.background = fatias.length ? `conic-gradient(${fatias.join(',')})` : '#e7e9e1';
    campo('dash-donut').setAttribute('aria-label',`${numero(total)} pedidos distribuídos por status`);
  }
  function montarProdutos(itens) {
    const alvo = campo('dash-produtos'); alvo.replaceChildren();
    if (!itens.length) { alvo.append(criar('p','dash-inline-empty','Nenhum produto vendido no período.')); return; }
    const maior = Math.max(...itens.map(item => Number(item.quantidade)),1);
    itens.forEach((item,i) => {
      const container = criar('div','dash-product');
      const linha = criar('div','dash-product-info');
      linha.append(criar('strong','',`${i+1}. ${item.nome}`),criar('span','',`${numero(item.quantidade)} un.`));
      const progresso = criar('div','dash-progress');
      const preenchimento = criar('div'); preenchimento.style.width = `${Math.max(0,Number(item.quantidade)/maior*100)}%`;
      progresso.append(preenchimento);
      container.append(linha,progresso,criar('p','dash-product-caption',`${moeda(item.faturamento)} em vendas`));
      alvo.append(container);
    });
  }
  function montarPedidos(itens) {
    const alvo = campo('dash-pedidos'); alvo.replaceChildren();
    if (!itens.length) {
      const linha = criar('tr'); const celula = criar('td','dash-no-results','Nenhum pedido encontrado.');
      celula.colSpan = 4; linha.append(celula); alvo.append(linha); return;
    }
    itens.forEach(item => {
      const linha = criar('tr');
      const pedido = criar('td');
      const link = criar('a','',`#${item.numero}`);
      link.href = `/pedidos/${encodeURIComponent(item.id)}`;
      pedido.append(link,criar('small','',dataHora(item.entradaEm)));
      const cliente = criar('td','',item.cliente);
      const valor = criar('td','',moeda(item.total));
      const situacao = criar('td');
      const status = criar('span',`status ${item.status}`,
        ({RECEBIDO:'Recebido',EM_PREPARACAO:'Em preparação',PRONTO:'Pronto',SAIU_PARA_ENTREGA:'Saiu para entrega',RETIRADO:'Retirado',FINALIZADO:'Finalizado',CANCELADO:'Cancelado'})[item.status] || item.status);
      situacao.append(status); linha.append(pedido,cliente,valor,situacao); alvo.append(linha);
    });
  }
  function atualizarTela(dados) {
    const r = dados.resumo;
    campo('dash-faturamento').textContent = moeda(r.faturamento);
    campo('dash-total').textContent = numero(r.totalPedidos);
    campo('dash-finalizados').textContent = numero(r.pedidosFinalizados);
    campo('dash-ticket').textContent = moeda(r.ticketMedio);
    campo('dash-andamento').textContent = numero(r.pedidosEmAndamento);
    campo('dash-cancelados').textContent = numero(r.pedidosCancelados);
    campo('dash-taxa').textContent = `${numero(r.taxaCancelamento)}% de todos os pedidos`;
    campo('dash-atualizado').textContent = `Atualizado em ${dataHora(dados.periodo.atualizadoEm)}`;
    campo('dash-intervalo').textContent = `${dados.periodo.inicio.split('-').reverse().join('/')} a ${dados.periodo.fim.split('-').reverse().join('/')}`;
    if (!campo('dash-inicio').value) campo('dash-inicio').value = dados.periodo.inicio;
    if (!campo('dash-fim').value) campo('dash-fim').value = dados.periodo.fim;
    montarGrafico(dados.vendas,dados.periodo.agrupamento);
    montarSituacao(dados.situacao,Number(r.totalPedidos));
    montarProdutos(dados.produtos);
    montarPedidos(dados.pedidos);
  }
  async function buscar() {
    const requisicao = ++ultimaRequisicao;
    controlador?.abort();
    controlador = new AbortController();
    campo('dash-erro').hidden = true;
    campo('dash-atualizar').disabled = true;
    const params = new URLSearchParams(new FormData(formulario));
    for (const [chave,valor] of [...params.entries()]) if (!valor) params.delete(chave);
    try {
      const resposta = await fetch(`/api/dashboard?${params}`, {
        credentials:'same-origin', headers:{Accept:'application/json'}, signal:controlador.signal,
        cache:'no-store'
      });
      if (resposta.status === 401) { window.location.assign('/login?expired'); return; }
      const corpo = await resposta.json().catch(() => ({}));
      if (!resposta.ok) throw new Error(corpo.mensagem || 'Não foi possível consultar o painel.');
      if (requisicao === ultimaRequisicao) atualizarTela(corpo);
    } catch (erro) {
      if (erro.name === 'AbortError' || requisicao !== ultimaRequisicao) return;
      campo('dash-erro').textContent = `Não foi possível atualizar os dados: ${erro.message}. Confira sua conexão e tente novamente.`;
      campo('dash-erro').hidden = false;
    } finally {
      if (requisicao === ultimaRequisicao) campo('dash-atualizar').disabled = false;
    }
  }
  formulario.addEventListener('submit', event => { event.preventDefault(); buscar(); });
  campo('dash-atualizar').addEventListener('click', buscar);
  campo('dash-limpar').addEventListener('click', () => { formulario.reset(); campo('dash-inicio').value=''; campo('dash-fim').value=''; buscar(); });
  document.addEventListener('visibilitychange', () => { if (!document.hidden) buscar(); });
  window.setInterval(() => { if (!document.hidden) buscar(); }, 60000);
  buscar();
})();
