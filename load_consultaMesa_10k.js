import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';

// -------  ❖  DOCUMENTOS  ❖  -------
// ①  usa lista real:
// const docs = new SharedArray('docs', () => JSON.parse(open('./docs.json')));

// ②  ó genera números aleatorios en un rango:
const MIN = 100000000;
const MAX = 999999999;

export const options = {
    scenarios: {
      consulta_mesa: {
        executor: 'constant-arrival-rate',
        rate: 2000,              // 2 000 peticiones/seg
        timeUnit: '1s',
        duration: '2m',
        preAllocatedVUs: 500,    // suficiente para 2k rps
        maxVUs: 1000,
      },
    },
  };

export default function () {
  // ► usa lista:
  // const doc = docs[Math.floor(Math.random() * docs.length)];

  // ► o aleatorio:
  const doc = Math.floor(Math.random() * (MAX - MIN + 1)) + MIN;

  const payload = JSON.stringify({ documento: String(doc) });
  const params  = { headers: { 'Content-Type': 'application/json', Connection: 'keep-alive' } };

  const res = http.post('http://localhost:8080/api/consulta', payload, params);
  check(res, { '200/404': r => r.status === 200 || r.status === 404 });
}
