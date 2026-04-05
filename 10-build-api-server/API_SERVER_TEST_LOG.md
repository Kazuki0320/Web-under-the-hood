# 10 API Server Test Log
Sun Apr  5 22:43:05 JST 2026

## /health
HTTP/1.1 200 OK
Content-TypeL: application/json; charset=UTF-8
Content-Length: 15
Connection: close

{"status":"ok"}
## /hello
HTTP/1.1 200 OK
Content-TypeL: application/json; charset=UTF-8
Content-Length: 19
Connection: close

{"message":"hello"}
## /not-found
HTTP/1.1 404 Not Found
Content-TypeL: application/json; charset=UTF-8
Content-Length: 21
Connection: close

{"error":"not found"}