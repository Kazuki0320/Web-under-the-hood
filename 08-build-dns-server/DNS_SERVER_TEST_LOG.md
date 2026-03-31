# 08 DNS Server Log
Tue Mar 31 00:01:20 JST 2026

## A query
;; Warning: Message parser reports malformed message packet.

; <<>> DiG 9.10.6 <<>> @127.0.0.1 -p 10053 my.local A
; (1 server found)
;; global options: +cmd
;; Got answer:
;; WARNING: .local is reserved for Multicast DNS
;; You are currently testing what happens when an mDNS query is leaked to DNS
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 56186
;; flags: qr rd ra; QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 1

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 4096
;; QUESTION SECTION:
;my.local.			IN	A

;; ADDITIONAL SECTION:
my.local.		60	IN	A	127.0.0.1

;; Query time: 3 msec
;; SERVER: 127.0.0.1#10053(127.0.0.1)
;; WHEN: Tue Mar 31 00:01:20 JST 2026
;; MSG SIZE  rcvd: 53


## AAAA query

; <<>> DiG 9.10.6 <<>> @127.0.0.1 -p 10053 my.local AAAA
; (1 server found)
;; global options: +cmd
;; connection timed out; no servers could be reached
