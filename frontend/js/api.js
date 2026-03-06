// ═══════════════════════════════════════════════════════════
//  Ruchulu API Client
//  All backend calls go through this file.
//  Change BASE_URL to switch between local/production.
// ═══════════════════════════════════════════════════════════

const BASE_URL = window.RUCHULU_API_URL || 'http://localhost:8080/api/v1';

// ── Token management ──────────────────────────────────────
const Auth = {
  getToken:   () => localStorage.getItem('ruchulu_token'),
  setToken:   (t) => localStorage.setItem('ruchulu_token', t),
  getUser:    () => JSON.parse(localStorage.getItem('ruchulu_user') || 'null'),
  setUser:    (u) => localStorage.setItem('ruchulu_user', JSON.stringify(u)),
  isLoggedIn: () => !!localStorage.getItem('ruchulu_token'),
  logout: () => {
    localStorage.removeItem('ruchulu_token');
    localStorage.removeItem('ruchulu_user');
    window.location.href = '/index.html';
  },
  isCustomer: () => Auth.getUser()?.role === 'CUSTOMER',
  isCaterer:  () => Auth.getUser()?.role === 'CATERER',
  isAdmin:    () => Auth.getUser()?.role === 'ADMIN',
};

// ── HTTP helper ───────────────────────────────────────────
async function apiCall(method, path, body = null, requiresAuth = false) {
  const headers = { 'Content-Type': 'application/json' };
  if (requiresAuth) {
    const token = Auth.getToken();
    if (!token) { Auth.logout(); return; }
    headers['Authorization'] = `Bearer ${token}`;
  }
  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);

  try {
    const res = await fetch(`${BASE_URL}${path}`, opts);
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw { status: res.status, ...data };
    return data;
  } catch (err) {
    if (err.status === 401) Auth.logout();
    throw err;
  }
}

const get  = (path, auth = false) => apiCall('GET', path, null, auth);
const post = (path, body, auth = false) => apiCall('POST', path, body, auth);
const put  = (path, body, auth = true) => apiCall('PUT', path, body, auth);
const del  = (path, auth = true) => apiCall('DELETE', path, null, auth);

// ═══════════════════════════════════════════════════════════
//  API Endpoints
// ═══════════════════════════════════════════════════════════

const API = {
  // Auth
  register:        (data) => post('/auth/register', data),
  requestOtp:      (data) => post('/otp/request', data),
  verifyOtp:       (data) => post('/otp/verify', data),

  // Users
  getMe:           ()     => get('/users/me', true),
  updateMe:        (data) => put('/users/me', data),

  // Caterers
  searchCaterers:  (params) => get(`/caterers/search?${new URLSearchParams(params)}`),
  topRated:        (city)   => get(`/caterers/top-rated/${city}`),
  getCaterer:      (id)     => get(`/caterers/${id}`),
  getMyProfile:    ()       => get('/caterers/me', true),
  registerCaterer: (data)   => post('/caterers/register', data, true),
  updateCaterer:   (data)   => put('/caterers/me', data),
  getMenu:         (id)     => get(`/caterers/${id}/menu`),
  getReviews:      (id)     => get(`/caterers/${id}/reviews`),
  addMenuItem:     (id, d)  => post(`/caterers/${id}/menu`, d, true),

  // Bookings
  createBooking:   (data)   => post('/bookings', data, true),
  getMyBookings:   (status) => get(`/bookings/my${status ? '?status='+status : ''}`, true),
  getBooking:      (id)     => get(`/bookings/${id}`, true),
  cancelBooking:   (id, r)  => post(`/bookings/${id}/cancel`, { reason: r }, true),
  confirmBooking:  (id, n)  => post(`/bookings/${id}/confirm`, { catererNotes: n }, true),
  rejectBooking:   (id, r)  => post(`/bookings/${id}/reject`, { reason: r }, true),
  completeBooking: (id)     => post(`/bookings/${id}/complete`, {}, true),
  getTimeline:     (id)     => get(`/bookings/${id}/timeline`, true),
  getCatBookings:  (status) => get(`/bookings/caterer/incoming${status ? '?status='+status : ''}`, true),

  // Notifications
  getNotifications: () => get('/notifications/my', true),
};

// ═══════════════════════════════════════════════════════════
//  UI Utilities
// ═══════════════════════════════════════════════════════════

function toast(msg, type = 'info') {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }
  const t = document.createElement('div');
  t.className = `toast ${type}`;
  const icons = { success: '✅', error: '❌', info: 'ℹ️' };
  t.innerHTML = `<span>${icons[type] || '🔔'}</span> ${msg}`;
  container.appendChild(t);
  setTimeout(() => t.remove(), 3100);
}

function showModal(id)  { document.getElementById(id)?.classList.add('open'); }
function closeModal(id) { document.getElementById(id)?.classList.remove('open'); }

function setLoading(btn, loading) {
  if (loading) {
    btn.dataset.original = btn.innerHTML;
    btn.innerHTML = '<span class="spinner" style="width:18px;height:18px;border-width:2px"></span>';
    btn.disabled = true;
  } else {
    btn.innerHTML = btn.dataset.original || btn.innerHTML;
    btn.disabled = false;
  }
}

function formatDate(d) {
  if (!d) return '';
  return new Date(d).toLocaleDateString('en-IN', { day:'numeric', month:'short', year:'numeric' });
}

function formatCurrency(n) {
  return '₹' + Number(n).toLocaleString('en-IN');
}

function starsHtml(rating) {
  const r = Math.round(rating || 0);
  return '★'.repeat(r) + '☆'.repeat(5 - r);
}

function statusBadge(status) {
  return `<span class="badge badge-${(status||'').toLowerCase()}">${status || ''}</span>`;
}

function initNavbar() {
  window.addEventListener('scroll', () => {
    document.querySelector('.navbar')?.classList.toggle('scrolled', window.scrollY > 20);
  });
  // Hamburger
  document.querySelector('.hamburger')?.addEventListener('click', () => {
    document.querySelector('.nav-links')?.classList.toggle('open');
    document.querySelector('.nav-auth')?.classList.toggle('open');
  });
  // Auth-aware nav
  const user = Auth.getUser();
  const navAuth = document.getElementById('nav-auth');
  if (navAuth) {
    if (user) {
      navAuth.innerHTML = `
        <a href="pages/dashboard.html" class="btn btn-outline btn-sm">Dashboard</a>
        <button onclick="Auth.logout()" class="btn btn-primary btn-sm">Sign Out</button>`;
    } else {
      navAuth.innerHTML = `
        <button onclick="showModal('modal-login')" class="btn btn-outline btn-sm">Sign In</button>
        <button onclick="showModal('modal-register')" class="btn btn-primary btn-sm">Register</button>`;
    }
  }
  // Close modals on overlay click
  document.querySelectorAll('.modal-overlay').forEach(overlay => {
    overlay.addEventListener('click', e => {
      if (e.target === overlay) overlay.classList.remove('open');
    });
  });
}

function initOtpInputs() {
  const inputs = document.querySelectorAll('.otp-input');
  inputs.forEach((inp, i) => {
    inp.addEventListener('input', () => {
      if (inp.value.length === 1 && inputs[i + 1]) inputs[i + 1].focus();
    });
    inp.addEventListener('keydown', e => {
      if (e.key === 'Backspace' && !inp.value && inputs[i - 1]) inputs[i - 1].focus();
    });
    inp.addEventListener('paste', e => {
      const paste = e.clipboardData.getData('text').slice(0, 6);
      [...paste].forEach((c, j) => { if (inputs[j]) inputs[j].value = c; });
      e.preventDefault();
    });
  });
}

function getOtpValue() {
  return [...document.querySelectorAll('.otp-input')].map(i => i.value).join('');
}

function renderCatererCard(c) {
  const img = c.profilePictureUrl
    ? `<img src="${c.profilePictureUrl}" alt="${c.businessName}" loading="lazy">`
    : `<span>🍽️</span>`;
  const verified = c.fssaiVerified
    ? `<span class="cat-badge verified">✓ FSSAI</span>` : '';
  const tags = (c.occasions || []).slice(0,3).map(o =>
    `<span class="tag">${o.replace(/_/g,' ')}</span>`).join('');

  return `
    <div class="card caterer-card" onclick="window.location.href='pages/caterer.html?id=${c.id}'">
      <div class="img-wrap">${img}${verified}</div>
      <div class="card-body">
        <h3 style="margin-bottom:.25rem">${c.businessName}</h3>
        <p style="font-size:.85rem;margin-bottom:.5rem">📍 ${c.city}</p>
        <div class="cat-meta">
          <span class="stars">${starsHtml(c.averageRating)}</span>
          <span>${(c.averageRating||0).toFixed(1)} (${c.totalReviews||0})</span>
          <span>·</span>
          <span>👥 ${c.minGuests}–${c.maxGuests} guests</span>
        </div>
        <div class="cat-price">${formatCurrency(c.minPricePerPlate)}–${formatCurrency(c.maxPricePerPlate)} / plate</div>
        <div class="cat-tags">${tags}</div>
      </div>
    </div>`;
}

// ── Tab switching ─────────────────────────────────────────
function initTabs() {
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const target = btn.dataset.tab;
      btn.closest('.tabs').querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      document.querySelectorAll(`.tab-pane[data-tab="${target}"]`).forEach(p => {
        p.classList.add('active');
      });
      document.querySelectorAll(`.tab-pane:not([data-tab="${target}"])`).forEach(p => {
        p.classList.remove('active');
      });
    });
  });
}

// Guard: redirect to login if not authenticated
function requireAuth() {
  if (!Auth.isLoggedIn()) {
    toast('Please sign in to continue.', 'error');
    setTimeout(() => window.location.href = '/', 1200);
    return false;
  }
  return true;
}

document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  initTabs();
  initOtpInputs();
});
