import { useEffect, useMemo, useState } from 'react'
import { api, type Expense, type User } from '../lib/api'
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js'
import { Pie } from 'react-chartjs-2'

ChartJS.register(ArcElement, Tooltip, Legend)

const DEMO = { email: 'demo@demo.com', name: 'Demo', password: 'demo123' }

function StatCard({ title, value }: { title: string; value: string }) {
  return (
    <div className="card">
      <div className="text-sm dark:text-neutral-400 light:text-gray-600 mb-1">{title}</div>
      <div className="text-2xl font-bold">{value}</div>
    </div>
  )
}

export default function App(){
  const [user, setUser] = useState<User | null>(() => {
    try{ return JSON.parse(localStorage.getItem('currentUser')||'') }catch{ return null }
  })
  const [list, setList] = useState<Expense[]>([])
  const [categories, setCategories] = useState<string[]>([])
  const [form, setForm] = useState<Expense>({ title:'', amount:0, category:'', expenseDate: new Date().toISOString().slice(0,10), notes:'', userId: user?.id||null })
  const [amountText, setAmountText] = useState<string>('0')
  const [customCategory, setCustomCategory] = useState(false)
  const [loading, setLoading] = useState(false)
  const [theme, setTheme] = useState<'light'|'dark'>(()=>(localStorage.getItem('theme') as 'light'|'dark')||'dark')

  useEffect(()=>{ localStorage.setItem('currentUser', user? JSON.stringify(user):''); setForm(f=>({...f, userId: user?.id||null})); refresh() }, [user?.id])
  useEffect(()=>{ document.body.className = theme; localStorage.setItem('theme', theme) }, [theme])

  async function refresh(){
    if(!user){ setList([]); return }
    setLoading(true)
    try{ 
      const [expenses, cats] = await Promise.all([api.listExpenses(user.id), api.listCategories()])
      setList(expenses)
      setCategories(cats.map(c=>c.name))
    } finally{ setLoading(false) }
  }

  const stats = useMemo(() => {
    const total = list.reduce((s, e) => s + (e.amount || 0), 0)
    const byCategory: Record<string, number> = {}
    list.forEach(e => { byCategory[e.category] = (byCategory[e.category] || 0) + (e.amount || 0) })
    return { total, count: list.length, byCategory }
  }, [list])

  async function login(email: string, password: string){
    try{ const u = await api.login(email, password); setUser(u) } catch{ alert('Login failed. Try Register or Use demo.')} }
  async function registerDemo(){
    try{ await api.register(DEMO.email, DEMO.name, DEMO.password) }catch{ /* maybe exists */ }
    await login(DEMO.email, DEMO.password)
  }
  async function register(email:string, name:string, password:string){
    try{ await api.register(email, name, password); await login(email, password) } catch { alert('Register failed') }
  }
  function logout(){ setUser(null) }

  async function save(){
    if(!user){ alert('Login first'); return }
    const amount = parseFloat(amountText || '0') || 0
    const payload: Expense = { ...form, amount, userId: user.id }
    await api.saveExpense(payload)
    setForm({ title:'', amount:0, category:'', expenseDate: new Date().toISOString().slice(0,10), notes:'', userId: user.id })
    setAmountText('0')
    await refresh()
  }
  async function del(id: number){ if(confirm('Delete?')){ await api.deleteExpense(id); await refresh() } }
  async function smart(){
    const s = await api.smartCategory(form.title||'', form.notes||'')
    setForm(f=>({ ...f, category: s.category }))
    setCustomCategory(false)
  }

  return (
    <div>
      <div className="border-b transition-colors dark:bg-black dark:border-neutral-800 light:bg-white light:border-gray-200">
        <div className="container py-6 flex items-center justify-between">
          <h1 className="text-3xl font-bold">Expense Tracker</h1>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3">
              <span className="text-sm font-medium" style={{color: theme==='dark'?'#9ca3af':'#111827'}}>Light</span>
              <label className="flex items-center cursor-pointer">
                <div className="relative">
                  <input type="checkbox" checked={theme==='dark'} onChange={()=>setTheme(theme==='dark'?'light':'dark')} className="hidden" />
                  <div className="block w-16 h-8 rounded-full transition-colors" style={{backgroundColor: '#dc2626'}}></div>
                  <div className="dot absolute left-1 top-1 bg-white w-6 h-6 rounded-full transition shadow-md"></div>
                </div>
              </label>
              <span className="text-sm font-medium" style={{color: theme==='dark'?'#ffffff':'#9ca3af'}}>Dark</span>
            </div>
            {user ? (
              <>
                <span className="text-sm dark:text-neutral-400 light:text-gray-600 hidden sm:inline">{user.name}</span>
                <button className="btn bg-red-600 text-white hover:bg-red-700" onClick={logout}>Logout</button>
              </>
            ) : (
              <div className="flex items-center gap-3">
                <span className="hidden sm:block text-sm dark:text-neutral-400 light:text-gray-600">Demo: {DEMO.email}</span>
                <button className="btn bg-red-600 text-white hover:bg-red-700" onClick={registerDemo}>Use demo</button>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="container py-6 space-y-6">
        {!user && (
          <div className="grid md:grid-cols-2 gap-6">
            <div className="card"><Login onLogin={login} /></div>
            <div className="card"><Register onRegister={register} /></div>
          </div>
        )}

        {user && (
          <>
            {/* Stats Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <StatCard title="Total Expenses" value={`₹${stats.total.toFixed(2)}`} />
              <StatCard title="Transactions" value={String(stats.count)} />
              <StatCard title="Categories" value={String(Object.keys(stats.byCategory).length)} />
            </div>

            {/* Main Layout: Form + Category Summary */}
            <div className="grid lg:grid-cols-3 gap-6">
              {/* Add/Edit Form */}
              <div className="lg:col-span-2 card space-y-4">
                <h2 className="text-xl font-bold mb-4">Add / Edit Expense</h2>
                <div className="grid sm:grid-cols-2 gap-3">
                  <Field label="Title"><input className="input" value={form.title} onChange={e=>setForm({...form, title:e.target.value})} placeholder="e.g. Coffee"/></Field>
                  <Field label="Amount"><input inputMode="decimal" type="text" className="input" value={amountText} onChange={e=>setAmountText(e.target.value.replace(/[^0-9.]/g, ''))} placeholder="0.00"/></Field>
                  <Field label="Category">
                    {customCategory ? (
                      <div className="flex gap-2">
                        <input className="input" value={form.category} onChange={e=>setForm({...form, category:e.target.value})} placeholder="New category"/>
                        <button className="btn bg-neutral-700 text-white hover:bg-neutral-600" onClick={()=>setCustomCategory(false)}>Cancel</button>
                      </div>
                    ) : (
                      <div className="flex gap-2">
                        <select className="input flex-1" value={form.category} onChange={e=>setForm({...form, category:e.target.value})}>
                          <option value="">Select category</option>
                          {categories.map(c=><option key={c} value={c}>{c}</option>)}
                        </select>
                        <button className="btn bg-neutral-700 text-white hover:bg-neutral-600" onClick={()=>setCustomCategory(true)}>+</button>
                      </div>
                    )}
                  </Field>
                  <Field label="Date"><input type="date" className="input" value={form.expenseDate} onChange={e=>setForm({...form, expenseDate:e.target.value})}/></Field>
                </div>
                <Field label="Notes"><input className="input" value={form.notes||''} onChange={e=>setForm({...form, notes:e.target.value})} placeholder="Optional"/></Field>
                <div className="flex gap-3 flex-wrap">
                  <button className="btn bg-red-600 text-white hover:bg-red-700" onClick={save}>Save</button>
                  <button className="btn bg-neutral-700 text-white hover:bg-neutral-600" onClick={()=>{ setForm({ title:'', amount:0, category:'', expenseDate:new Date().toISOString().slice(0,10), notes:'', userId:user.id }); setAmountText('0'); }}>Reset</button>
                  <button className="btn bg-neutral-700 text-white hover:bg-neutral-600" onClick={smart}>Smart Categorize</button>
                </div>
              </div>

              {/* Category Breakdown */}
              <div className="card">
                <h2 className="text-lg font-bold mb-4">Spending by Category</h2>
                {Object.keys(stats.byCategory).length > 0 ? (
                  <div className="space-y-4">
                    <div className="max-w-xs mx-auto">
                      <Pie data={{
                        labels: Object.keys(stats.byCategory),
                        datasets: [{
                          data: Object.values(stats.byCategory),
                          backgroundColor: ['#ef4444', '#f59e0b', '#10b981', '#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316'],
                          borderWidth: 0
                        }]
                      }} options={{
                        plugins: { legend: { display: false } },
                        maintainAspectRatio: true
                      }} />
                    </div>
                    <div className="space-y-2">
                      {Object.entries(stats.byCategory).sort((a,b)=>b[1]-a[1]).map(([cat, amt]) => (
                        <div key={cat} className="flex items-center justify-between text-sm">
                          <span className="capitalize dark:text-neutral-300 light:text-gray-600">{cat}</span>
                          <span className="font-semibold">₹{amt.toFixed(2)}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : <div className="dark:text-neutral-400 light:text-gray-500 text-sm">No data yet</div>}
              </div>
            </div>

            {/* Transaction History */}
            <div className="card">
              <h2 className="text-xl font-bold mb-4">Transaction History</h2>
              {loading ? <div className="dark:text-neutral-400 light:text-gray-500">Loading...</div> : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead className="text-left dark:text-neutral-400 light:text-gray-600 border-b dark:border-neutral-700 light:border-gray-200">
                      <tr>
                        <th className="pb-3 pr-4 font-medium">Transaction</th>
                        <th className="pb-3 pr-4 font-medium">Date</th>
                        <th className="pb-3 pr-4 text-right font-medium">Amount</th>
                        <th className="pb-3 text-right font-medium">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y dark:divide-neutral-700 light:divide-gray-200">
                      {list.map(e=> (
                        <tr key={e.id} className="dark:hover:bg-neutral-750 light:hover:bg-gray-50">
                          <td className="py-3 pr-4">
                            <div className="font-medium">{e.title}</div>
                            <div className="text-xs dark:text-neutral-400 light:text-gray-500 capitalize">{e.category}{e.notes ? ` • ${e.notes}` : ''}</div>
                          </td>
                          <td className="py-3 pr-4 dark:text-neutral-300 light:text-gray-600">{e.expenseDate}</td>
                          <td className="py-3 pr-4 text-right font-semibold">₹{e.amount}</td>
                          <td className="py-3 text-right space-x-3">
                            <button className="dark:text-white dark:hover:text-neutral-300 light:text-gray-700 light:hover:text-gray-900" onClick={()=>{ setForm({ ...(e as any), expenseDate: e.expenseDate }); setAmountText(String(e.amount ?? '0')); }}>Edit</button>
                            <button className="text-red-500 hover:text-red-400" onClick={()=>del(e.id!)}>Delete</button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  )
}

function Field({label, children}:{label:string, children:React.ReactNode}){
  return (
    <label className="block">
      <div className="label mb-1">{label}</div>
      {children}
    </label>
  )
}

function Login({ onLogin }:{ onLogin:(email:string,pwd:string)=>void }){
  const [email, setEmail] = useState(()=>localStorage.getItem('rememberedEmail')||'')
  const [pwd, setPwd] = useState(()=>localStorage.getItem('rememberedPassword')||'')
  const [remember, setRemember] = useState(()=>!!localStorage.getItem('rememberedEmail'))

  function handleLogin(){
    if(remember){ localStorage.setItem('rememberedEmail', email); localStorage.setItem('rememberedPassword', pwd); }
    else { localStorage.removeItem('rememberedEmail'); localStorage.removeItem('rememberedPassword'); }
    onLogin(email, pwd)
  }

  return (
    <div className="space-y-3">
      <h2 className="text-xl font-semibold">Login</h2>
      <Field label="Email"><input className="input" value={email} onChange={e=>setEmail(e.target.value)} placeholder="demo@demo.com"/></Field>
      <Field label="Password"><input type="password" className="input" value={pwd} onChange={e=>setPwd(e.target.value)} placeholder="demo123"/></Field>
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" checked={remember} onChange={e=>setRemember(e.target.checked)} />
        <span>Remember me</span>
      </label>
      <button className="btn bg-red-600 text-white hover:bg-red-700 w-full" onClick={handleLogin}>Login</button>
      <p className="text-sm text-gray-600">Or click "Use demo" at top to auto-create/login demo account.</p>
    </div>
  )
}

function Register({ onRegister }:{ onRegister:(email:string,name:string,pwd:string)=>void }){
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [pwd, setPwd] = useState('')
  return (
    <div className="space-y-3">
      <h2 className="text-xl font-semibold">Register</h2>
      <Field label="Name"><input className="input" value={name} onChange={e=>setName(e.target.value)} placeholder="Your name"/></Field>
      <Field label="Email"><input className="input" value={email} onChange={e=>setEmail(e.target.value)} placeholder="you@example.com"/></Field>
      <Field label="Password"><input type="password" className="input" value={pwd} onChange={e=>setPwd(e.target.value)} placeholder="min 6 chars"/></Field>
      <button className="btn bg-red-600 text-white hover:bg-red-700 w-full" onClick={()=>onRegister(email, name, pwd)}>Create account</button>
    </div>
  )
}
