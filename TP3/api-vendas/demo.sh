#!/usr/bin/env bash
# Roteiro de demonstracao do TP3 (requer curl e jq).
# Uso: ./demo.sh   (com todos os servicos ja em execucao)
AUTH=${AUTH:-http://localhost:9000}
PRODUTOS=${PRODUTOS:-http://localhost:8082}
VENDAS=${VENDAS:-http://localhost:8083}
JSON='Content-Type: application/json'

passo() { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
req()   { curl -s -w '\n[HTTP %{http_code}]\n' "$@"; }

passo "1. Acesso a rota protegida SEM token (esperado: 401)"
req "$PRODUTOS/produtos"

passo "2. Login com credenciais INVALIDAS (esperado: 401)"
req -H "$JSON" -d '{"username":"admin","password":"senha-errada"}' "$AUTH/auth/login"

passo "3. Login com credenciais validas (esperado: 200 + accessToken e refreshToken)"
RESP=$(curl -s -H "$JSON" -d '{"username":"admin","password":"admin123"}' "$AUTH/auth/login")
echo "$RESP" | jq .
ACCESS=$(echo "$RESP" | jq -r .accessToken)
REFRESH=$(echo "$RESP" | jq -r .refreshToken)

passo "4. Acesso a rota protegida COM o access token (esperado: 200)"
req -H "Authorization: Bearer $ACCESS" "$PRODUTOS/produtos"
req -H "Authorization: Bearer $ACCESS" "$PRODUTOS/produtos/me"

passo "5. Token adulterado / invalido (esperado: 401)"
req -H "Authorization: Bearer ${ACCESS}xyz" "$PRODUTOS/produtos"

passo "6. Refresh: obtendo um NOVO access token a partir do refresh token (esperado: 200)"
sleep 1
RESP2=$(curl -s -H "$JSON" -d "{\"refreshToken\":\"$REFRESH\"}" "$AUTH/auth/refresh")
echo "$RESP2" | jq .
NOVO_ACCESS=$(echo "$RESP2" | jq -r .accessToken)
[ "$NOVO_ACCESS" != "$ACCESS" ] && echo "-> novo access token e diferente do anterior"

passo "7. Acesso a rota protegida com o NOVO access token (esperado: 200)"
req -H "Authorization: Bearer $NOVO_ACCESS" "$PRODUTOS/produtos/1"

passo "8. Refresh com token invalido (esperado: 401)"
req -H "$JSON" -d '{"refreshToken":"token-invalido"}' "$AUTH/auth/refresh"

passo "9. Refresh token usado como access token (esperado: 401)"
req -H "Authorization: Bearer $REFRESH" "$PRODUTOS/produtos"

passo "10. Autorizacao por perfil: POST /produtos como USER (esperado: 403)"
USER_TOKEN=$(curl -s -H "$JSON" -d '{"username":"usuario","password":"usuario123"}' "$AUTH/auth/login" | jq -r .accessToken)
req -H "$JSON" -H "Authorization: Bearer $USER_TOKEN" -d '{"nome":"Pendrive 64GB","preco":39.90}' "$PRODUTOS/produtos"

passo "11. POST /produtos como ADMIN (esperado: 201)"
req -H "$JSON" -H "Authorization: Bearer $NOVO_ACCESS" -d '{"nome":"Pendrive 64GB","preco":39.90}' "$PRODUTOS/produtos"

passo "12. Registrar venda SEM token (esperado: 401)"
req -H "$JSON" -d '{"idProduto":1,"quantidade":2}' "$VENDAS/vendas"

passo "13. Registrar venda COM token (esperado: 201)"
echo "    vendas-service -> WebClient (reativo, via Eureka) -> GET produtos-service/produtos/1 com o mesmo token"
req -H "$JSON" -H "Authorization: Bearer $USER_TOKEN" -d '{"idProduto":1,"quantidade":2}' "$VENDAS/vendas"

passo "14. Registrar venda de produto inexistente (esperado: 422)"
req -H "$JSON" -H "Authorization: Bearer $USER_TOKEN" -d '{"idProduto":999,"quantidade":1}' "$VENDAS/vendas"

passo "15. Listar as vendas do usuario autenticado (esperado: 200)"
req -H "Authorization: Bearer $USER_TOKEN" "$VENDAS/vendas"
