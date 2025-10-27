const api = '/api/expenses';
const aiApi = '/api/ai/suggest-category';
const authApi = '/api/auth';

async function fetchExpenses() {
  const user = getCurrentUser();
  const url = user ? `${api}?userId=${encodeURIComponent(user.id)}` : api;
  const res = await fetch(url);
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
  const user = getCurrentUser();
  return {
    id: document.getElementById('id').value || null,
    title: document.getElementById('title').value,
    amount: parseFloat(document.getElementById('amount').value),
    category: document.getElementById('category').value,
    expenseDate: expenseDate || new Date().toISOString().split('T')[0],
    notes: document.getElementById('notes').value || null,
    userId: user ? user.id : null
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
initAuthUI();
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

// --- Auth (simple) ---
function getCurrentUser(){
  try { return JSON.parse(localStorage.getItem('currentUser')||''); } catch(e){ return null; }
}
function setCurrentUser(u){ if(u){ localStorage.setItem('currentUser', JSON.stringify(u)); } else { localStorage.removeItem('currentUser'); } }

function initAuthUI(){
  const bar = document.createElement('div');
  bar.id = 'auth-bar';
  bar.style.display = 'flex'; bar.style.gap='8px'; bar.style.alignItems='center'; bar.style.margin='8px 0';
  bar.innerHTML = `
    <span id="auth-status"></span>
    <input id="auth-email" placeholder="email" style="max-width:200px;">
    <input id="auth-name" placeholder="name (register)" style="max-width:160px;">
    <input id="auth-password" placeholder="password" type="password" style="max-width:140px;">
    <button id="btn-login">Login</button>
    <button id="btn-register">Register</button>
    <button id="btn-logout" style="display:none;">Logout</button>
  `;
  document.body.prepend(bar);
  const user = getCurrentUser();
  updateAuthStatus(user);
  document.getElementById('btn-login').onclick = login;
  document.getElementById('btn-register').onclick = register;
  document.getElementById('btn-logout').onclick = ()=>{ setCurrentUser(null); updateAuthStatus(null); fetchExpenses(); };
}

function updateAuthStatus(user){
  const status = document.getElementById('auth-status');
  const email = document.getElementById('auth-email');
  const name = document.getElementById('auth-name');
  const pwd = document.getElementById('auth-password');
  const btnL = document.getElementById('btn-login');
  const btnR = document.getElementById('btn-register');
  const btnO = document.getElementById('btn-logout');
  if(user){
    status.textContent = `Logged in as ${user.name} (${user.email})`;
    email.style.display = name.style.display = pwd.style.display = btnL.style.display = btnR.style.display = 'none';
    btnO.style.display = '';
  } else {
    status.textContent = 'Not logged in';
    email.style.display = name.style.display = pwd.style.display = btnL.style.display = btnR.style.display = '';
    btnO.style.display = 'none';
  }
}

async function login(){
  const email = document.getElementById('auth-email').value.trim();
  const password = document.getElementById('auth-password').value;
  if(!email || !password){ alert('email and password required'); return; }
  try{
    const res = await fetch(authApi + '/login', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({email, password}) });
    if(!res.ok){ const j = await res.json(); alert(j.error||'login failed'); return; }
    const u = await res.json(); setCurrentUser(u); updateAuthStatus(u); fetchExpenses();
  }catch(e){ alert('login error'); }
}

async function register(){
  const email = document.getElementById('auth-email').value.trim();
  const name = document.getElementById('auth-name').value.trim();
  const password = document.getElementById('auth-password').value;
  if(!email || !name || !password){ alert('email, name, password required'); return; }
  try{
    const res = await fetch(authApi + '/register', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({email, name, password}) });
    if(!res.ok){ const j = await res.json(); alert(j.error||'register failed'); return; }
    const u = await fetch(authApi + '/login', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({email, password}) }).then(r=>r.json());
    setCurrentUser(u); updateAuthStatus(u); fetchExpenses();
  }catch(e){ alert('register error'); }
}


