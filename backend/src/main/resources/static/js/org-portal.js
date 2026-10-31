/**
 * Organization Portal — Client-Side Privacy Engine & Utilities.
 *
 * PRIVACY PRESERVATION MANDATE:
 *   - Raw CSV parsing, numeric column detection, statistics calculation,
 *     Z-score anomaly detection, data clipping, sensitivity calculation,
 *     and Gaussian noise generation operate ENTIRELY in browser memory.
 *   - RAW DATA IS NEVER TRANSMITTED TO THE CENTRAL SERVER.
 *   - Only privacy-preserved aggregate metadata is sent via POST /api/analytics/contributions.
 */

const ORG_AUTH_STORAGE_KEY = 'fpa_org_auth';

/* ---------------------------------------------------------------------- *
 * Auth & API Helpers
 * ---------------------------------------------------------------------- */

const OrgAuth = {
  getSession() {
    try {
      const raw = localStorage.getItem(ORG_AUTH_STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  },

  setSession(session) {
    localStorage.setItem(ORG_AUTH_STORAGE_KEY, JSON.stringify(session));
  },

  clearSession() {
    localStorage.removeItem(ORG_AUTH_STORAGE_KEY);
  },

  notifyPublicDashboardUpdated() {
    try {
      localStorage.setItem('fpa-public-dashboard-refresh', String(Date.now()));
    } catch (error) {
      console.warn('Could not notify other tabs to refresh public dashboard data:', error);
    }
  },

  isDataOwnerSession() {
    const s = this.getSession();
    return !!(s && s.token && s.role === 'DATA_OWNER');
  },

  isAdminSession() {
    const s = this.getSession();
    return !!(s && s.token && s.role === 'ADMIN');
  },

  requireDataOwner() {
    const s = this.getSession();
    if (!s || !s.token) {
      window.location.href = 'org-login.html';
      return null;
    }
    if (s.role !== 'DATA_OWNER') {
      const dashboard = s.role === 'ADMIN'
        ? 'admin-dashboard.html'
        : s.role === 'RESEARCHER'
          ? 'researcher-dashboard.html'
          : 'org-login.html';
      window.location.replace(dashboard);
      return null;
    }
    return s;
  },

  async logout() {
    const session = this.getSession();
    if (session && session.token) {
      try {
        const response = await fetch('/api/auth/logout', {
          method: 'POST',
          headers: { Authorization: `Bearer ${session.token}` }
        });
        if (!response.ok) {
          throw new Error(`Server logout failed (${response.status})`);
        }
      } catch (error) {
        console.error('Could not confirm server-side logout:', error);
        window.alert('The server could not confirm logout. Your local session will be cleared, but this token may remain valid until it expires.');
      }
    }
    this.clearSession();
    window.location.href = 'org-login.html';
  },

  async login(email, password) {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      throw new Error(data.error || 'Login failed. Check your credentials.');
    }
    return data;
  },

  isAuthenticationFailure(response) {
    return response.status === 401
      && /^Bearer(?:\s|$)/i.test(response.headers.get('WWW-Authenticate') || '');
  },

  handleAuthFailure(response, message) {
    if (this.isAuthenticationFailure(response)) {
      this.clearSession();
      window.location.href = 'org-login.html?reason=session-expired';
      throw new Error(message || 'Your session has expired. Please log in again.');
    }
    if (response.status === 403) {
      throw new Error(message || 'Access denied. You do not have permission for this action.');
    }
    return false;
  },

  async fetchMyDatabases() {
    const session = this.getSession();
    if (!session || !session.token) throw new AuthError('Not authenticated.');

    const res = await fetch('/api/databases/mine', {
      headers: { Authorization: `Bearer ${session.token}` }
    });
    this.handleAuthFailure(res, res.status === 401
      ? 'The server rejected the authentication request.'
      : 'Access denied. You do not have permission for this action.');
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.error || `Request failed (${res.status})`);
    }
    return res.json();
  },

  async registerDatabase(organizationName, dbIdentifier, description) {
    const session = this.getSession();
    if (!session || !session.token) throw new AuthError('Not authenticated.');

    const res = await fetch('/api/databases', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${session.token}`
      },
      body: JSON.stringify({ organizationName, dbIdentifier, description })
    });
    const body = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(body.error || `Registration failed (${res.status})`);
    this.notifyPublicDashboardUpdated();
    return body;
  },

  async fetchPrivacyStatus(databaseId) {
    const session = this.getSession();
    const res = await fetch(`/api/analytics/privacy-status/${databaseId}`, {
      headers: session && session.token ? { Authorization: `Bearer ${session.token}` } : {}
    });
    if (!res.ok) return null;
    return res.json();
  },

  async fetchContributions(databaseId, page = 0, size = 10, query = '') {
    const session = this.getSession();
    let url = `/api/analytics/contributions?databaseId=${databaseId}&page=${page}&size=${size}`;
    if (query) url += `&query=${encodeURIComponent(query)}`;

    const res = await fetch(url, {
      headers: session && session.token ? { Authorization: `Bearer ${session.token}` } : {}
    });
    if (!res.ok) throw new Error('Failed to load contributions');
    return res.json();
  },

  async deleteContribution(id) {
    const session = this.getSession();
    const res = await fetch(`/api/analytics/contributions/${id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${session.token}` }
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.error || 'Failed to delete contribution');
    }
    return res.json();
  },

  async fetchFederatedResult(databaseId) {
    const session = this.getSession();
    const res = await fetch(`/api/analytics/results/${databaseId}`, {
      headers: session && session.token ? { Authorization: `Bearer ${session.token}` } : {}
    });
    if (!res.ok) throw new Error('Failed to load federated result');
    return res.json();
  },

  async submitContribution(payload) {
    const session = this.getSession();
    if (!session || !session.token) throw new AuthError('Not authenticated.');

    const res = await fetch('/api/analytics/contributions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${session.token}`
      },
      body: JSON.stringify(payload)
    });

    const data = await res.json().catch(() => ({}));
    this.handleAuthFailure(res, res.status === 401
      ? 'The server rejected the authentication request.'
      : 'Access denied. You do not have permission for this action.');
    if (!res.ok) {
      throw new Error(data.error || `Contribution rejected (${res.status})`);
    }
    this.notifyPublicDashboardUpdated();
    return data;
  }
};

class AuthError extends Error {}

/* ---------------------------------------------------------------------- *
 * Local In-Browser CSV Processing
 * ---------------------------------------------------------------------- */

function parseCsv(text) {
  if (typeof text !== 'string' || text.trim().length === 0) {
    throw new Error('The file appears to be empty.');
  }

  const rows = [];
  let field = '';
  let row = [];
  let inQuotes = false;

  const src = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n');

  for (let i = 0; i < src.length; i++) {
    const c = src[i];
    if (inQuotes) {
      if (c === '"') {
        if (src[i + 1] === '"') {
          field += '"';
          i++;
        } else {
          inQuotes = false;
        }
      } else {
        field += c;
      }
      continue;
    }

    if (c === '"') {
      inQuotes = true;
    } else if (c === ',') {
      row.push(field);
      field = '';
    } else if (c === '\n') {
      row.push(field);
      rows.push(row);
      row = [];
      field = '';
    } else {
      field += c;
    }
  }
  if (field.length > 0 || row.length > 0) {
    row.push(field);
    rows.push(row);
  }

  while (rows.length && rows[rows.length - 1].every(v => v.trim() === '')) {
    rows.pop();
  }

  if (rows.length === 0) throw new Error('No rows found in file.');

  const headers = rows[0].map(h => h.trim());
  if (headers.length === 0 || headers.every(h => h === '')) {
    throw new Error('Could not detect a header row.');
  }

  const dataRows = rows.slice(1).map(r => {
    const obj = {};
    headers.forEach((h, idx) => {
      obj[h] = r[idx] !== undefined ? r[idx].trim() : '';
    });
    return obj;
  });

  return { headers, rows: dataRows };
}

function detectNumericColumns(headers, rows) {
  const numeric = [];
  const nonNumeric = [];

  headers.forEach(h => {
    let sawValue = false;
    let allNumeric = true;

    for (const r of rows) {
      const raw = r[h];
      if (raw === undefined || raw === '') continue;
      sawValue = true;
      const n = Number(raw);
      if (!Number.isFinite(n)) {
        allNumeric = false;
        break;
      }
    }

    if (sawValue && allNumeric) {
      numeric.push(h);
    } else {
      nonNumeric.push(h);
    }
  });

  return { numeric, nonNumeric };
}

function extractNumericValues(rows, column) {
  const values = [];
  for (const r of rows) {
    const raw = r[column];
    if (raw === undefined || raw === '') continue;
    const n = Number(raw);
    if (Number.isFinite(n)) values.push(n);
  }
  return values;
}

function computeLocalStats(values) {
  if (values.length === 0) throw new Error('No numeric values found.');

  const count = values.length;
  const sum = values.reduce((a, b) => a + b, 0);
  const mean = sum / count;

  let varSum = 0;
  for (const v of values) {
    const diff = v - mean;
    varSum += diff * diff;
  }
  const variance = count > 1 ? varSum / (count - 1) : 0;
  const stdDev = Math.sqrt(variance);
  const min = Math.min(...values);
  const max = Math.max(...values);

  return { count, sum, mean, variance, stdDev, min, max };
}

/* ---------------------------------------------------------------------- *
 * Data Clipping & Sensitivity
 * ---------------------------------------------------------------------- */

function clipValue(val, lower, upper) {
  return Math.max(lower, Math.min(upper, val));
}

function clipValues(values, lower, upper) {
  if (lower >= upper) throw new Error('Clipping lower bound must be less than upper bound.');
  return values.map(v => clipValue(v, lower, upper));
}

function calculateSensitivity(lower, upper) {
  return upper - lower;
}

/* ---------------------------------------------------------------------- *
 * Z-Score Anomaly Detection (|z| > 3.0)
 * Matches backend ZScoreAnomalyDetector.java
 * ---------------------------------------------------------------------- */

function detectAnomaliesZScore(values, stats, threshold = 3.0) {
  if (stats.stdDev === 0) {
    return {
      anomalyCount: 0,
      anomalyRate: 0.0,
      threshold,
      meanZScore: 0.0,
      maxZScore: 0.0,
      anomalies: [],
      summary: 'All values are identical; standard deviation is zero.'
    };
  }

  const anomalies = [];
  let scoreSum = 0;
  let maxScore = 0;

  values.forEach((v, idx) => {
    const z = Math.abs((v - stats.mean) / stats.stdDev);
    if (z > threshold) {
      anomalies.push({ rowIndex: idx + 1, value: v, zScore: round4(z) });
      scoreSum += z;
      if (z > maxScore) maxScore = z;
    }
  });

  const rate = anomalies.length / values.length;
  const meanZScore = anomalies.length > 0 ? scoreSum / anomalies.length : 0.0;
  const summary = anomalies.length === 0
    ? `No values exceeded |z| > ${threshold}.`
    : `${anomalies.length} of ${values.length} records exceeded |z| > ${threshold} (anomaly rate: ${(rate * 100).toFixed(2)}%).`;

  return {
    anomalyCount: anomalies.length,
    anomalyRate: rate,
    threshold,
    meanZScore: round4(meanZScore),
    maxZScore: round4(maxScore),
    anomalies,
    summary
  };
}

/* ---------------------------------------------------------------------- *
 * Gaussian Differential Privacy & RDP Privacy Accounting
 * Exact matching implementation of GaussianMechanism.java and PrivacyAccountant.java
 * ---------------------------------------------------------------------- */

const ALPHA_GRID = (function buildAlphaGrid() {
  const alphas = [];
  for (let a = 1.5; a <= 64.0; a += 0.5) alphas.push(a);
  [80, 100, 128, 150, 200, 256].forEach(a => alphas.push(a));
  return alphas;
})();

function rdpOfSingleRelease(alpha, noiseScale) {
  if (noiseScale <= 0) throw new Error('noiseScale must be > 0');
  return alpha / (2.0 * noiseScale * noiseScale);
}

function composeRdp(currentState, noiseScale) {
  const updated = {};
  for (const alpha of ALPHA_GRID) {
    const existing = currentState[alpha] || 0.0;
    updated[alpha] = existing + rdpOfSingleRelease(alpha, noiseScale);
  }
  return updated;
}

function rdpToEpsilon(rdpState, targetDelta) {
  if (Object.keys(rdpState).length === 0) return 0.0;
  let best = Infinity;
  for (const alpha of ALPHA_GRID) {
    const rdp = rdpState[alpha];
    if (rdp === undefined || alpha <= 1.0) continue;
    const candidate = rdp + (Math.log(1.0 / targetDelta) / (alpha - 1.0));
    if (candidate < best) best = candidate;
  }
  return best;
}

function calibrateNoiseScale(epsilon, delta) {
  if (epsilon <= 0) throw new Error('epsilon must be > 0');
  if (delta <= 0 || delta >= 1) throw new Error('delta must be in (0, 1)');

  let lo = 0.1;
  let hi = 1000.0;
  for (let i = 0; i < 80; i++) {
    const mid = (lo + hi) / 2.0;
    const singleState = composeRdp({}, mid);
    const achieved = rdpToEpsilon(singleState, delta);
    if (achieved <= epsilon) {
      hi = mid;
    } else {
      lo = mid;
    }
  }
  return hi;
}

function sampleSecureGaussian() {
  let u1 = 0;
  let u2 = 0;
  while (u1 <= 1e-12) u1 = secureRandomUnit();
  u2 = secureRandomUnit();
  return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
}

function generateGaussianNoise(sigma) {
  if (sigma === 0) return 0.0;
  return sigma * sampleSecureGaussian();
}

function secureRandomUnit() {
  if (window.crypto && window.crypto.getRandomValues) {
    const buf = new Uint32Array(1);
    window.crypto.getRandomValues(buf);
    return buf[0] / 4294967296;
  }
  return Math.random();
}

function round4(n) {
  return Math.round(n * 10000) / 10000;
}

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function initialsFor(name) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  const initials = parts.slice(0, 2).map(p => p[0]?.toUpperCase() || '').join('');
  return initials || name[0].toUpperCase();
}