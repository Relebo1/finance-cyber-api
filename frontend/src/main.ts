import { bootstrapApplication } from '@angular/platform-browser';
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, provideHttpClient } from '@angular/common/http';

type Category = { id: number; name: string; description?: string };
type Account = { id: number; name: string; currency: string; userId: number };
type Transaction = { id: number; description: string; amount: number; type: string; categoryName: string; transactionDate: string };

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <main class="login-page" *ngIf="!signedIn">
      <section class="login-card">
        <p class="eyebrow">PERSONAL FINANCE</p>
        <h1>Welcome back</h1>
        <p class="muted">Sign in to open your expense tracker.</p>
        <form (ngSubmit)="submitAccess()">
          <label *ngIf="registering">Display name<input name="displayName" [(ngModel)]="displayName" required></label>
          <label>Username<input name="username" [(ngModel)]="username" autocomplete="username" required></label>
          <label *ngIf="registering">Email (optional)<input name="email" type="email" [(ngModel)]="email" autocomplete="email"></label>
          <label>Password<input name="password" type="password" [(ngModel)]="password" autocomplete="current-password" required></label>
          <button>{{registering ? 'Create account' : 'Sign in'}}</button>
        </form>
        <button class="text-button" type="button" (click)="registering = !registering">{{registering ? 'Back to sign in' : 'Create a new user'}}</button>
        <p class="login-error" *ngIf="message">{{message}}</p>
        <p class="login-note">This practice version uses weak credentials and does not protect later API requests with a session.</p>
      </section>
    </main>
    <main class="page" *ngIf="signedIn">
      <header><p class="eyebrow">PERSONAL FINANCE</p><h1>Expense tracker</h1><p class="muted">Record and review transactions</p></header>
      <section class="panel">
        <div class="section-heading"><h2>Categories</h2></div>
        <form class="account-form" (ngSubmit)="createCategory()">
          <label>Category name<input name="categoryName" [(ngModel)]="categoryName" required></label>
          <label>Description<input name="categoryDescription" [(ngModel)]="categoryDescription"></label>
          <button>Add category</button>
        </form>
      </section>
      <section class="panel">
        <h2>Accounts</h2>
        <form class="account-form" (ngSubmit)="createAccount()">
          <label>Account name<input name="accountName" [(ngModel)]="accountName" placeholder="Everyday account" required></label>
          <label>Currency<input name="currency" [(ngModel)]="currency" maxlength="3" required></label>
          <button [disabled]="!userId">Add account</button>
        </form>
        <p class="muted" *ngIf="!accounts.length">Add a financial account before recording transactions.</p>
      </section>
      <section class="panel">
        <h2>New transaction</h2>
        <form (ngSubmit)="createTransaction()">
          <label>Description<input name="description" [(ngModel)]="form.description" required maxlength="255"></label>
          <div class="row">
            <label>Amount<input name="amount" type="number" min="0.01" step="0.01" [(ngModel)]="form.amount" required></label>
            <label>Type<select name="type" [(ngModel)]="form.type"><option>EXPENSE</option><option>INCOME</option></select></label>
          </div>
          <div class="row">
            <label>Category<select name="categoryId" [(ngModel)]="form.categoryId" required><option [ngValue]="null">Choose category</option><option *ngFor="let c of categories" [ngValue]="c.id">{{c.name}}</option></select></label>
            <label>Account<select name="accountId" [(ngModel)]="form.accountId" required><option [ngValue]="null">Choose account</option><option *ngFor="let a of accounts" [ngValue]="a.id">{{a.name}} ({{a.currency}})</option></select></label>
          </div>
          <div class="row">
            <label>Date<input name="transactionDate" type="date" [(ngModel)]="form.transactionDate" required></label>
          </div>
          <button [disabled]="!categories.length || !accounts.length">Save transaction</button>
        </form>
        <p class="notice" *ngIf="!categories.length">Create a category first with <code>POST /api/category</code>.</p>
        <p class="message" aria-live="polite">{{message}}</p>
      </section>
      <section class="panel">
        <div class="section-heading"><h2>Transactions</h2><button class="secondary" (click)="loadTransactions()">Refresh</button></div>
        <div class="summary" *ngIf="summary"><span>Income <b>{{summary.totalIncome | currency}}</b></span><span>Expenses <b>{{summary.totalExpenses | currency}}</b></span><span>Balance <b>{{summary.balance | currency}}</b></span></div>
        <p *ngIf="!transactions.length" class="muted">No transactions found.</p>
        <article class="item" *ngFor="let t of transactions"><div><b>{{t.description}}</b><small>{{t.categoryName}} · {{t.transactionDate}}</small></div><div class="amount" [class.income]="t.type === 'INCOME'">{{t.type === 'EXPENSE' ? '−' : '+'}}{{t.amount | currency}}</div></article>
      </section>
    </main>
  `
})
class AppComponent {
  signedIn = false;
  registering = false;
  displayName = '';
  username = '';
  email = '';
  password = '';
  userId: number | null = null;
  accounts: Account[] = [];
  accountName = '';
  currency = 'USD';
  categoryName = '';
  categoryDescription = '';
  categories: Category[] = [];
  transactions: Transaction[] = [];
  summary: { totalIncome: number; totalExpenses: number; balance: number } | null = null;
  message = '';
  form = { description: '', amount: 0, type: 'EXPENSE', categoryId: null as number | null, accountId: null as number | null, transactionDate: new Date().toISOString().slice(0, 10) };

  constructor(private http: HttpClient) {
  }

  submitAccess() {
    if (this.registering) {
      this.http.post('/api/users/register', { username: this.username, email: this.email || null, password: this.password, displayName: this.displayName }).subscribe({
        next: () => { this.registering = false; this.signIn(); },
        error: () => this.message = 'Could not create user. The email may already be registered.'
      });
    } else this.signIn();
  }

  signIn() {
    this.http.post<{user: {id: number}}>('/api/auth/login', { username: this.username, password: this.password }).subscribe({
      next: result => {
        this.userId = result.user.id; this.signedIn = true;
        this.http.get<Account[]>('/api/accounts', { params: { userId: String(this.userId) } }).subscribe({ next: a => this.accounts = a });
        this.http.get<{content: Category[]}>('/api/category').subscribe({ next: r => this.categories = r.content ?? [] });
        this.loadTransactions();
      },
      error: () => this.message = 'Sign-in failed. Create a user first or check your credentials.'
    });
  }

  createAccount() {
    if (this.userId === null) return;
    this.http.post<Account>('/api/accounts', { name: this.accountName, currency: this.currency.toUpperCase(), userId: this.userId }).subscribe({
      next: a => { this.accounts = [...this.accounts, a]; this.accountName = ''; },
      error: () => this.message = 'Could not create account.'
    });
  }

  createCategory() {
    this.http.post<Category>('/api/category', { name: this.categoryName, description: this.categoryDescription }).subscribe({
      next: () => {
        this.categoryName = ''; this.categoryDescription = '';
        this.http.get<{content: Category[]}>('/api/category').subscribe({ next: r => this.categories = r.content ?? [] });
      },
      error: () => this.message = 'Could not create category.'
    });
  }

  loadTransactions() {
    this.http.get<Transaction[]>('/api/transactions').subscribe({
      next: rows => this.transactions = rows,
      error: () => this.message = 'Could not load transactions. Check that the API is running.'
    });
    this.http.get<typeof this.summary>('/api/transactions/summary').subscribe({ next: value => this.summary = value });
  }

  createTransaction() {
    if (this.form.categoryId === null) return;
    this.http.post('/api/transactions', this.form).subscribe({
      next: () => { this.message = 'Transaction saved.'; this.form.description = ''; this.form.amount = 0; this.loadTransactions(); },
      error: () => this.message = 'Could not save transaction. Check the category and transaction details.'
    });
  }
}

bootstrapApplication(AppComponent, { providers: [provideHttpClient()] }).catch(console.error);
