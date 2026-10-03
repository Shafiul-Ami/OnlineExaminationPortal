/* ExamSphere — shared frontend helpers: API client, navbar, icons, toasts, modals, theme. */
(function () {
  'use strict';

  // ---------- theme (applied immediately to avoid a flash) ----------
  const THEME_KEY = 'examsphere-theme';
  function storedTheme() { try { return localStorage.getItem(THEME_KEY); } catch { return null; } }
  function currentTheme() {
    return document.documentElement.dataset.theme ||
      (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  }
  const saved = storedTheme();
  if (saved) document.documentElement.dataset.theme = saved;

  // ---------- icons (Lucide-style strokes) ----------
  const ICONS = {
    logo: '<path d="M12 2 3 7v10l9 5 9-5V7z"/><path d="m9 12 2 2 4-4"/>',
    home: '<path d="m3 10 9-7 9 7v10a2 2 0 0 1-2 2h-4v-7H9v7H5a2 2 0 0 1-2-2z"/>',
    book: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20V2H6.5A2.5 2.5 0 0 0 4 4.5z"/><path d="M4 19.5A2.5 2.5 0 0 0 6.5 22H20v-5"/>',
    layers: '<path d="m12 2 10 5-10 5L2 7z"/><path d="m2 17 10 5 10-5"/><path d="m2 12 10 5 10-5"/>',
    help: '<circle cx="12" cy="12" r="10"/><path d="M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3"/><path d="M12 17h.01"/>',
    chart: '<path d="M3 3v18h18"/><path d="M7 16v-5"/><path d="M12 16V8"/><path d="M17 16v-9"/>',
    users: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.9"/><path d="M16 3.1a4 4 0 0 1 0 7.8"/>',
    user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
    logout: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5"/><path d="M21 12H9"/>',
    login: '<path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><path d="m10 17 5-5-5-5"/><path d="M15 12H3"/>',
    sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
    moon: '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>',
    menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
    x: '<path d="M18 6 6 18M6 6l12 12"/>',
    clock: '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
    check: '<path d="M20 6 9 17l-5-5"/>',
    checkCircle: '<circle cx="12" cy="12" r="10"/><path d="m9 12 2 2 4-4"/>',
    xCircle: '<circle cx="12" cy="12" r="10"/><path d="m15 9-6 6M9 9l6 6"/>',
    minusCircle: '<circle cx="12" cy="12" r="10"/><path d="M8 12h8"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    edit: '<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/>',
    trash: '<path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>',
    upload: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="m17 8-5-5-5 5"/><path d="M12 3v12"/>',
    search: '<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',
    trophy: '<path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/><path d="M4 22h16"/><path d="M10 14.7V17c0 .6-.5 1-1 1.2C7.8 18.8 7 20.2 7 22"/><path d="M14 14.7V17c0 .6.5 1 1 1.2 1.2.6 2 2 2 3.8"/><path d="M18 2H6v7a6 6 0 0 0 12 0z"/>',
    play: '<circle cx="12" cy="12" r="10"/><path d="m10 8 6 4-6 4z"/>',
    flag: '<path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><path d="M4 22v-7"/>',
    arrowRight: '<path d="M5 12h14M12 5l7 7-7 7"/>',
    arrowLeft: '<path d="M19 12H5M12 19l-7-7 7-7"/>',
    chevronLeft: '<path d="m15 18-6-6 6-6"/>',
    chevronRight: '<path d="m9 18 6-6-6-6"/>',
    chevronDown: '<path d="m6 9 6 6 6-6"/>',
    grid: '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/>',
    file: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>',
    zap: '<path d="M13 2 3 14h9l-1 8 10-12h-9z"/>',
    shield: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="m9 12 2 2 4-4"/>',
    target: '<circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/>',
    bulb: '<path d="M9 18h6M10 22h4"/><path d="M15.1 14c.2-1 .7-1.7 1.4-2.5A6 6 0 1 0 7.5 11.5c.7.8 1.2 1.5 1.4 2.5"/>',
    mail: '<rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/>',
    lock: '<rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>',
    eye: '<path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/>',
    eyeOff: '<path d="M9.9 4.2A10 10 0 0 1 12 4c7 0 10 8 10 8a13 13 0 0 1-1.7 2.7M6.6 6.6A13 13 0 0 0 2 12s3 8 10 8a9.7 9.7 0 0 0 5.4-1.6"/><path d="m2 2 20 20"/><path d="M14.1 14.1a3 3 0 1 1-4.2-4.2"/>',
    info: '<circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/>',
    alert: '<path d="m21.7 18-8-14a2 2 0 0 0-3.4 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3z"/><path d="M12 9v4M12 17h.01"/>',
    rotate: '<path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5"/>',
    printer: '<path d="M6 9V2h12v7"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/>',
    sparkles: '<path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3z"/>',
    history: '<path d="M3 12a9 9 0 1 0 9-9 9.8 9.8 0 0 0-6.7 2.7L3 8"/><path d="M3 3v5h5M12 7v5l4 2"/>',
    send: '<path d="m22 2-7 20-4-9-9-4z"/><path d="M22 2 11 13"/>',
    eraser: '<path d="m7 21-4.3-4.3a1 1 0 0 1 0-1.4l10-10a1 1 0 0 1 1.4 0l5.6 5.6a1 1 0 0 1 0 1.4L11 21"/><path d="M22 21H7M5 11l9 9"/>',
  };
  function icon(name, cls = '') {
    return `<svg class="icon ${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[name] || ''}</svg>`;
  }

  // ---------- utils ----------
  const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
  const initials = (name) => (name || '?').trim().split(/\s+/).slice(0, 2).map((p) => p[0]).join('').toUpperCase();
  function fmtDate(iso, withTime = true) {
    if (!iso) return '—';
    const d = new Date(iso);
    const opts = { day: 'numeric', month: 'short', year: 'numeric' };
    if (withTime) Object.assign(opts, { hour: '2-digit', minute: '2-digit' });
    return d.toLocaleString(undefined, opts);
  }
  function fmtDuration(sec) {
    if (sec == null) return '—';
    sec = Math.max(0, Math.round(sec));
    const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60), s = sec % 60;
    if (h) return `${h}h ${m}m`;
    return m ? `${m}m ${String(s).padStart(2, '0')}s` : `${s}s`;
  }
  function scoreClass(p) { return p >= 75 ? 'score-high' : p >= 50 ? 'score-mid' : 'score-low'; }
  function scoreBadge(p) {
    const v = Number(p || 0);
    return `<span class="score-badge ${scoreClass(v)}">${v % 1 ? v.toFixed(1) : v}%</span>`;
  }
  function diffChip(d) { return `<span class="chip chip-${String(d).toLowerCase()}">${esc(d)}</span>`; }
  function qs(name) { return new URLSearchParams(location.search).get(name); }

  // ---------- API ----------
  async function api(path, { method = 'GET', body } = {}) {
    const res = await fetch(path, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : {},
      body: body ? JSON.stringify(body) : undefined,
      credentials: 'same-origin',
    });
    let data = null;
    try { data = await res.json(); } catch { /* empty body */ }
    if (!res.ok) {
      const err = new Error((data && data.error) || `Request failed (${res.status})`);
      err.status = res.status;
      if (res.status === 401 && !path.startsWith('/api/auth/')) {
        location.href = '/login.html?next=' + encodeURIComponent(location.pathname + location.search);
      }
      throw err;
    }
    return data;
  }

  // ---------- toasts ----------
  function toast(message, type = 'ok') {
    let box = $('.toasts');
    if (!box) { box = document.createElement('div'); box.className = 'toasts'; box.setAttribute('role', 'status'); document.body.appendChild(box); }
    const t = document.createElement('div');
    t.className = `toast ${type}`;
    t.innerHTML = icon(type === 'error' ? 'xCircle' : type === 'info' ? 'info' : 'checkCircle') + `<span>${esc(message)}</span>`;
    box.appendChild(t);
    setTimeout(() => { t.classList.add('leaving'); setTimeout(() => t.remove(), 300); }, 3600);
  }

  // ---------- modals ----------
  function modal({ title, body, footer = '', wide = false, onOpen }) {
    const back = document.createElement('div');
    back.className = 'modal-backdrop';
    back.innerHTML = `
      <div class="modal ${wide ? 'wide' : ''}" role="dialog" aria-modal="true" aria-label="${esc(title)}">
        <div class="modal-head"><h3>${esc(title)}</h3><button class="icon-btn" data-close aria-label="Close">${icon('x')}</button></div>
        <div class="modal-body">${body}</div>
        ${footer ? `<div class="modal-foot">${footer}</div>` : ''}
      </div>`;
    const close = () => { back.remove(); document.removeEventListener('keydown', onKey); };
    const onKey = (e) => { if (e.key === 'Escape') close(); };
    back.addEventListener('mousedown', (e) => { if (e.target === back) close(); });
    back.querySelectorAll('[data-close]').forEach((b) => b.addEventListener('click', close));
    document.addEventListener('keydown', onKey);
    document.body.appendChild(back);
    const first = back.querySelector('input:not([type=hidden]):not([type=radio]), textarea, select');
    if (first) setTimeout(() => first.focus(), 50);
    if (onOpen) onOpen(back, close);
    return { el: back, close };
  }

  function confirmDialog({ title = 'Are you sure?', message = '', confirmText = 'Confirm', danger = false }) {
    return new Promise((resolve) => {
      let done = false;
      const m = modal({
        title,
        body: `<p class="muted" style="margin:0">${esc(message)}</p>`,
        footer: `<button class="btn btn-ghost" data-close>Cancel</button>
                 <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" data-ok>${esc(confirmText)}</button>`,
        onOpen(el, close) {
          el.querySelector('[data-ok]').addEventListener('click', () => { done = true; close(); resolve(true); });
          el.querySelector('[data-ok]').focus();
        },
      });
      const obs = new MutationObserver(() => { if (!document.body.contains(m.el)) { obs.disconnect(); if (!done) resolve(false); } });
      obs.observe(document.body, { childList: true });
    });
  }

  /** Puts a spinner on a button while an async action runs. */
  async function busy(btn, fn) {
    const html = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span>' + (btn.dataset.busy || 'Please wait…');
    try { return await fn(); } finally { btn.disabled = false; btn.innerHTML = html; }
  }

  // ---------- tooltip for chart marks ----------
  let tipEl;
  function bindTooltips(root = document) {
    if (!tipEl) { tipEl = document.createElement('div'); tipEl.className = 'tooltip'; document.body.appendChild(tipEl); }
    $$('[data-tip]', root).forEach((el) => {
      el.addEventListener('mousemove', (e) => {
        tipEl.innerHTML = el.dataset.tip;
        tipEl.style.opacity = 1;
        const x = Math.min(e.clientX + 14, innerWidth - tipEl.offsetWidth - 8);
        tipEl.style.left = x + 'px';
        tipEl.style.top = (e.clientY - tipEl.offsetHeight - 12) + 'px';
      });
      el.addEventListener('mouseleave', () => { tipEl.style.opacity = 0; });
    });
  }

  // ---------- navbar ----------
  const NAV = {
    GUEST: [
      { key: 'home', href: '/', label: 'Home', icon: 'home' },
      { key: 'subjects', href: '/#subjects', label: 'Subjects', icon: 'book' },
      { key: 'features', href: '/#features', label: 'Features', icon: 'sparkles' },
      { key: 'how', href: '/#how', label: 'How it works', icon: 'zap' },
    ],
    STUDENT: [
      { key: 'dashboard', href: '/student/dashboard.html', label: 'Dashboard', icon: 'grid' },
      { key: 'exams', href: '/student/exams.html', label: 'Take a Test', icon: 'play' },
      { key: 'history', href: '/student/history.html', label: 'My Results', icon: 'trophy' },
    ],
    ADMIN: [
      { key: 'dashboard', href: '/admin/dashboard.html', label: 'Dashboard', icon: 'grid' },
      { key: 'subjects', href: '/admin/subjects.html', label: 'Subjects', icon: 'layers' },
      { key: 'questions', href: '/admin/questions.html', label: 'Questions', icon: 'help' },
      { key: 'results', href: '/admin/results.html', label: 'Results', icon: 'chart' },
      { key: 'students', href: '/admin/students.html', label: 'Students', icon: 'users' },
    ],
  };

  function renderNav(user, active) {
    const links = NAV[user ? user.role : 'GUEST'];
    const linkHtml = links.map((l) => `<a class="nav-link ${l.key === active ? 'active' : ''}" href="${l.href}" data-key="${l.key}">${icon(l.icon)}<span>${l.label}</span></a>`).join('');
    const themeBtn = `<button class="icon-btn theme-toggle" aria-label="Toggle dark mode" title="Toggle theme">${icon('moon', 'moon')}${icon('sun', 'sun')}</button>`;
    const home = user ? (user.role === 'ADMIN' ? '/admin/dashboard.html' : '/student/dashboard.html') : '/';

    const actions = user
      ? `<div class="user-menu desktop-only">
           <button class="user-chip" aria-haspopup="true" aria-expanded="false">
             <span class="avatar">${esc(initials(user.name))}</span>
             <span class="who"><strong>${esc(user.name)}</strong><small>${user.role === 'ADMIN' ? 'Administrator' : 'Student'}</small></span>
             ${icon('chevronDown')}
           </button>
           <div class="dropdown" role="menu">
             <div class="dd-head"><strong>${esc(user.name)}</strong><span>${esc(user.email)}</span></div>
             <a href="${home}">${icon('grid')} Dashboard</a>
             ${user.role === 'STUDENT' ? `<a href="/student/history.html">${icon('trophy')} My results</a>` : `<a href="/admin/questions.html">${icon('help')} Question bank</a>`}
             <a href="/">${icon('home')} Home page</a>
             <button class="danger" data-logout>${icon('logout')} Log out</button>
           </div>
         </div>`
      : `<a class="btn btn-ghost btn-sm desktop-only" href="/login.html">${icon('login')} Log in</a>
         <a class="btn btn-primary btn-sm desktop-only" href="/register.html">Get started ${icon('arrowRight')}</a>`;

    const drawerFoot = user
      ? `<div class="person" style="padding:12px;border:1px solid var(--border);border-radius:14px"><span class="avatar">${esc(initials(user.name))}</span><div><strong>${esc(user.name)}</strong><small>${esc(user.email)}</small></div></div>
         <button class="btn btn-ghost" data-logout>${icon('logout')} Log out</button>`
      : `<a class="btn btn-ghost" href="/login.html">${icon('login')} Log in</a><a class="btn btn-primary" href="/register.html">Get started ${icon('arrowRight')}</a>`;

    const wrap = document.createElement('div');
    wrap.innerHTML = `
      <div class="scroll-progress"></div>
      <header class="nav" id="nav">
        <div class="nav-inner">
          <a class="brand" href="${home}"><span class="brand-mark">${icon('logo')}</span><span>Exam<b class="grad-text">Sphere</b></span></a>
          <nav class="nav-links" aria-label="Main">${linkHtml}<span class="nav-indicator"></span></nav>
          <div class="nav-actions">${themeBtn}${actions}
            <button class="icon-btn nav-burger" aria-label="Open menu">${icon('menu')}</button>
          </div>
        </div>
      </header>
      <div class="drawer-backdrop"></div>
      <aside class="drawer" aria-label="Mobile menu">
        <div class="drawer-head"><a class="brand" href="${home}"><span class="brand-mark">${icon('logo')}</span><span>Exam<b class="grad-text">Sphere</b></span></a>
          <button class="icon-btn" data-drawer-close aria-label="Close menu">${icon('x')}</button></div>
        ${linkHtml}
        <div class="drawer-foot">${drawerFoot}</div>
      </aside>`;
    document.body.prepend(...wrap.children);
    wireNav();
  }

  function wireNav() {
    const nav = $('#nav');
    const progress = $('.scroll-progress');
    const onScroll = () => {
      nav.classList.toggle('scrolled', scrollY > 20);
      const h = document.documentElement.scrollHeight - innerHeight;
      progress.style.width = (h > 0 ? (scrollY / h) * 100 : 0) + '%';
    };
    addEventListener('scroll', onScroll, { passive: true });
    onScroll();

    // sliding indicator follows hover, rests on the active link
    const linksBox = $('.nav-links', nav);
    const ind = $('.nav-indicator', nav);
    const moveTo = (el) => {
      if (!el) { ind.style.opacity = 0; return; }
      ind.style.left = el.offsetLeft + 'px';
      ind.style.width = el.offsetWidth + 'px';
      ind.style.opacity = 1;
    };
    const activeLink = () => $('.nav-link.active', linksBox);
    $$('.nav-link', linksBox).forEach((a) => a.addEventListener('mouseenter', () => moveTo(a)));
    linksBox.addEventListener('mouseleave', () => moveTo(activeLink()));
    requestAnimationFrame(() => moveTo(activeLink()));
    addEventListener('resize', () => moveTo(activeLink()));

    // landing page: highlight section links as you scroll
    if (location.pathname === '/' || location.pathname.endsWith('/index.html')) {
      const sections = ['subjects', 'features', 'how'].map((id) => document.getElementById(id)).filter(Boolean);
      const spy = () => {
        let key = 'home';
        sections.forEach((s) => { if (s.getBoundingClientRect().top < 160) key = s.id; });
        $$('.nav-link').forEach((a) => a.classList.toggle('active', a.dataset.key === key));
        moveTo(activeLink());
      };
      addEventListener('scroll', spy, { passive: true });
    }

    $$('.theme-toggle').forEach((b) => b.addEventListener('click', () => {
      const next = currentTheme() === 'dark' ? 'light' : 'dark';
      document.documentElement.dataset.theme = next;
      try { localStorage.setItem(THEME_KEY, next); } catch { /* storage unavailable */ }
    }));

    const menu = $('.user-menu');
    if (menu) {
      const chip = $('.user-chip', menu);
      chip.addEventListener('click', (e) => { e.stopPropagation(); const o = menu.classList.toggle('open'); chip.setAttribute('aria-expanded', o); });
      document.addEventListener('click', () => menu.classList.remove('open'));
    }

    const openDrawer = (o) => document.body.classList.toggle('drawer-open', o);
    $('.nav-burger').addEventListener('click', () => openDrawer(true));
    $('.drawer-backdrop').addEventListener('click', () => openDrawer(false));
    $('[data-drawer-close]').addEventListener('click', () => openDrawer(false));
    $$('.drawer .nav-link').forEach((a, i) => { a.style.transitionDelay = (i * 50 + 80) + 'ms'; a.addEventListener('click', () => openDrawer(false)); });

    $$('[data-logout]').forEach((b) => b.addEventListener('click', async () => {
      try { await api('/api/auth/logout', { method: 'POST' }); } catch { /* ignore */ }
      location.href = '/';
    }));
  }

  /**
   * Boots a page: loads the session, enforces the required role and renders the navbar.
   * role: 'STUDENT' | 'ADMIN' | 'ANY' (any logged-in user) | null (public)
   */
  async function init({ role = null, active = '', nav = true } = {}) {
    let user = null;
    try { user = (await api('/api/auth/me')).user || null; } catch { user = null; }
    if (role && !user) {
      location.href = '/login.html?next=' + encodeURIComponent(location.pathname + location.search);
      return new Promise(() => {});
    }
    if (role && role !== 'ANY' && user.role !== role) {
      location.href = user.role === 'ADMIN' ? '/admin/dashboard.html' : '/student/dashboard.html';
      return new Promise(() => {});
    }
    if (nav) renderNav(user, active);
    App.user = user;
    return user;
  }

  function animateCount(el, to, { decimals = 0, suffix = '' } = {}) {
    const start = performance.now(), dur = 1100;
    const step = (t) => {
      const p = Math.min(1, (t - start) / dur);
      const e = 1 - Math.pow(1 - p, 3);
      el.textContent = (to * e).toFixed(decimals) + suffix;
      if (p < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  }

  function emptyState(iconName, title, text, action = '') {
    return `<div class="empty"><div class="empty-icon">${icon(iconName)}</div><h4>${esc(title)}</h4><p>${esc(text)}</p>${action}</div>`;
  }

  window.App = { api, init, icon, esc, $, $$, toast, modal, confirm: confirmDialog, busy, fmtDate, fmtDuration, scoreBadge, scoreClass, diffChip, qs, initials, animateCount, emptyState, bindTooltips, user: null };
})();
