import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Configuración de la prueba de carga
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp up: 10 usuarios virtuales
    { duration: '1m', target: 30 },    // Sostener: 30 usuarios
    { duration: '30s', target: 50 },   // Pico: 50 usuarios
    { duration: '30s', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% de requests < 2s (RNF-001)
    http_req_failed: ['rate<0.01'],    // < 1% de errores
    'checks{type:login}': ['rate>0.99'],
    'checks{type:register}': ['rate>0.95'],
  },
};

// Métricas personalizadas
const loginFailRate = new Rate('login_fail_rate');
const registerFailRate = new Rate('register_fail_rate');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_URL = `${BASE_URL}/api/v1`;

// Credenciales de prueba (admin por defecto)
const ADMIN_USER = __ENV.ADMIN_USER || 'admin';
const ADMIN_PASS = __ENV.ADMIN_PASS || 'Admin12345678!';

export function setup() {
  // Login y obtener token
  const loginRes = http.post(`${API_URL}/auth/login`, JSON.stringify({
    username: ADMIN_USER,
    password: ADMIN_PASS
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  check(loginRes, { 'login successful': (r) => r.status === 200 });
  
  if (loginRes.status !== 200) {
    throw new Error(`Login failed: ${loginRes.status} ${loginRes.body}`);
  }

  const token = loginRes.json('accessToken');
  return { token };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  // 1. Verificar health del backend
  const healthRes = http.get(`${BASE_URL}/actuator/health`, { headers });
  check(healthRes, { 'health check ok': (r) => r.status === 200 });

  // 2. Obtener sesiones activas (para obtener sesionId)
  const sesionesRes = http.get(`${API_URL}/sesiones/activas`, { headers });
  check(sesionesRes, { 'sesiones activas ok': (r) => r.status === 200 });

  let sesionId = null;
  if (sesionesRes.status === 200) {
    const sesiones = sesionesRes.json();
    if (sesiones.length > 0) {
      sesionId = sesiones[0].id;
    }
  }

  // 3. Obtener estudiantes para probar registro
  const estudiantesRes = http.get(`${API_URL}/estudiantes?activo=true`, { headers });
  check(estudiantesRes, { 'estudiantes ok': (r) => r.status === 200 });

  let codigoEstudiante = null;
  if (estudiantesRes.status === 200) {
    const estudiantes = estudiantesRes.json();
    if (estudiantes.length > 0) {
      // Usar el código del primer estudiante
      codigoEstudiante = estudiantes[0].codigo;
    }
  }

  // 4. Probar registro de asistencia (si hay sesión y estudiante)
  if (sesionId && codigoEstudiante) {
    const registerRes = http.post(`${API_URL}/asistencias/registrar`, JSON.stringify({
      sesionId: sesionId,
      codigo: codigoEstudiante,
      metodo: 'CODIGO'
    }), { headers });

    const registerSuccess = check(registerRes, {
      'registro asistencia ok': (r) => r.status === 200 || r.status === 409, // 409 = duplicado es OK
    });
    
    registerFailRate.add(!registerSuccess);
    
    if (registerRes.status === 200) {
      const body = registerRes.json();
      check(body, { 'response has success': (b) => b.success === true });
    }
  }

  // 5. Probar dashboard docente
  const dashboardRes = http.get(`${API_URL}/dashboard/docente`, { headers });
  check(dashboardRes, { 'dashboard ok': (r) => r.status === 200 });

  sleep(1); // Pausa entre iteraciones
}

export function teardown(data) {
  // Cleanup si es necesario
  console.log('Load test completed');
}