const tableBody = document.getElementById("tableBody");
const SYMBOLS = ["SPY", "QQQ", "IWM"];

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

function render(rows) {
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
                    wrap.append(...s.averages.map((a) => box(a, s.close)));
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

document.getElementById("updateButton").addEventListener("click", () => request("/prices/update", "POST"));
document.getElementById("olderButton").addEventListener("click", () => request("/prices/older", "POST"));

request("/prices"); // show stored data on page load