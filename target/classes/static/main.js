const api = '/api/expenses';

async function fetchExpenses() {
  const res = await fetch(api);
  const data = await res.json();
  renderTable(data);
}

function renderTable(expenses) {
  const tbody = document.querySelector('#expenses-table tbody');
  tbody.innerHTML = '';
  expenses.forEach(e => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${e.title}</td>
      <td>${e.amount}</td>
      <td>${e.category}</td>
      <td>${e.expenseDate}</td>
      <td>${e.notes ?? ''}</td>
      <td>
        <a href="#" class="btn edit" data-id="${e.id}">Edit</a>
        <a href="#" class="btn delete" data-id="${e.id}">Delete</a>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function getFormData() {
  return {
    id: document.getElementById('id').value || null,
    title: document.getElementById('title').value,
    amount: parseFloat(document.getElementById('amount').value),
    category: document.getElementById('category').value,
    expenseDate: document.getElementById('expenseDate').value,
    notes: document.getElementById('notes').value || null
  };
}

function setFormData(e) {
  document.getElementById('id').value = e.id || '';
  document.getElementById('title').value = e.title || '';
  document.getElementById('amount').value = e.amount || '';
  document.getElementById('category').value = e.category || '';
  document.getElementById('expenseDate').value = e.expenseDate || '';
  document.getElementById('notes').value = e.notes || '';
}

async function saveExpense(evt) {
  evt.preventDefault();
  const data = getFormData();
  const isUpdate = !!data.id;
  const method = isUpdate ? 'PUT' : 'POST';
  const url = isUpdate ? `${api}/${data.id}` : api;
  if (isUpdate) delete data.id;
  await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) });
  await fetchExpenses();
  setFormData({});
}

async function onTableClick(evt) {
  if (evt.target.classList.contains('edit')) {
    evt.preventDefault();
    const id = evt.target.dataset.id;
    const res = await fetch(`${api}/${id}`);
    const e = await res.json();
    setFormData(e);
  }
  if (evt.target.classList.contains('delete')) {
    evt.preventDefault();
    const id = evt.target.dataset.id;
    await fetch(`${api}/${id}`, { method: 'DELETE' });
    await fetchExpenses();
  }
}

function resetForm() { setFormData({}); }

document.getElementById('expense-form').addEventListener('submit', saveExpense);
document.getElementById('reset').addEventListener('click', resetForm);
document.querySelector('#expenses-table tbody').addEventListener('click', onTableClick);

fetchExpenses();


