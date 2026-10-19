const sid = process.argv[2] || 'test-001';
const url = `ws://127.0.0.1:8080/ws/${sid}`;
const ws = new WebSocket(url);

ws.onopen = () => {
  console.log('CONNECTED ' + url);
};
ws.onmessage = (e) => {
  console.log('RECV: ' + e.data);
};
ws.onerror = (e) => {
  console.log('ERROR: ' + (e.message || e.type));
};
ws.onclose = (e) => {
  console.log('CLOSED code=' + e.code);
};

// 20 秒后自动退出
setTimeout(() => {
  console.log('LISTEN_DONE');
  process.exit(0);
}, 20000);
