const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const inteiro = new Intl.NumberFormat('pt-BR');
const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];
const CORES_STATUS = {
    RECEBIDO: 'bg-secondary',
    EM_PREPARACAO: 'bg-warning',
    PRONTO: 'bg-info',
    SAIU_PARA_ENTREGA: 'bg-primary',
    RETIRADO: 'bg-primary',
    FINALIZADO: 'bg-success',
    CANCELADO: 'bg-danger'
};

const INTERVALO_ATUALIZACAO = 60000;
const relogio = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

const graficos = {};
let filtrosAplicados = new URLSearchParams();
let carregando = false;
let ultimaAtualizacao = null;

class SessaoEncerrada extends Error {}

function lerFiltros() {
    const form = document.getElementById('filtros');
    const params = new URLSearchParams();
    for (const [nome, valor] of new FormData(form)) {
        if (valor) {
            params.append(nome, valor);
        }
    }
    return params;
}

function agrupamento() {
    return document.querySelector('input[name="agrupamento"]:checked').value;
}

async function buscar(caminho, extras = {}) {
    const params = new URLSearchParams(filtrosAplicados);
    for (const [nome, valor] of Object.entries(extras)) {
        params.set(nome, valor);
    }
    let resposta;
    try {
        resposta = await fetch(`/api/dashboard/${caminho}?${params}`, {
            headers: { Accept: 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
        });
    } catch (e) {
        throw new Error('Não foi possível conectar ao servidor. Verifique a conexão.');
    }
    if (resposta.status === 401 || (resposta.redirected && resposta.url.includes('/login'))) {
        window.location.href = '/login';
        throw new SessaoEncerrada();
    }
    if (acessoNegado(resposta)) {
        throw new SessaoEncerrada();
    }
    if (!resposta.ok) {
        const corpo = await resposta.json().catch(() => ({}));
        throw new Error(corpo.erro || `Erro ${resposta.status} ao consultar o dashboard.`);
    }
    return resposta.json();
}

async function carregar() {
    if (carregando) {
        return;
    }
    definirCarregando(true);
    try {
        const [indicadores, vendas, status, ranking] = await Promise.all([
            buscar('indicadores'),
            buscar('vendas', { agrupamento: agrupamento() }),
            buscar('status'),
            buscar('produtos-mais-vendidos', { limite: document.getElementById('ranking-limite').value })
        ]);
        renderizarIndicadores(indicadores);
        renderizarVendas(vendas, indicadores.pedidos);
        renderizarStatus(status);
        renderizarRanking(ranking);
        renderizarSemDados(status);
        renderizarPeriodo();
        ultimaAtualizacao = new Date();
        esconderErro();
    } catch (erro) {
        if (!(erro instanceof SessaoEncerrada)) {
            mostrarErro(erro.message);
        }
    } finally {
        definirCarregando(false);
    }
}

function definirCarregando(ativo) {
    carregando = ativo;
    document.getElementById('dashboard').classList.toggle('carregando', ativo);
    document.getElementById('indicador-carregando').classList.toggle('d-none', !ativo);
    document.getElementById('atualizar').disabled = ativo;
    document.getElementById('ultima-atualizacao').textContent = ativo
        ? 'Atualizando...'
        : ultimaAtualizacao ? `Atualizado às ${relogio.format(ultimaAtualizacao)}` : 'Dados não carregados';
}

function mostrarErro(mensagem) {
    document.getElementById('mensagem-erro-texto').textContent = mensagem;
    document.getElementById('mensagem-erro-detalhe').textContent = ultimaAtualizacao
        ? `Exibindo os últimos dados carregados às ${relogio.format(ultimaAtualizacao)}.`
        : '';
    document.getElementById('mensagem-erro').classList.remove('d-none');
}

function esconderErro() {
    document.getElementById('mensagem-erro').classList.add('d-none');
}

function renderizarSemDados(status) {
    const vazio = status.every(s => s.quantidade === 0);
    document.getElementById('sem-dados').classList.toggle('d-none', !vazio);
    document.getElementById('dados').classList.toggle('d-none', vazio);
}

function renderizarIndicadores(dados) {
    document.getElementById('ind-faturamento').textContent = moeda.format(dados.faturamento);
    document.getElementById('ind-pedidos').textContent = inteiro.format(dados.pedidos);
    document.getElementById('ind-ticket').textContent = moeda.format(dados.ticketMedio);
    document.getElementById('ind-cancelamentos').textContent = inteiro.format(dados.cancelamentos);
    document.getElementById('ind-descontos').textContent = moeda.format(dados.descontos);
}

function rotuloPeriodo(periodo, tipo) {
    const [ano, mes, dia] = periodo.split('-');
    if (tipo === 'MES') {
        return `${MESES[Number(mes) - 1]}/${ano}`;
    }
    if (tipo === 'SEMANA') {
        return `Sem. ${dia}/${mes}`;
    }
    return `${dia}/${mes}`;
}

function renderizarVendas(dados, pedidos) {
    const semVendas = pedidos === 0;
    document.getElementById('sem-vendas').classList.toggle('d-none', !semVendas);
    document.getElementById('graficos').classList.toggle('d-none', semVendas);
    const rotulos = dados.pontos.map(p => rotuloPeriodo(p.periodo, dados.agrupamento));
    desenharGrafico('grafico-faturamento', 'line', rotulos, dados.pontos.map(p => p.faturamento), {
        label: 'Faturamento',
        borderColor: '#198754',
        backgroundColor: 'rgba(25, 135, 84, 0.15)',
        fill: true,
        tension: 0.1,
        pointRadius: dados.pontos.length > 45 ? 0 : 3
    }, valor => moeda.format(valor));
    desenharGrafico('grafico-pedidos', 'bar', rotulos, dados.pontos.map(p => p.pedidos), {
        label: 'Pedidos',
        backgroundColor: '#0d6efd'
    }, valor => inteiro.format(valor));
}

function desenharGrafico(id, tipo, rotulos, valores, serie, formatar) {
    if (graficos[id]) {
        graficos[id].data.labels = rotulos;
        graficos[id].data.datasets[0].data = valores;
        graficos[id].data.datasets[0].pointRadius = serie.pointRadius;
        graficos[id].update();
        return;
    }
    graficos[id] = new Chart(document.getElementById(id), {
        type: tipo,
        data: { labels: rotulos, datasets: [{ ...serie, data: valores }] },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: { display: false },
                tooltip: { callbacks: { label: ctx => `${serie.label}: ${formatar(ctx.parsed.y)}` } }
            },
            scales: {
                x: { ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 12 } },
                y: { beginAtZero: true, ticks: { callback: formatar, precision: 0 } }
            }
        }
    });
}

function renderizarStatus(lista) {
    const total = lista.reduce((soma, s) => soma + s.quantidade, 0);
    const ul = document.getElementById('lista-status');
    ul.replaceChildren(...lista.map(s => {
        const percentual = total ? (s.quantidade * 100) / total : 0;
        const li = document.createElement('li');
        li.className = 'list-group-item';

        const linha = document.createElement('div');
        linha.className = 'd-flex justify-content-between small';
        const nome = document.createElement('span');
        nome.textContent = s.descricao;
        const qtd = document.createElement('span');
        qtd.className = 'fw-semibold';
        qtd.textContent = `${inteiro.format(s.quantidade)} (${percentual.toFixed(1).replace('.', ',')}%)`;
        linha.append(nome, qtd);

        const barra = document.createElement('div');
        barra.className = 'progress status-barra mt-1';
        const preenchimento = document.createElement('div');
        preenchimento.className = `progress-bar ${CORES_STATUS[s.status] || 'bg-secondary'}`;
        preenchimento.style.width = `${percentual}%`;
        barra.append(preenchimento);

        li.append(linha, barra);
        return li;
    }));
}

function renderizarRanking(lista) {
    const tbody = document.getElementById('tabela-ranking');
    if (lista.length === 0) {
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.colSpan = 4;
        td.className = 'text-center text-muted py-4';
        td.textContent = 'Nenhum produto vendido no período.';
        tr.append(td);
        tbody.replaceChildren(tr);
        return;
    }
    tbody.replaceChildren(...lista.map((p, i) => {
        const tr = document.createElement('tr');
        [
            [String(i + 1), 'text-center text-muted'],
            [p.nome, ''],
            [inteiro.format(p.quantidade), 'text-end'],
            [moeda.format(p.faturamento), 'text-end']
        ].forEach(([texto, classe]) => {
            const td = document.createElement('td');
            td.className = classe;
            td.textContent = texto;
            tr.append(td);
        });
        return tr;
    }));
}

function dataBr(iso) {
    const [ano, mes, dia] = iso.split('-');
    return `${dia}/${mes}/${ano}`;
}

function hojeIso(deslocamentoDias = 0) {
    const data = new Date();
    data.setDate(data.getDate() + deslocamentoDias);
    return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`;
}

function renderizarPeriodo() {
    const de = filtrosAplicados.get('de') || '';
    const ate = filtrosAplicados.get('ate') || '';
    const fim = ate || hojeIso();
    const inicio = de || (ate ? null : hojeIso(-29));
    const texto = inicio ? `${dataBr(inicio)} a ${dataBr(fim)}` : `29 dias antes de ${dataBr(fim)} até ${dataBr(fim)}`;
    document.getElementById('periodo-consultado').textContent = `Período consultado: ${texto}`;
}

async function carregarOpcoesFiltro() {
    let resposta;
    try {
        resposta = await fetch('/api/produtos', {
            headers: { Accept: 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
        });
    } catch (e) {
        return;
    }
    if (!resposta.ok) {
        return;
    }
    const produtos = await resposta.json();
    const selectProduto = document.getElementById('filtro-produto');
    produtos
        .slice()
        .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
        .forEach(p => selectProduto.append(new Option(p.nome, p.id)));

    const selectTipo = document.getElementById('filtro-tipo');
    [...new Set(produtos.map(p => p.tipo).filter(Boolean))]
        .sort((a, b) => a.localeCompare(b, 'pt-BR'))
        .forEach(tipo => selectTipo.append(new Option(tipo, tipo)));
}

function aplicarFiltros() {
    filtrosAplicados = lerFiltros();
    carregar();
}

function iniciar() {
    const form = document.getElementById('filtros');
    form.addEventListener('submit', evento => {
        evento.preventDefault();
        aplicarFiltros();
    });
    document.getElementById('limpar-filtros').addEventListener('click', () => {
        form.reset();
        aplicarFiltros();
    });
    document.getElementById('atualizar').addEventListener('click', carregar);
    document.querySelectorAll('input[name="agrupamento"]').forEach(radio => radio.addEventListener('change', carregar));
    document.getElementById('ranking-limite').addEventListener('change', carregar);

    setInterval(() => {
        if (!document.hidden) {
            carregar();
        }
    }, INTERVALO_ATUALIZACAO);
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden && ultimaAtualizacao && Date.now() - ultimaAtualizacao.getTime() > INTERVALO_ATUALIZACAO) {
            carregar();
        }
    });

    carregarOpcoesFiltro();
    aplicarFiltros();
}

iniciar();
