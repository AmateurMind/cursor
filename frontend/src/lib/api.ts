export type User = { id: number; email: string; name: string }
export type Expense = { id?: number; title: string; amount: number; category: string; expenseDate: string; notes?: string|null; userId?: number|null }
export type Category = { id: number; name: string; type?: string; color?: string }

const API_BASE = (import.meta as any).env?.VITE_API_BASE || ''

async function json<T>(res: Response): Promise<T> { if(!res.ok) throw new Error(await res.text()); return res.json() }

export const api = {
  async login(email: string, password: string){
    const res = await fetch(`${API_BASE}/api/auth/login`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({email, password}) })
    return json<User>(res)
  },
  async register(email: string, name: string, password: string){
    const res = await fetch(`${API_BASE}/api/auth/register`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({email, name, password}) })
    return json<{id:number}>(res)
  },
  async listExpenses(userId?: number){
    const q = userId ? `?userId=${encodeURIComponent(userId)}` : ''
    const res = await fetch(`${API_BASE}/api/expenses${q}`)
    return json<Expense[]>(res)
  },
  async saveExpense(expense: Expense){
    const method = expense.id ? 'PUT' : 'POST'
    const url = expense.id ? `${API_BASE}/api/expenses/${expense.id}` : `${API_BASE}/api/expenses`
    const e = { ...expense }; if(e.id) delete (e as any).id
    const res = await fetch(url, { method, headers:{'Content-Type':'application/json'}, body: JSON.stringify(e) })
    if(!res.ok) throw new Error(await res.text())
    if(method === 'POST') return res.json() as Promise<Expense>
    return undefined as unknown as Expense
  },
  async deleteExpense(id: number){
    const res = await fetch(`${API_BASE}/api/expenses/${id}`, { method:'DELETE' })
    if(!res.ok) throw new Error('delete failed')
  },
  async smartCategory(title: string, notes: string){
    const res = await fetch(`${API_BASE}/api/ai/suggest-category`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({title, notes}) })
    return json<{category:string, source:string}>(res)
  },
  async listCategories(){
    const res = await fetch(`${API_BASE}/api/categories`)
    return json<Category[]>(res)
  }
}
