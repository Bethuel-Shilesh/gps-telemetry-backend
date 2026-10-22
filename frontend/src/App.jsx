
import { useCallback, useEffect, useMemo, useState } from 'react'
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import './App.css'

const API_URL = 'http://localhost:8080/api/telemetry'
const PAGE_SIZE = 20
const MAP_PAGE_SIZE = 100

const vehicleIcon = L.divIcon({
  className: 'vehicle-marker-wrapper',
  html: '<div class="vehicle-marker"><span></span></div>',
  iconSize: [28, 28],
  iconAnchor: [14, 14],
})

function formatTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString()
}

function displayValue(value) {
  return value === null || value === undefined || value === '' ? '—' : value
}

function csvCell(value) {
  return `"${String(value ?? '').replace(/"/g, '""')}"`
}

function validCoordinates(item) {
  const lat = Number(item.latitude)
  const lng = Number(item.longitude)

  return (
    Number.isFinite(lat) &&
    Number.isFinite(lng) &&
    lat >= -90 &&
    lat <= 90 &&
    lng >= -180 &&
    lng <= 180 &&
    !(lat === 0 && lng === 0)
  )
}

function MapViewport({ position }) {
  const map = useMap()

  useEffect(() => {
    if (position) {
      map.setView(position, Math.max(map.getZoom(), 10))
    }
  }, [map, position])

  return null
}

function App() {
  const [section, setSection] = useState('overview')
  const [records, setRecords] = useState([])
  const [pageInfo, setPageInfo] = useState({
    totalPages: 0,
    totalElements: 0,
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [connection, setConnection] = useState('checking')
  const [vehicleId, setVehicleId] = useState('')
  const [month, setMonth] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState({
    field: 'timestamp',
    direction: 'desc',
  })
  const [refreshKey, setRefreshKey] = useState(0)
  const [filters, setFilters] = useState({
    vehicleId: '',
    month: '',
    fromDate: '',
    toDate: '',
  })

  const loadTelemetry = useCallback(async (signal) => {
    setLoading(true)
    setError('')

    try {
      const params = new URLSearchParams()
      params.set('page', String(section === 'records' ? page : 0))
      params.set(
        'size',
        String(section === 'map' ? MAP_PAGE_SIZE : PAGE_SIZE)
      )
      params.set('sort', `${sort.field},${sort.direction}`)

      if (section === 'records') {
        if (filters.vehicleId.trim()) {
          params.set('vehicleId', filters.vehicleId.trim())
        }

        // A custom date range takes priority over the month selection.
        let start = filters.fromDate
        let end = filters.toDate

        if (!start && !end && filters.month) {
          const [year, monthNumber] = filters.month.split('-').map(Number)
          start = `${year}-${String(monthNumber).padStart(2, '0')}-01`

          const nextMonth = new Date(year, monthNumber, 1)
          end = [
            nextMonth.getFullYear(),
            String(nextMonth.getMonth() + 1).padStart(2, '0'),
            '01',
          ].join('-')
        }

        if (start) {
          params.set('from', new Date(`${start}T00:00:00`).toISOString())
        }

        if (end) {
          const exclusiveEnd = new Date(`${end}T00:00:00`)
          if (filters.toDate) {
            exclusiveEnd.setDate(exclusiveEnd.getDate() + 1)
          }
          params.set('to', exclusiveEnd.toISOString())
        }
      }

      const response = await fetch(`${API_URL}?${params.toString()}`, {
        signal,
      })

      if (!response.ok) {
        throw new Error(`Backend returned HTTP ${response.status}`)
      }

      const data = await response.json()
      const items = Array.isArray(data)
        ? data
        : Array.isArray(data.content)
          ? data.content
          : Array.isArray(data.data)
            ? data.data
            : []

      setRecords(items)
      setPageInfo({
        totalPages: Number(data.totalPages ?? (items.length ? 1 : 0)),
        totalElements: Number(data.totalElements ?? items.length),
      })
      setConnection('connected')
    } catch (err) {
      if (err.name === 'AbortError') return

      setError(
        `${err.message || 'Request failed'}. Check that Spring Boot is running on port 8080.`
      )
      setConnection('disconnected')
      setRecords([])
      setPageInfo({ totalPages: 0, totalElements: 0 })
    } finally {
      if (!signal.aborted) setLoading(false)
    }
  }, [section, page, sort.field, sort.direction, filters])

  useEffect(() => {
    const controller = new AbortController()
    loadTelemetry(controller.signal)
    return () => controller.abort()
  }, [loadTelemetry, refreshKey])

  const vehicles = useMemo(
    () => new Set(records.map((item) => item.vehicleId).filter(Boolean)).size,
    [records]
  )

  const moving = records.filter((item) => Number(item.speed) > 0).length

  const averageSpeed = records.length
    ? (
        records.reduce((sum, item) => sum + (Number(item.speed) || 0), 0) /
        records.length
      ).toFixed(1)
    : '0.0'

  const mapRecords = useMemo(() => {
    const latestByVehicle = new Map()

    // The API is sorted newest first; retain the newest valid location
    // for each vehicle in the records returned to this map view.
    for (const item of records) {
      if (item.vehicleId && validCoordinates(item) &&
          !latestByVehicle.has(item.vehicleId)) {
        latestByVehicle.set(item.vehicleId, item)
      }
    }

    return Array.from(latestByVehicle.values())
  }, [records])

  const mapCenter = mapRecords.length
    ? [Number(mapRecords[0].latitude), Number(mapRecords[0].longitude)]
    : [20.5937, 78.9629]

  function navigate(nextSection) {
    setSection(nextSection)
    setPage(0)
    setError('')
  }

  function changeSort(field) {
    setSort((current) => ({
      field,
      direction:
        current.field === field && current.direction === 'asc'
          ? 'desc'
          : 'asc',
    }))
    setPage(0)
  }

  function searchRecords(event) {
    event.preventDefault()
    setPage(0)
    setFilters({ vehicleId, month, fromDate, toDate })
    setRefreshKey((key) => key + 1)
  }

  function clearFilters() {
    setVehicleId('')
    setMonth('')
    setFromDate('')
    setToDate('')
    setPage(0)
    setFilters({
      vehicleId: '',
      month: '',
      fromDate: '',
      toDate: '',
    })
    setRefreshKey((key) => key + 1)
  }

  function exportCsv() {
    if (!records.length) return

    const columns = [
      ['Vehicle ID', 'vehicleId'],
      ['Timestamp', 'timestamp'],
      ['Latitude', 'latitude'],
      ['Longitude', 'longitude'],
      ['Speed (km/h)', 'speed'],
      ['Ignition', 'ignition'],
      ['Battery voltage', 'batteryVoltage'],
      ['Harsh brake', 'harshBrake'],
      ['Harsh acceleration', 'harshAcceleration'],
      ['Crash event', 'crashEvent'],
    ]

    const lines = [
      columns.map(([label]) => csvCell(label)).join(','),
      ...records.map((item) =>
        columns.map(([, field]) => csvCell(item[field])).join(',')
      ),
    ]

    const blob = new Blob(['\uFEFF' + lines.join('\r\n')], {
      type: 'text/csv;charset=utf-8;',
    })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `gps-telemetry-${section}-page-${page + 1}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  }

  const sectionNames = {
    overview: 'Overview',
    map: 'Live Tracking',
    latest: 'Latest Telemetry',
    records: 'Telemetry Records',
  }

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

        {[
          ['overview', '▦', 'Overview'],
          ['map', '⌖', 'Live Tracking'],
          ['latest', '◷', 'Latest Telemetry'],
          ['records', '▤', 'Telemetry Records'],
        ].map(([id, icon, label]) => (
          <button
            key={id}
            className={`nav-item ${section === id ? 'active' : ''}`}
            onClick={() => navigate(id)}
          >
            <span>{icon}</span> {label}
          </button>
        ))}

        <div className="sidebar-bottom">
          <span className={`connection-dot ${connection}`} />
          <div>
            <strong>
              {connection === 'connected'
                ? 'GPS MONITORING'
                : connection === 'disconnected'
                  ? 'API DISCONNECTED'
                  : 'CHECKING CONNECTION'}
            </strong>

          </div>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div className="breadcrumb">
            Workspace <span>/</span> {sectionNames[section]}
          </div>
          <div className="topbar-right">
            <span className="environment"><span /> DEVELOPMENT</span>
            <div className="avatar" title="GPS Pulse">GP</div>
          </div>
        </header>

        <section className="page-heading">
          <div>
            <p className="eyebrow">FLEET MONITORING</p>
            <h1>{sectionNames[section]}</h1>
            <p className="subtitle">
              {section === 'overview' && 'Your workspace for GPS fleet telemetry.'}
              {section === 'map' && 'View the latest available vehicle coordinates on the map.'}
              {section === 'latest' && 'The 20 most recent records returned by the API.'}
              {section === 'records' && 'Search historical telemetry by vehicle and date.'}
            </p>
          </div>
          <button
            className="refresh-button"
            onClick={() => setRefreshKey((key) => key + 1)}
            disabled={loading}
          >
            {loading ? 'Loading…' : '↻ Refresh'}
          </button>
        </section>



        {error && (
          <div className="error-banner" role="alert">
            <strong>Unable to load telemetry</strong>
            <p>{error}</p>
            <button className="secondary-button" onClick={() => setRefreshKey((key) => key + 1)}>
              Try again
            </button>
          </div>
        )}

        {section === 'overview' && (
          <>
            <section className="welcome-panel">
              <div>
                <p className="eyebrow">GPS TELEMETRY WORKSPACE</p>
                <h2>Fleet visibility starts here.</h2>
                <p>
                  Review recent telemetry, explore vehicle coordinates, or
                  search historical records from one workspace.
                </p>
                <button className="primary-button" onClick={() => navigate('map')}>
                  Open live tracking <span>→</span>
                </button>
              </div>
              <div className="welcome-symbol" aria-hidden="true">
                <span>G</span>
                <i />
              </div>
            </section>

            <section className="stats-grid">
              <article className="stat-card">
                <div className="stat-top"><span>VEHICLES IN RECENT DATA</span><span className="stat-icon blue">⌖</span></div>
                <div className="stat-value">{loading ? '—' : vehicles}</div>
                <div className="stat-foot">Unique IDs in the loaded records</div>
              </article>
              <article className="stat-card">
                <div className="stat-top"><span>TOTAL STORED RECORDS</span><span className="stat-icon purple">▤</span></div>
                <div className="stat-value">{loading ? '—' : pageInfo.totalElements}</div>
                <div className="stat-foot">API total for this request</div>
              </article>
              <article className="stat-card">
                <div className="stat-top"><span>RECORDS WITH MOVEMENT</span><span className="stat-icon green">↗</span></div>
                <div className="stat-value">{loading ? '—' : moving}</div>
                <div className="stat-foot">Speed above 0 in recent data</div>
              </article>
              <article className="stat-card">
                <div className="stat-top"><span>AVERAGE RECORDED SPEED</span><span className="stat-icon orange">◷</span></div>
                <div className="stat-value">{loading ? '—' : averageSpeed}<small> km/h</small></div>
                <div className="stat-foot">Average for loaded records</div>
              </article>
            </section>

            <section className="module-grid">
              <button className="module-card" onClick={() => navigate('map')}>
                <span className="module-icon">⌖</span>
                <strong>Live Tracking</strong>
                <p>Explore available vehicle coordinates on an interactive map.</p>
                <span className="module-link">Open map →</span>
              </button>
              <button className="module-card" onClick={() => navigate('latest')}>
                <span className="module-icon">◷</span>
                <strong>Latest Telemetry</strong>
                <p>Inspect the newest 20 telemetry records.</p>
                <span className="module-link">View latest →</span>
              </button>
              <button className="module-card" onClick={() => navigate('records')}>
                <span className="module-icon">▤</span>
                <strong>Historical Records</strong>
                <p>Filter stored records by vehicle, month, or custom dates.</p>
                <span className="module-link">Search history →</span>
              </button>
            </section>
          </>
        )}

        {section === 'map' && (
          <section className="panel">
            <div className="panel-heading">
              <div>
                <h2>Vehicle location map</h2>
                <p>Latest valid coordinates per vehicle among the 100 records fetched.</p>
              </div>
              <span className="data-label">{mapRecords.length} MAP MARKERS</span>
            </div>

            {mapRecords.length === 0 && (
              <div className="map-empty">
                <span className="module-icon">⌖</span>
                <strong>No valid vehicle coordinates available</strong>
                <p>
                  The map will display vehicle markers when the API returns
                  telemetry with valid latitude and longitude values.
                </p>
              </div>
            )}

            <div className="map-container">
              <MapContainer center={mapCenter} zoom={mapRecords.length ? 10 : 4} scrollWheelZoom>
                <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                <MapViewport position={mapRecords.length ? mapCenter : null} />
                {mapRecords.map((item) => (
                  <Marker
                    key={`${item.vehicleId}-${item.timestamp}`}
                    position={[Number(item.latitude), Number(item.longitude)]}
                    icon={vehicleIcon}
                  >
                    <Popup>
                      <strong>{item.vehicleId}</strong>
                      <br />
                      Speed: {displayValue(item.speed)} km/h
                      <br />
                      Ignition: {String(item.ignition)}
                      <br />
                      Recorded: {formatTime(item.timestamp)}
                      <br />
                      {item.latitude}, {item.longitude}
                    </Popup>
                  </Marker>
                ))}
              </MapContainer>
            </div>
            <p className="map-note">
              Markers show stored API coordinates, not necessarily live vehicle positions.
              OpenStreetMap tiles require an internet connection.
            </p>
          </section>
        )}

        {section === 'latest' && (
          <RecordsTable
            records={records}
            loading={loading}
            page={page}
            pageInfo={pageInfo}
            sort={sort}
            changeSort={changeSort}
            setPage={setPage}
            exportCsv={exportCsv}
            latestMode
          />
        )}

        {section === 'records' && (
          <>
            <section className="panel filter-panel">
              <div className="panel-heading">
                <div>
                  <h2>Find historical telemetry</h2>
                  <p>Search across database records using the available filters.</p>
                </div>
              </div>

              <form className="filter-grid" onSubmit={searchRecords}>
                <label>
                  <span>Vehicle ID</span>
                  <input
                    value={vehicleId}
                    onChange={(event) => setVehicleId(event.target.value)}
                    placeholder="Enter full or partial vehicle ID"
                  />
                </label>
                <label>
                  <span>Month and year</span>
                  <input
                    type="month"
                    value={month}
                    onChange={(event) => {
                      setMonth(event.target.value)
                      setFromDate('')
                      setToDate('')
                    }}
                  />
                </label>
                <label>
                  <span>Start date (optional)</span>
                  <input
                    type="date"
                    value={fromDate}
                    onChange={(event) => {
                      setFromDate(event.target.value)
                      if (event.target.value) setMonth('')
                    }}
                  />
                </label>
                <label>
                  <span>End date (optional)</span>
                  <input
                    type="date"
                    min={fromDate || undefined}
                    value={toDate}
                    onChange={(event) => {
                      setToDate(event.target.value)
                      if (event.target.value) setMonth('')
                    }}
                  />
                </label>
                <div className="filter-actions">
                  <button className="primary-button" type="submit">Search records</button>
                  <button className="secondary-button" type="button" onClick={clearFilters}>Clear filters</button>
                </div>
              </form>
              <p className="filter-hint">
                Choose a month or a custom date range. Dates use your computer's local time.
                The end date is included.
              </p>
            </section>

            <RecordsTable
              records={records}
              loading={loading}
              page={page}
              pageInfo={pageInfo}
              sort={sort}
              changeSort={changeSort}
              setPage={setPage}
              exportCsv={exportCsv}
            />
          </>
        )}


      </main>
    </div>
  )
}

function RecordsTable({
  records,
  loading,
  page,
  pageInfo,
  sort,
  changeSort,
  setPage,
  exportCsv,
  latestMode = false,
}) {
  const sortHeading = (field, label) => (
    <button className="sort-button" onClick={() => changeSort(field)}>
      {label}{' '}
      {sort.field === field
        ? sort.direction === 'asc' ? '↑' : '↓'
        : '↕'}
    </button>
  )

  return (
    <section className="panel records-panel">
      <div className="panel-heading records-heading">
        <div>
          <h2>{latestMode ? 'Latest 20 records' : 'Historical telemetry records'}</h2>
          <p>
            {latestMode
              ? 'Newest records first, with pagination if more records exist.'
              : 'Results matching the applied filters, sorted by timestamp by default.'}
          </p>
        </div>
        <button className="secondary-button" onClick={exportCsv} disabled={!records.length || loading}>
          ↓ Export current page
        </button>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{sortHeading('vehicleId', 'VEHICLE ID')}</th>
              <th>{sortHeading('timestamp', 'TIMESTAMP')}</th>
              <th>{sortHeading('latitude', 'LATITUDE')}</th>
              <th>{sortHeading('longitude', 'LONGITUDE')}</th>
              <th>{sortHeading('speed', 'SPEED')}</th>
              <th>IGNITION</th>
            </tr>
          </thead>
          <tbody>
            {records.map((item, index) => (
              <tr key={item.id ?? `${item.vehicleId}-${item.timestamp}-${index}`}>
                <td><strong className="vehicle-id">{displayValue(item.vehicleId)}</strong></td>
                <td>{formatTime(item.timestamp)}</td>
                <td>{displayValue(item.latitude)}</td>
                <td>{displayValue(item.longitude)}</td>
                <td><span className="speed-pill">{displayValue(item.speed)} km/h</span></td>
                <td>{String(item.ignition)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {!loading && records.length === 0 && (
          <div className="empty-state">
            {latestMode
              ? 'No telemetry records are available yet.'
              : 'No records match these filters. Try another vehicle or date range.'}
          </div>
        )}

        {loading && <div className="empty-state">Loading telemetry…</div>}
      </div>

      <div className="table-footer">
        <span>
          Page {page + 1} of {Math.max(pageInfo.totalPages, 1)}
          {' · '}{pageInfo.totalElements} matching records
        </span>
        <div className="pagination">
          <button
            disabled={loading || page === 0}
            onClick={() => setPage((current) => Math.max(0, current - 1))}
          >
            Previous
          </button>
          <button
            disabled={loading || pageInfo.totalPages === 0 || page + 1 >= pageInfo.totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Next page
          </button>
        </div>
      </div>
    </section>
  )
}

export default App
