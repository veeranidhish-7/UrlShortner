const API_BASE = '/api/v1/urls';
const ADMIN_API = '/api/v1/admin/urls';
let chartInstance = null;
let cachedAdminUrls = [];

// Initialize Page
document.addEventListener('DOMContentLoaded', () => {
    checkAdminUrlParam();
});

// Check for ?admin=... in the URL
function checkAdminUrlParam() {
    const params = new URLSearchParams(window.location.search);
    const adminParam = params.get('admin');

    if (adminParam) {
        localStorage.setItem('admin_key', adminParam);
        // Clean URL in browser bar so secret key is never left visible
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    const savedKey = localStorage.getItem('admin_key');
    if (savedKey) {
        loadAdminDashboard(savedKey);
    }
}

// Load and Render Admin Dashboard
async function loadAdminDashboard(adminKey) {
    const adminSection = document.getElementById('admin-section');
    const adminBar = document.getElementById('admin-status-bar');
    const tbody = document.getElementById('admin-urls-tbody');
    const errorEl = document.getElementById('admin-error');

    errorEl.classList.add('hidden');

    try {
        const response = await fetch(ADMIN_API, {
            headers: { 'X-Admin-Key': adminKey }
        });

        if (response.status === 401 || response.status === 403) {
            localStorage.removeItem('admin_key');
            adminSection.classList.add('hidden');
            adminBar.classList.add('hidden');
            return;
        }

        if (!response.ok) {
            throw new Error('Failed to fetch admin data');
        }

        const data = await response.json();
        cachedAdminUrls = data.urls || [];

        // Update KPI Badges
        document.getElementById('admin-total-urls').textContent = data.totalUrls;
        document.getElementById('admin-total-clicks').textContent = data.totalClicks;
        document.getElementById('admin-active-urls').textContent = data.activeUrls;

        // Render URL list
        renderAdminUrlsTable();

        // Reveal Admin UI
        adminSection.classList.remove('hidden');
        adminBar.classList.remove('hidden');
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
}

// Render Admin URLs Table with live search & filter
function renderAdminUrlsTable() {
    const tbody = document.getElementById('admin-urls-tbody');
    const searchQuery = (document.getElementById('admin-search-input')?.value || '').toLowerCase().trim();
    const statusFilter = document.getElementById('admin-status-filter')?.value || 'all';

    tbody.innerHTML = '';

    const filtered = cachedAdminUrls.filter(url => {
        const matchesSearch = 
            (url.shortCode && url.shortCode.toLowerCase().includes(searchQuery)) ||
            (url.originalUrl && url.originalUrl.toLowerCase().includes(searchQuery));
        
        const matchesStatus = 
            statusFilter === 'all' ||
            (statusFilter === 'active' && url.isActive) ||
            (statusFilter === 'inactive' && !url.isActive);

        return matchesSearch && matchesStatus;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-secondary); padding: 1.5rem;">No matching short URLs found</td></tr>';
        return;
    }

    filtered.forEach(url => {
        const tr = document.createElement('tr');
        
        const shortUrlDisplay = url.shortUrl || `/${url.shortCode}`;
        const createdDate = url.createdAt ? new Date(url.createdAt).toLocaleDateString(undefined, {
            month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
        }) : '-';

        tr.innerHTML = `
            <td>
                <a href="${shortUrlDisplay}" target="_blank" class="short-code-link">/${url.shortCode}</a>
                <button class="btn-action btn-subtle" onclick="copyText('${shortUrlDisplay}', this)" title="Copy link" style="margin-left: 0.35rem; padding: 0.2rem 0.45rem;">📋</button>
            </td>
            <td class="dest-url-cell" title="${escapeHtml(url.originalUrl)}">
                <a href="${url.originalUrl}" target="_blank" rel="noopener noreferrer">${escapeHtml(url.originalUrl)}</a>
            </td>
            <td><strong>${url.clickCount || 0}</strong></td>
            <td style="color: var(--text-secondary); font-size: 0.85rem;">${createdDate}</td>
            <td>
                <span class="badge ${url.isActive ? 'badge-active' : 'badge-inactive'}">
                    ${url.isActive ? 'Active' : 'Inactive'}
                </span>
            </td>
            <td>
                <button class="btn-action btn-info" onclick="inspectStats('${url.shortCode}')" title="View click telemetry">📊 Stats</button>
                <button class="btn-action btn-danger" onclick="deleteAdminUrl('${url.shortCode}')" title="Delete from Database">🗑️ Delete</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// Delete URL as Admin
async function deleteAdminUrl(shortCode) {
    const adminKey = localStorage.getItem('admin_key');
    if (!adminKey) return;

    const confirmed = confirm(`Are you sure you want to permanently delete /${shortCode} and all its click telemetry directly from the database?`);
    if (!confirmed) return;

    try {
        const response = await fetch(`${ADMIN_API}/${shortCode}?purge=true`, {
            method: 'DELETE',
            headers: { 'X-Admin-Key': adminKey }
        });

        if (!response.ok) {
            const errData = await response.json().catch(() => ({}));
            throw new Error(errData.error || 'Failed to delete URL');
        }

        // Remove from cached array and re-render
        cachedAdminUrls = cachedAdminUrls.filter(u => u.shortCode !== shortCode);
        document.getElementById('admin-total-urls').textContent = cachedAdminUrls.length;
        document.getElementById('admin-active-urls').textContent = cachedAdminUrls.filter(u => u.isActive).length;
        renderAdminUrlsTable();
    } catch (err) {
        alert(`Error deleting URL: ${err.message}`);
    }
}

// Quick jump to Stats from Admin table
function inspectStats(shortCode) {
    const input = document.getElementById('short-code-input');
    input.value = shortCode;
    document.getElementById('stats-form').dispatchEvent(new Event('submit'));
    document.querySelector('.dashboard-card').scrollIntoView({ behavior: 'smooth' });
}

// Copy Helper
function copyText(text, btnElement) {
    navigator.clipboard.writeText(text).then(() => {
        const originalText = btnElement.textContent;
        btnElement.textContent = '✓';
        setTimeout(() => btnElement.textContent = originalText, 1500);
    });
}

// Escape HTML utility to prevent XSS
function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// Admin Listeners
document.getElementById('admin-refresh-btn')?.addEventListener('click', () => {
    const key = localStorage.getItem('admin_key');
    if (key) loadAdminDashboard(key);
});

document.getElementById('admin-logout-btn')?.addEventListener('click', () => {
    localStorage.removeItem('admin_key');
    document.getElementById('admin-section').classList.add('hidden');
    document.getElementById('admin-status-bar').classList.add('hidden');
});

document.getElementById('admin-search-input')?.addEventListener('input', renderAdminUrlsTable);
document.getElementById('admin-status-filter')?.addEventListener('change', renderAdminUrlsTable);

// Shorten URL Form (Guest or Admin)
document.getElementById('shorten-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const originalUrl = document.getElementById('original-url').value;
    const errorEl = document.getElementById('error-message');
    const resultContainer = document.getElementById('result-container');
    const shortUrlEl = document.getElementById('short-url');
    
    errorEl.classList.add('hidden');
    resultContainer.classList.add('hidden');

    try {
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ originalUrl })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.originalUrl || data.error || 'Failed to shorten URL');
        }

        shortUrlEl.href = data.shortUrl;
        shortUrlEl.textContent = data.shortUrl;
        resultContainer.classList.remove('hidden');

        // If admin is logged in, refresh table to show new link
        const adminKey = localStorage.getItem('admin_key');
        if (adminKey) {
            loadAdminDashboard(adminKey);
        }
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
});

// Copy button on creation
document.getElementById('copy-btn').addEventListener('click', () => {
    const shortUrl = document.getElementById('short-url').textContent;
    navigator.clipboard.writeText(shortUrl).then(() => {
        const btn = document.getElementById('copy-btn');
        btn.textContent = 'Copied!';
        setTimeout(() => btn.textContent = 'Copy', 2000);
    });
});

// Stats Form
document.getElementById('stats-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const shortCode = document.getElementById('short-code-input').value.trim();
    const errorEl = document.getElementById('stats-error');
    const statsContainer = document.getElementById('stats-container');
    
    errorEl.classList.add('hidden');
    statsContainer.classList.add('hidden');

    try {
        const response = await fetch(`${API_BASE}/${shortCode}/stats`);
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Failed to fetch stats');
        }

        document.getElementById('total-clicks').textContent = data.totalClicks;
        document.getElementById('last-clicked').textContent = data.lastClickedAt 
            ? new Date(data.lastClickedAt).toLocaleString() 
            : 'Never';

        renderTable(data.recentClicks);
        renderChart(data.recentClicks);

        statsContainer.classList.remove('hidden');
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
});

function renderTable(clicks) {
    const tbody = document.querySelector('#visits-table tbody');
    tbody.innerHTML = '';
    
    if (clicks.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" style="text-align: center; color: var(--text-secondary);">No visits yet</td></tr>';
        return;
    }

    clicks.forEach(click => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${new Date(click.clickTime).toLocaleString()}</td>
            <td>${escapeHtml(click.browserOs)}</td>
            <td>${escapeHtml(click.referer)}</td>
        `;
        tbody.appendChild(tr);
    });
}

function renderChart(clicks) {
    const ctx = document.getElementById('clicksChart').getContext('2d');
    
    if (chartInstance) {
        chartInstance.destroy();
    }

    const dateCounts = {};
    clicks.forEach(click => {
        const date = new Date(click.clickTime).toLocaleDateString();
        dateCounts[date] = (dateCounts[date] || 0) + 1;
    });

    const labels = Object.keys(dateCounts);
    const data = Object.values(dateCounts);

    chartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Clicks per Day (Last 7 Days)',
                data: data,
                backgroundColor: 'rgba(96, 165, 250, 0.5)',
                borderColor: 'rgba(96, 165, 250, 1)',
                borderWidth: 1,
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { stepSize: 1, color: '#94a3b8' },
                    grid: { color: '#334155' }
                },
                x: {
                    ticks: { color: '#94a3b8' },
                    grid: { display: false }
                }
            },
            plugins: {
                legend: { labels: { color: '#f8fafc' } }
            }
        }
    });
}
