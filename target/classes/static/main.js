const api = '/api/expenses';
const aiApi = '/api/ai/suggest-category';

async function fetchExpenses() {
  const res = await fetch(api);
  const data = await res.json();
  renderTable(data);
  renderCharts(data);
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
  const expenseDate = document.getElementById('expenseDate').value;
  return {
    id: document.getElementById('id').value || null,
    title: document.getElementById('title').value,
    amount: parseFloat(document.getElementById('amount').value),
    category: document.getElementById('category').value,
    expenseDate: expenseDate || new Date().toISOString().split('T')[0],
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

document.getElementById('smart-categorize').addEventListener('click', async () => {
  const data = getFormData();
  if (!data.title && !data.notes) {
    alert('Enter a title or notes first.');
    return;
  }
  try {
    const res = await fetch(aiApi, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title: data.title, notes: data.notes || '' })
    });
    const json = await res.json();
    if (json && json.category) {
      document.getElementById('category').value = json.category;
    }
  } catch (e) {
    // ignore and keep current category
  }
});

let categoryChart, timeChart;
function renderCharts(expenses) {
  // aggregate by category
  const byCategory = {};
  // aggregate by month (YYYY-MM)
  const byMonth = {};
  expenses.forEach(e => {
    const cat = e.category || 'Uncategorized';
    byCategory[cat] = (byCategory[cat] || 0) + (Number(e.amount) || 0);
    const month = (e.expenseDate || '').slice(0, 7);
    if (month) byMonth[month] = (byMonth[month] || 0) + (Number(e.amount) || 0);
  });

  // category doughnut
  const catLabels = Object.keys(byCategory);
  const catValues = Object.values(byCategory);
  if (categoryChart) categoryChart.destroy();
  const catCtx = document.getElementById('categoryChart').getContext('2d');
  categoryChart = new Chart(catCtx, {
    type: 'doughnut',
    data: { labels: catLabels, datasets: [{ data: catValues }] },
    options: { plugins: { legend: { position: 'bottom' } } }
  });

  // time line chart
  const months = Object.keys(byMonth).sort();
  const monthValues = months.map(m => byMonth[m]);
  if (timeChart) timeChart.destroy();
  const timeCtx = document.getElementById('timeChart').getContext('2d');
  timeChart = new Chart(timeCtx, {
    type: 'line',
    data: { labels: months, datasets: [{ label: 'Total', data: monthValues }] },
    options: { scales: { y: { beginAtZero: true } } }
  });
}

fetchExpenses();


