const tableBody = document.getElementById("tableBody");
const chartsContainer = document.getElementById("charts");
const maToggles = document.getElementById("maToggles");
const SYMBOLS = ["SPY", "QQQ", "IWM"];
const MA_COLORS = {
    10: "#38bdf8",
    20: "#fbbf24",
    50: "#c084fc"
};
const MA_STORAGE_KEY = "marketBreath.activeMAs";
const charts = {};
let lastRows = [];

function loadSavedMAs() {
    try {
        const raw = localStorage.getItem(MA_STORAGE_KEY);
        if (raw === null) return;
        const saved = JSON.parse(raw);
        if (!Array.isArray(saved)) return;
        const active = new Set(saved.map(Number));
        maToggles.querySelectorAll('input[type="checkbox"]').forEach((cb) => {
            cb.checked = active.has(Number(cb.value));
        });
    } catch {
        // Ignore storage/parse errors and fall back to HTML defaults
    }
}

function saveActiveMAs() {
    try {
        localStorage.setItem(MA_STORAGE_KEY, JSON.stringify([...getActiveMAs()]));
    } catch {
        // Ignore storage errors (e.g. private browsing quota)
    }
}

function getActiveMAs() {
    return new Set(
        Array.from(maToggles.querySelectorAll('input[type="checkbox"]:checked'), (cb) => Number(cb.value))
    );
}

const money = (v) => "$" + Number(v).toFixed(2);

function box(avg, close) {
    const div = document.createElement("div");
    div.className = "box" + (avg.above == null ? "" : avg.above ? " up" : " down");
    div.textContent = avg.days;
    div.title = avg.value == null
        ? "Not enough data yet"
        : `Close ${money(close)} vs ${avg.days}-day average ${money(avg.value)}`;
    return div;
}

function renderCharts(rows, activeMAs) {
    const chronological = [...rows].reverse();
    const labels = chronological.map((r) => r.date);

    SYMBOLS.forEach((symbol) => {
        charts[symbol]?.destroy();

        let canvas = document.getElementById(`chart-${symbol}`);
        if (!canvas) {
            const card = document.createElement("div");
            card.className = "chart-card";

            const title = document.createElement("h2");
            title.textContent = symbol;

            const wrap = document.createElement("div");
            wrap.className = "chart-box";

            canvas = document.createElement("canvas");
            canvas.id = `chart-${symbol}`;

            wrap.appendChild(canvas);
            card.append(title, wrap);
            chartsContainer.appendChild(card);
        }

        const datasets = [
            {
                label: "Close",
                data: chronological.map((r) => r.signals[symbol]?.close ?? null),
                borderColor: "#e2e8f0",
                backgroundColor: "#e2e8f0",
                borderWidth: 2,
                pointRadius: 0,
                tension: 0.2
            },
            ...Object.entries(MA_COLORS)
                .filter(([days]) => activeMAs.has(Number(days)))
                .map(([days, color]) => ({
                    label: `${days} MA`,
                    data: chronological.map(
                        (r) => r.signals[symbol]?.averages.find((a) => a.days === Number(days))?.value ?? null
                    ),
                    borderColor: color,
                    backgroundColor: color,
                    borderWidth: 1.5,
                    pointRadius: 0,
                    tension: 0.2
                }))
        ];

        charts[symbol] = new Chart(canvas, {
            type: "line",
            data: { labels, datasets },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: "index", intersect: false },
                plugins: {
                    legend: {
                        labels: { color: "#e2e8f0", boxWidth: 12, font: { family: "Inter" } }
                    },
                    tooltip: {
                        callbacks: {
                            label: (ctx) => `${ctx.dataset.label}: ${ctx.raw == null ? "N/A" : money(ctx.raw)}`
                        }
                    }
                },
                scales: {
                    x: {
                        ticks: { color: "#94a3b8", maxTicksLimit: 6 },
                        grid: { color: "#334155" }
                    },
                    y: {
                        ticks: { color: "#94a3b8", callback: (v) => "$" + v },
                        grid: { color: "#334155" }
                    }
                }
            }
        });
    });
}

function render(rows) {
    lastRows = rows;
    const activeMAs = getActiveMAs();
    renderCharts(rows, activeMAs);

    tableBody.replaceChildren(
        ...rows.map((row) => {
            const tr = document.createElement("tr");

            const dateCell = document.createElement("td");
            dateCell.textContent = row.date;
            tr.appendChild(dateCell);

            SYMBOLS.forEach((symbol) => {
                const td = document.createElement("td");
                td.className = "cells";

                const wrap = document.createElement("div");
                wrap.className = "boxes";

                const s = row.signals[symbol];
                if (s) {
                    wrap.append(
                        ...s.averages
                            .filter((a) => activeMAs.has(a.days))
                            .map((a) => box(a, s.close))
                    );
                }

                td.appendChild(wrap);
                tr.appendChild(td);
            });
            return tr;
        })
    );
}

async function request(url, method = "GET") {
    try {
        const res = await fetch(url, { method });
        if (!res.ok) throw new Error(`Request failed: ${res.status}`);
        render(await res.json());
    } catch (err) {
        alert(err.message);
    }
}

maToggles.addEventListener("change", () => {
    saveActiveMAs();
    render(lastRows);
});

document.getElementById("updateButton").addEventListener("click", () => request("/prices/update", "POST"));

loadSavedMAs();
request("/prices"); // show stored data on page load