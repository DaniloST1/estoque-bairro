'use strict';
const here = location.pathname;
const mode = new URLSearchParams(location.search).get('motivo');
for (const link of document.querySelectorAll('[data-nav]')) {
  const target = link.dataset.nav;
  const match = target === '/' ? here === '/' : target === 'COMPRA' || target === 'VENDA' ? here === '/movimentos/novo' && (mode || 'COMPRA') === target : here.startsWith(target);
  if (match) { link.classList.add('active'); link.setAttribute('aria-current','page'); }
}
const reason = document.querySelector('#motivo');
function updateFields() {
  for (const section of document.querySelectorAll('[data-mode]')) {
    section.hidden = section.dataset.mode !== reason.value;
    const input = section.querySelector('input');
    if (input) { input.required = !section.hidden; input.disabled = section.hidden; }
  }
  const justification = document.querySelector('#justificativa');
  if (justification) justification.required = reason.value === 'PERDA';
}
if (reason) { reason.addEventListener('change',updateFields); updateFields(); }

// ---------- Modal de entrada/saída ----------
const dialog = document.getElementById('movement-dialog');
if (dialog) {
  const form = dialog.querySelector('form');
  const select = form.querySelector('#m-produto');
  const search = form.querySelector('#m-busca');
  const qty = form.querySelector('#m-quantidade');
  const reasonSelect = form.querySelector('#m-motivo');
  const cost = form.querySelector('#m-custo');
  const price = form.querySelector('#m-preco');
  const ref = form.querySelector('#m-custo-ref');
  const note = form.querySelector('#m-justificativa');
  const noteLabel = form.querySelector('#m-justificativa-label');
  const info = form.querySelector('#m-info');
  const total = form.querySelector('#m-total');
  const totalLabel = form.querySelector('#m-total-label');
  const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
  let opener = null;
  let costEdited = false;

  const chosen = () => (select.value ? select.selectedOptions[0] : null);

  function sync() {
    const m = reasonSelect.value;
    const opt = chosen();
    for (const section of form.querySelectorAll('[data-dialog-mode]')) {
      const on = section.dataset.dialogMode === m;
      section.hidden = !on;
      for (const input of section.querySelectorAll('input')) {
        input.disabled = !on;
        if (input.id === 'm-custo') input.required = on;
      }
    }
    note.required = m === 'PERDA';
    noteLabel.textContent = m === 'PERDA' ? 'Justificativa da perda *' : 'Observação (opcional)';
    note.placeholder = m === 'PERDA' ? 'Ex.: embalagem danificada, produto vencido' : '';
    totalLabel.textContent = m === 'PERDA' ? 'Perda estimada' : m === 'VENDA' ? 'Total da venda' : 'Total da compra';

    qty.setCustomValidity('');
    info.classList.remove('is-error');
    if (opt) {
      const saldo = Number(opt.dataset.saldo);
      price.value = opt.dataset.preco;
      ref.value = opt.dataset.custo;
      if (!costEdited) cost.value = opt.dataset.custo;
      info.innerHTML = '';
      const strong = document.createElement('strong');
      strong.textContent = `${saldo} UN`;
      info.append('Saldo atual: ', strong, `, mínimo de ${opt.dataset.minimo} UN`);
      if (m !== 'COMPRA') {
        qty.max = saldo;
        if (Number(qty.value) > saldo) {
          const msg = `Saldo insuficiente. Disponível: ${saldo} UN.`;
          qty.setCustomValidity(msg);
          info.textContent = msg;
          info.classList.add('is-error');
        }
      } else {
        qty.removeAttribute('max');
      }
    } else {
      info.textContent = 'Escolha um produto para ver o saldo.';
    }
    const unit = parseFloat(m === 'COMPRA' ? cost.value : m === 'VENDA' ? price.value : ref.value);
    const q = parseInt(qty.value, 10);
    total.textContent = brl.format(opt && unit > 0 && q > 0 ? unit * q : 0);
  }

  search.addEventListener('input', () => {
    const term = search.value.trim().toLocaleLowerCase('pt-BR');
    const visible = [];
    for (const o of select.options) {
      if (!o.value) continue;
      const show = !term || o.textContent.toLocaleLowerCase('pt-BR').includes(term);
      o.hidden = !show;
      if (show && !o.disabled) visible.push(o);
    }
    if (visible.length === 1 && select.value !== visible[0].value) {
      select.value = visible[0].value;
      costEdited = false;
      sync();
    }
  });
  search.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { e.preventDefault(); qty.focus(); }
  });
  select.addEventListener('change', () => { costEdited = false; sync(); });
  cost.addEventListener('input', () => { costEdited = true; sync(); });
  for (const el of [qty, reasonSelect]) {
    el.addEventListener('input', sync);
    el.addEventListener('change', sync);
  }

  function open(productId, button) {
    opener = button;
    if (productId) { select.value = productId; costEdited = false; }
    sync();
    dialog.showModal();
    (productId ? qty : search).focus();
    if (productId) qty.select();
  }
  for (const b of document.querySelectorAll('[data-open-dialog]')) {
    b.addEventListener('click', () => open(b.dataset.product, b));
  }
  for (const b of dialog.querySelectorAll('[data-close-dialog]')) {
    b.addEventListener('click', () => dialog.close());
  }
  dialog.addEventListener('click', (e) => { if (e.target === dialog) dialog.close(); });
  dialog.addEventListener('close', () => { if (opener) opener.focus(); });
  form.addEventListener('submit', (e) => {
    sync();
    if (!form.reportValidity()) e.preventDefault();
  });
  if (new URLSearchParams(location.search).has('abrir')) {
    history.replaceState(null, '', location.pathname + '?motivo=' + (reasonSelect.value === 'COMPRA' ? 'COMPRA' : 'VENDA'));
    open(null, null);
  }
  sync();
}
