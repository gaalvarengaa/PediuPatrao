// Cabecalho CSRF para chamadas fetch
function csrfHeaders(headers = {}) {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header) {
        headers[header.content] = token.content;
    }
    return headers;
}

// Alternar sidebar em telas pequenas
document.querySelector('.toggle-btn').addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('active');
});

// Alternar submenu
function toggleSubmenu(event, element) {
    event.preventDefault();
    element.parentElement.classList.toggle('open');
}

// Resposta 403 do backend: leva o usuário para a página de acesso negado
function acessoNegado(response) {
    if (response.status !== 403) {
        return false;
    }
    window.location.href = '/acesso-negado';
    return true;
}
