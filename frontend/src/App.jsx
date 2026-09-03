import { useState, useEffect } from 'react';
import axios from 'axios';

const API = 'http://localhost:8080/api';

export default function App() {
  const [summary, setSummary] = useState(null);
  const [cases, setCases] = useState([]);
  const [experiment, setExperiment] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      axios.get(`${API}/dashboard/summary`),
      axios.get(`${API}/recovery/cases`),
      axios.get(`${API}/experiments/results`)
    ]).then(([s, c, e]) => {
      setSummary(s.data);
      setCases(c.data);
      setExperiment(e.data[e.data.length - 1]);
      setLoading(false);
    });
  }, []);

  if (loading) return <div style={{ padding: 40 }}>Loading...</div>;

  return (
    <div style={{ fontFamily: 'sans-serif', padding: 32, maxWidth: 1100, margin: '0 auto' }}>
      <h1>RecoverIQ — Revenue Recovery Dashboard</h1>

      <div style={{ display: 'flex', gap: 16, marginBottom: 32, flexWrap: 'wrap' }}>
        <Card label="Open Cases" value={summary.totalOpenCases} />
        <Card label="Amount at Risk" value={`₹${Number(summary.totalAmountAtRisk).toLocaleString()}`} />
        <Card label="Expected Recovery" value={`₹${Number(summary.totalExpectedRecovery).toLocaleString()}`} />
      </div>

      {experiment && (
        <div style={{ background: '#f0f9ff', padding: 20, borderRadius: 8, marginBottom: 32 }}>
          <h2>Latest Experiment Results</h2>
          <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
            <Stat label="Treatment Recovery Rate" value={`${(experiment.treatmentRecoveryRate * 100).toFixed(0)}%`} />
            <Stat label="Control Recovery Rate" value={`${(experiment.controlRecoveryRate * 100).toFixed(0)}%`} />
            <Stat label="Incremental Lift" value={`+${(experiment.incrementalLift * 100).toFixed(0)}%`} highlight />
            <Stat label="Actual Recovered" value={`₹${Number(experiment.totalActualRecovered).toLocaleString()}`} />
            <Stat label="Interventions" value={experiment.interventionCount} />
            <Stat label="Cost / Recovery" value={`₹${experiment.costPerRecovery}`} />
          </div>
        </div>
      )}

      <h2>Recovery Queue (ranked by expected value)</h2>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #ddd', textAlign: 'left' }}>
            <th style={th}>Customer</th>
            <th style={th}>Amount at Risk</th>
            <th style={th}>Failure</th>
            <th style={th}>Risk</th>
            <th style={th}>Probability</th>
            <th style={th}>Expected Recovery</th>
            <th style={th}>Status</th>
          </tr>
        </thead>
        <tbody>
          {cases.map(c => (
            <tr key={c.id} style={{ borderBottom: '1px solid #eee' }}>
              <td style={td}>{c.customerName}</td>
              <td style={td}>₹{Number(c.amountAtRisk).toLocaleString()}</td>
              <td style={td}>{c.failureCategory}</td>
              <td style={td}><Badge level={c.riskLevel} /></td>
              <td style={td}>{(c.recoveryProbability * 100).toFixed(0)}%</td>
              <td style={td}>₹{Number(c.expectedRecovery).toLocaleString()}</td>
              <td style={td}>{c.status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

const th = { padding: '8px 12px' };
const td = { padding: '8px 12px' };

function Card({ label, value }) {
  return (
    <div style={{ background: '#fff', border: '1px solid #e5e5e5', borderRadius: 8, padding: 16, minWidth: 180 }}>
      <div style={{ fontSize: 13, color: '#666' }}>{label}</div>
      <div style={{ fontSize: 24, fontWeight: 700 }}>{value}</div>
    </div>
  );
}

function Stat({ label, value, highlight }) {
  return (
    <div>
      <div style={{ fontSize: 12, color: '#666' }}>{label}</div>
      <div style={{ fontSize: 20, fontWeight: 700, color: highlight ? '#16a34a' : '#111' }}>{value}</div>
    </div>
  );
}

function Badge({ level }) {
  const colors = { HIGH: '#dc2626', MEDIUM: '#d97706', LOW: '#16a34a' };
  return <span style={{ color: colors[level], fontWeight: 600 }}>{level}</span>;
}