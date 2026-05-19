// ===== API CONFIGURATION =====
const API_BASE = '/api';
const APP_VERSION = 'hostel-warden-fix-v10-attendance-dashboard';
(function refreshAfterCodeUpdate(){
    try {
        const oldVersion = localStorage.getItem('hostelAppVersion');
        if (oldVersion && oldVersion !== APP_VERSION) {
            // Keep login after app updates; only clear old static cache.
            if (window.caches) caches.keys().then(keys => keys.forEach(k => caches.delete(k)));
            localStorage.setItem('hostelAppVersion', APP_VERSION);
        } else {
            localStorage.setItem('hostelAppVersion', APP_VERSION);
        }
    } catch (e) { console.warn('Version refresh skipped', e); }
})();

// ===== AUTH UTILITIES =====
const Auth = {
    getToken() { return localStorage.getItem('token'); },
    getUser() {
        const u = localStorage.getItem('user');
        return u ? JSON.parse(u) : null;
    },
    setSession(data) {
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify({ username: data.username, fullName: data.fullName, role: data.role }));
    },
    clearSession() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('user');
    },
    isLoggedIn() { return !!this.getToken(); },
    requireAuth() {
        if (!this.isLoggedIn()) {
            window.location.href = '/login.html';
        }
    },
    redirectIfLoggedIn() {
        if (this.isLoggedIn()) {
            window.location.href = '/dashboard.html';
        }
    }
};

// ===== HTTP CLIENT =====
const API = {
    async request(method, path, body = null) {
        const headers = { 'Content-Type': 'application/json' };
        const token = Auth.getToken();
        if (token) headers['Authorization'] = `Bearer ${token}`;

        const options = { method, headers };
        if (body !== null) options.body = JSON.stringify(body);

        if (!navigator.onLine) {
            throw new Error('Offline: connect to the hostel server to save or load data.');
        }

        const response = await fetchWithRetry(API_BASE + path, options);

        if (response.status === 401 || response.status === 403) {
            throw new Error('Access denied or login token is invalid. Use Logout and login again only if this keeps happening.');
        }

        const data = await parseResponseBody(response);

        if (!response.ok) {
            const msg = errorMessage(data, 'Request failed');
            console.error('API ERROR:', { path: API_BASE + path, status: response.status, data });
            throw new Error(msg);
        }

        return data;
    },
    get: (path) => API.request('GET', path),
    post: (path, body) => API.request('POST', path, body),
    put: (path, body) => API.request('PUT', path, body),
    patch: (path, body) => API.request('PATCH', path, body),
    delete: (path) => API.request('DELETE', path),

    async upload(path, file) {
        const formData = new FormData();
        formData.append('file', file);
        const headers = {};
        const token = Auth.getToken();
        if (token) headers['Authorization'] = `Bearer ${token}`;

        if (!navigator.onLine) {
            throw new Error('Offline: connect to the hostel server to upload files.');
        }

        const response = await fetchWithRetry(API_BASE + path, { method: 'POST', headers, body: formData }, 1);

        if (response.status === 401 || response.status === 403) {
            throw new Error('Access denied or login token is invalid. Use Logout and login again only if this keeps happening.');
        }

        const data = await parseResponseBody(response);

        if (!response.ok) {
            const msg = errorMessage(data, 'Upload failed');
            console.error('UPLOAD ERROR:', { path: API_BASE + path, status: response.status, data });
            throw new Error(msg);
        }

        return data;
    },

    async download(path, filename) {
        const headers = {};
        const token = Auth.getToken();
        if (token) headers['Authorization'] = `Bearer ${token}`;

        if (!navigator.onLine) {
            throw new Error('Offline: connect to the hostel server to download reports.');
        }

        const response = await fetchWithRetry(API_BASE + path, { headers });

        if (response.status === 401 || response.status === 403) {
            throw new Error('Access denied or login token is invalid. Use Logout and login again only if this keeps happening.');
        }

        if (!response.ok) {
            const data = await parseResponseBody(response);
            const msg = errorMessage(data, 'Download failed');
            console.error('DOWNLOAD ERROR:', { path: API_BASE + path, status: response.status, data });
            throw new Error(msg);
        }

        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
    }
};


async function fetchWithRetry(url, options = {}, retries = 2) {
    let lastError;
    for (let attempt = 0; attempt <= retries; attempt++) {
        try {
            const controller = new AbortController();
            const timeout = setTimeout(() => controller.abort(), attempt === 0 ? 15000 : 30000);
            const response = await fetch(url, { ...options, signal: controller.signal });
            clearTimeout(timeout);
            return response;
        } catch (err) {
            lastError = err;
            if (attempt < retries) await new Promise(r => setTimeout(r, 900 * (attempt + 1)));
        }
    }
    throw new Error('Server is not responding. On Render free plan, wait 30–60 seconds and try again.');
}

async function parseResponseBody(response) {
    const text = await response.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch { return text; }
}

function errorMessage(data, fallback) {
    if (!data) return fallback;
    if (typeof data === 'string') return data || fallback;
    if (data.error) return data.error;
    if (data.message) return data.message;
    if (typeof data === 'object') return Object.values(data).filter(Boolean).join(', ') || fallback;
    return fallback;
}

// ===== TOAST NOTIFICATIONS =====
const Toast = {
    container: null,
    init() {
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.className = 'toast-container';
            document.body.appendChild(this.container);
        }
    },
    show(message, type = 'default', duration = 3500) {
        this.init();
        const icons = { success: '✅', error: '❌', warning: '⚠️', default: 'ℹ️' };
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `<span>${icons[type] || icons.default}</span><span>${message}</span>`;
        this.container.appendChild(toast);
        setTimeout(() => { toast.style.opacity = '0'; toast.style.transition = 'opacity 0.3s'; setTimeout(() => toast.remove(), 300); }, duration);
    },
    success: (msg) => Toast.show(msg, 'success'),
    error: (msg) => Toast.show(msg, 'error'),
    warning: (msg) => Toast.show(msg, 'warning')
};

// ===== MODAL UTILITIES =====
const Modal = {
    open(id) {
        const el = document.getElementById(id);
        if (el) el.classList.add('show');
    },
    close(id) {
        const el = document.getElementById(id);
        if (el) el.classList.remove('show');
    },
    closeAll() {
        document.querySelectorAll('.modal-overlay.show').forEach(m => m.classList.remove('show'));
    }
};

// Close modal on overlay click
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) Modal.closeAll();
});
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') Modal.closeAll();
});

// ===== SIDEBAR SETUP =====
function initSidebar(activePage) {
    const user = Auth.getUser();
    if (!user) return;
    // Set user info
    const nameEl = document.getElementById('sidebarUserName');
    const initEl = document.getElementById('sidebarUserInitial');
    const roleEl = document.getElementById('sidebarUserRole');
    if (nameEl) nameEl.textContent = user.fullName;
    if (initEl) initEl.textContent = user.fullName ? user.fullName[0].toUpperCase() : 'W';
    if (roleEl) roleEl.textContent = user.role;
    // Set active nav link
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
        if (link.dataset.page === activePage) link.classList.add('active');
    });
    setupMobileShell();
}

function setupMobileShell() {
    const sidebar = document.querySelector('.sidebar');
    const topbar = document.querySelector('.topbar');
    if (!sidebar || !topbar || document.getElementById('mobileMenuBtn')) return;

    const overlay = document.createElement('div');
    overlay.className = 'sidebar-backdrop';
    overlay.id = 'sidebarBackdrop';
    document.body.appendChild(overlay);

    const button = document.createElement('button');
    button.type = 'button';
    button.id = 'mobileMenuBtn';
    button.className = 'mobile-menu-btn';
    button.innerHTML = '☰';
    button.setAttribute('aria-label', 'Open menu');
    topbar.prepend(button);

    const closeSidebar = () => {
        sidebar.classList.remove('open');
        overlay.classList.remove('show');
        document.body.classList.remove('no-scroll');
    };
    const openSidebar = () => {
        sidebar.classList.add('open');
        overlay.classList.add('show');
        document.body.classList.add('no-scroll');
    };

    button.addEventListener('click', () => sidebar.classList.contains('open') ? closeSidebar() : openSidebar());
    overlay.addEventListener('click', closeSidebar);
    document.querySelectorAll('.sidebar .nav-link').forEach(link => link.addEventListener('click', closeSidebar));
}

window.addEventListener('online', () => Toast.success('Back online'));
window.addEventListener('offline', () => Toast.warning('Offline mode: pages may load, but saving needs server connection'));

function logout() {
    API.post('/auth/logout').catch(() => {});
    Auth.clearSession();
    window.location.href = '/login.html';
}

// ===== DATE UTILITIES =====
const DateUtil = {
    today() { return new Date().toISOString().split('T')[0]; },
    format(dateStr) {
        if (!dateStr) return '-';
        return new Date(dateStr).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
    },
    formatDateTime(dt) {
        if (!dt) return '-';
        return new Date(dt).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    }
};

// ===== LOADING STATE =====
function setLoading(containerId, isLoading) {
    const el = document.getElementById(containerId);
    if (!el) return;
    if (isLoading) {
        el.innerHTML = '<div class="loading"><div class="spinner"></div><span>Loading...</span></div>';
    }
}

// ===== DEBOUNCE =====
function debounce(fn, delay) {
    let timeout;
    return (...args) => { clearTimeout(timeout); timeout = setTimeout(() => fn(...args), delay); };
}
