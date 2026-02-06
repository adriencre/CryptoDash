/**
 * Polyfill pour les libs type Node (ex. sockjs-client) qui utilisent `global`
 * alors qu'il n'existe pas en navigateur.
 */
const g = typeof globalThis !== 'undefined' ? globalThis : typeof window !== 'undefined' ? window : (typeof self !== 'undefined' ? self : {});
const r = g as Record<string, unknown>;
if (typeof r['global'] === 'undefined') {
  r['global'] = g;
}
