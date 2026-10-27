
import { useEffect, useState } from 'react'
import './App.css'

const API_URL = 'http://localhost:8080/api/telemetry'

function formatTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

function App() {
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    let cancelled = false

    async function loadTelemetry() {
      setLoading(true)
      setError('')

      try {
        const response = await fetch(
          `${API_URL}?page=${page}&size=100&sort=timestamp,desc`
        )

        if (!response.ok) {
          throw new Error(`Backend returned HTTP ${response.status}`)
        }

        const data = await response.json()
        const items = Array.isArray(data)
          ? data
          : data.content ?? data.data ?? []

        if (!cancelled) setRecords(items)
      } catch (err) {
        if (!cancelled) {
          setError(
            `${err.message}. Check that the Spring Boot backend is running and CORS allows localhost:5173.`
          )
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    loadTelemetry()
    return () => {
      cancelled = true
    }
  }, [page, refreshKey])

  const filteredRecords = records.filter((item) =>
    String(item.vehicleId ?? '')
      .toLowerCase()
      .includes(search.toLowerCase())
  )

  const vehicles = new Set(records.map((item) => item.vehicleId)).size
  const latest = records[0]
  const moving = records.filter((item) => Number(item.speed) > 0).length
  const averageSpeed = records.length
    ? (
        records.reduce((sum, item) => sum + (Number(item.speed) || 0), 0) /
        records.length
      ).toFixed(1)
    : '0.0'

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">G</div>
          <div>
            <strong>GPS PULSE</strong>
            <span>TELEMETRY PLATFORM</span>
          </div>
        </div>

        <div className="nav-label">WORKSPACE</div>
        <div className="nav-item active"><span>▦</span> Overview</div>
        <div className="nav-item"><span>⌖</span> Live tracking</div>
        <div className="nav-item"><span>▤</span> Telemetry records</div>

        <div className="sidebar-bottom">
          <div className="connection-dot" />
          <div>
            <strong>Backend connection</strong>
            <span>Spring Boot · Port 8080</span>
          </div>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div className="breadcrumb">Workspace <span>/</span> Overview</div>
          <div className="topbar-right">
            <span className="environment"><span /> DEVELOPMENT</span>
            <div className="avatar">GP</div>
          </div>
        </header>

        <section className="page-heading">
          <div>
            <p className="eyebrow">FLEET MONITORING</p>
            <h1>GPS Telemetry Dashboard</h1>
            <p className="subtitle">
              Monitor vehicle telemetry and review incoming GPS records.
            </p>
          </div>
          <button
            className="refresh-button"
            onClick={() => setRefreshKey((key) => key + 1)}
          >
            ↻ &nbsp; Refresh data
          </button>
        </section>

        {error && (
          <div className="error-banner">
            <strong>Unable to load telemetry</strong>
            <p>{error}</p>
          </div>
        )}

        <section className="stats-grid">
          <article className="stat-card">
            <div className="stat-top"><span>VEHICLES IN RECORDS</span><span className="stat-icon blue">⌖</span></div>
            <div className="stat-value">{loading ? '—' : vehicles}</div>
            <div className="stat-foot">Unique vehicle IDs on this page</div>
          </article>
          <article className="stat-card">
            <div className="stat-top"><span>TELEMETRY RECORDS</span><span className="stat-icon purple">▤</span></div>
            <div className="stat-value">{loading ? '—' : records.length}</div>
            <div className="stat-foot">Records loaded from the API</div>
          </article>
          <article className="stat-card">
            <div className="stat-top"><span>VEHICLES MOVING</span><span className="stat-icon green">↗</span></div>
            <div className="stat-value">{loading ? '—' : moving}</div>
            <div className="stat-foot">Records with speed above 0</div>
          </article>
          <article className="stat-card">
            <div className="stat-top"><span>AVERAGE RECORDED SPEED</span><span className="stat-icon orange">◷</span></div>
            <div className="stat-value">{loading ? '—' : averageSpeed}<small> km/h</small></div>
            <div className="stat-foot">Average of loaded records</div>
          </article>
        </section>

        <section className="panel latest-panel">
          <div className="panel-heading">
            <div>
              <h2>Latest telemetry snapshot</h2>
              <p>Most recent record returned by the backend</p>
            </div>
            <span className="live-label"><span /> API DATA</span>
          </div>

          {loading ? (
            <div className="empty-state">Loading telemetry records…</div>
          ) : latest ? (
            <div className="snapshot-grid">
              <div className="snapshot-item">
                <span>VEHICLE ID</span>
                <strong>{latest.vehicleId ?? '—'}</strong>
              </div>
              <div className="snapshot-item">
                <span>SPEED</span>
                <strong>{latest.speed ?? '—'} <small>km/h</small></strong>
              </div>
              <div className="snapshot-item">
                <span>LATITUDE</span>
                <strong>{latest.latitude ?? '—'}</strong>
              </div>
              <div className="snapshot-item">
                <span>LONGITUDE</span>
                <strong>{latest.longitude ?? '—'}</strong>
              </div>
              <div className="snapshot-item">
                <span>IGNITION</span>
                <strong>{latest.ignition == null ? '—' : String(latest.ignition)}</strong>
              </div>
              <div className="snapshot-item">
                <span>RECORDED AT</span>
                <strong>{formatTime(latest.timestamp)}</strong>
              </div>
            </div>
          ) : (
            <div className="empty-state">No telemetry records are available yet.</div>
          )}
        </section>

        <section className="panel records-panel">
          <div className="panel-heading records-heading">
            <div>
              <h2>Telemetry records</h2>
              <p>Browse GPS data received by your backend</p>
            </div>
            <input
              className="search-input"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search vehicle ID..."
              aria-label="Search vehicle ID"
            />
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>VEHICLE ID</th>
                  <th>TIMESTAMP</th>
                  <th>LATITUDE</th>
                  <th>LONGITUDE</th>
                  <th>SPEED</th>
                  <th>IGNITION</th>
                </tr>
              </thead>
              <tbody>
                {filteredRecords.map((item, index) => (
                  <tr key={item.id ?? `${item.vehicleId}-${item.timestamp}-${index}`}>
                    <td><strong className="vehicle-id">{item.vehicleId ?? '—'}</strong></td>
                    <td>{formatTime(item.timestamp)}</td>
                    <td>{item.latitude ?? '—'}</td>
                    <td>{item.longitude ?? '—'}</td>
                    <td><span className="speed-pill">{item.speed ?? '—'} km/h</span></td>
                    <td>{item.ignition == null ? '—' : String(item.ignition)}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            {!loading && filteredRecords.length === 0 && (
              <div className="empty-state">
                {error ? 'Telemetry could not be loaded.' : 'No matching records found.'}
              </div>
            )}
          </div>

          <div className="table-footer">
            <span>Page {page + 1} · {filteredRecords.length} matching records</span>
            <div className="pagination">
              <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</button>
              <button onClick={() => setPage((p) => p + 1)}>Next page</button>
            </div>
          </div>
        </section>

        <footer className="page-footer">
          GPS Pulse <span>·</span> Spring Boot API <span>·</span> PostgreSQL / TimescaleDB
        </footer>
      </main>
    </div>
  )
}

export default App


