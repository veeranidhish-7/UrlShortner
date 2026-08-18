const API_BASE = '/api/v1/urls';
let chartInstance = null;

// Shorten URL Form
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
    } catch (err) {
        errorEl.textContent = err.message;
        errorEl.classList.remove('hidden');
    }
});

// Copy to Clipboard
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
        tbody.innerHTML = '<tr><td colspan="3" style="text-align: center;">No visits yet</td></tr>';
        return;
    }

    clicks.forEach(click => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${new Date(click.clickTime).toLocaleString()}</td>
            <td>${click.browserOs}</td>
            <td>${click.referer}</td>
        `;
        tbody.appendChild(tr);
    });
}

function renderChart(clicks) {
    const ctx = document.getElementById('clicksChart').getContext('2d');
    
    if (chartInstance) {
        chartInstance.destroy();
    }

    // Group clicks by date
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
