(function () {
  function q(sel) {
    return document.querySelector(sel);
  }

  function fmt(n, unit) {
    if (n == null || !Number.isFinite(Number(n))) return "—";
    var v = Number(n);
    var s = Math.abs(v) >= 1000
      ? v.toLocaleString(undefined, { maximumFractionDigits: 2 })
      : v.toLocaleString(undefined, { maximumFractionDigits: 3 });
    return unit ? s + " " + unit : s;
  }

  function card(id, series) {
    var unit = series.unit || "";
    var keys = [
      ["total", "Total"],
      ["average", "Average"],
      ["mean", "Mean"],
      ["median", "Median"],
      ["highest", "Highest"],
      ["lowest", "Lowest"],
      ["count", "Count"],
    ];
    var stats = keys
      .map(function (k) {
        var val = k[0] === "count" ? String(series.count ?? 0) : fmt(series[k[0]], unit);
        return '<div class="stat"><span>' + k[1] + "</span><strong>" + val + "</strong></div>";
      })
      .join("");
    var holders = "";
    if (series.highest_holder || series.lowest_holder) {
      holders =
        '<p class="holders">High: <strong>' +
        (series.highest_holder || "—") +
        "</strong> · Low: <strong>" +
        (series.lowest_holder || "—") +
        "</strong></p>";
    }
    return (
      '<section class="card"><h2>' +
      id +
      '</h2><div class="stats">' +
      stats +
      "</div>" +
      holders +
      "</section>"
    );
  }

  function render(data) {
    q("#server-name").textContent = data.server_name || "Server";
    document.title = (data.server_name || "Server") + " · Webstat";
    q("#meta").textContent =
      "server " +
      (data.server_id || "?") +
      " · computed " +
      (data.computed_at || "—");
    var series = data.series || {};
    var ids = Object.keys(series);
    q("#root").innerHTML = ids.length
      ? ids.map(function (id) {
          return card(id, series[id]);
        }).join("")
      : '<p class="meta">No series yet.</p>';
  }

  function tokenQuery() {
    var m = location.search.match(/[?&]token=([^&]+)/);
    return m ? "?token=" + encodeURIComponent(decodeURIComponent(m[1])) : "";
  }

  fetch("/stats.json" + tokenQuery(), { cache: "no-store" })
    .then(function (r) {
      if (!r.ok) throw new Error("HTTP " + r.status);
      return r.json();
    })
    .then(render)
    .catch(function (e) {
      q("#meta").textContent = "Failed to load stats.json: " + e.message;
    });
})();
