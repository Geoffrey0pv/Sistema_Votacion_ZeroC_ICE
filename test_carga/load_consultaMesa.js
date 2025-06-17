import http from 'k6/http';
import { check } from 'k6';

// Si tienes docs reales en JSON, descomenta esto y comenta el random:
import { SharedArray } from 'k6/data';
// const docs = new SharedArray('docs', () => JSON.parse(open('./docs.json')));

const MIN = 100000000;
const MAX = 999999999;

export const options = {
  scenarios: {
    consulta_mesa: {
      executor: 'constant-arrival-rate',
      rate: 6000,              // 6.000 peticiones por segundo
      timeUnit: '1s',
      duration: '10m',         // 10 minutos
      preAllocatedVUs: 1500,   // buen punto de arranque para 6k rps
      maxVUs: 3000,            // escala si toca
    },
  },
  thresholds: {
    'http_req_duration{status:200}': ['p(95)<500'], // 95% de las req deben responder en <500ms
    'http_req_failed': ['rate<0.01'],               // menos del 1% de errores
  },
};

export default function () {
  // 🔀 Elige entre lista real o random
  // const doc = docs[Math.floor(Math.random() * docs.length)];
  const doc = Math.floor(Math.random() * (MAX - MIN + 1)) + MIN;

  const payload = JSON.stringify({ documento: String(doc) });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      Connection: 'keep-alive',
    },
  };

  const res = http.post('http://10.147.17.110:9563/api/consulta', payload, params);

  check(res, {
    '200/404': r => r.status === 200 || r.status === 404,
  });
}
