#!/usr/bin/env bash
# Roteiro de testes de todos os endpoints (rodar com a aplicação no ar).
# No Windows, rode pelo Git Bash: ./testes.sh
# Também serve para gerar dados para o dashboard do Grafana.

URL=${URL:-http://localhost:8080}
JSON="Content-Type: application/json"

passo() { echo; echo "=== $1"; }

passo "1) Login válido (joao/joao123) -> 200"
curl -s -w "  [HTTP %{http_code}]\n" -X POST "$URL/login" -H "$JSON" -d '{"username":"joao","senha":"joao123"}'

passo "2) Login válido (maria/maria123) -> 200"
curl -s -w "  [HTTP %{http_code}]\n" -X POST "$URL/login" -H "$JSON" -d '{"username":"maria","senha":"maria123"}'

passo "3) Login inválido, usuário inexistente -> 401"
curl -s -w "  [HTTP %{http_code}]\n" -X POST "$URL/login" -H "$JSON" -d '{"username":"fulano","senha":"123"}'

passo "4) Maria erra a senha 1x -> 401"
curl -s -w "  [HTTP %{http_code}]\n" -X POST "$URL/login" -H "$JSON" -d '{"username":"maria","senha":"errada"}'

passo "5) Maria erra a senha 2x -> 401 e fica BLOQUEADA"
curl -s -w "  [HTTP %{http_code}]\n" -X POST "$URL/login" -H "$JSON" -d '{"username":"maria","senha":"errada"}'

passo "6) Maria tenta com a senha certa, mas está bloqueada -> 401"
curl -s -w "  [HTTP %{http_code}]\n" -X POST "$URL/login" -H "$JSON" -d '{"username":"maria","senha":"maria123"}'

passo "7) Listar usuários com ROLE_MANUTENCAO -> 200"
curl -s -w "  [HTTP %{http_code}]\n" -u manutencao:manut123 "$URL/"

passo "8) Listar usuários sem credencial -> 401"
curl -s -o /dev/null -w "  [HTTP %{http_code}]\n" "$URL/"

passo "9) Listar usuários com ROLE_USER (joao) -> 403"
curl -s -o /dev/null -w "  [HTTP %{http_code}]\n" -u joao:joao123 "$URL/"

passo "10) Cadastrar usuário com ROLE_MANUTENCAO -> 201"
curl -s -w "  [HTTP %{http_code}]\n" -u manutencao:manut123 -X POST "$URL/" -H "$JSON" -d '{"username":"ana","senha":"ana123"}'

passo "11) Cadastrar usuário repetido -> 409"
curl -s -w "  [HTTP %{http_code}]\n" -u manutencao:manut123 -X POST "$URL/" -H "$JSON" -d '{"username":"ana","senha":"ana123"}'

passo "12) Cadastrar com admin (sem ROLE_MANUTENCAO) -> 403"
curl -s -o /dev/null -w "  [HTTP %{http_code}]\n" -u admin:admin123 -X POST "$URL/" -H "$JSON" -d '{"username":"bia","senha":"bia123"}'

passo "13) Login com o usuário novo (ana) -> 200"
curl -s -w "  [HTTP %{http_code}]\n" -X POST "$URL/login" -H "$JSON" -d '{"username":"ana","senha":"ana123"}'

passo "14) Trocar senha da ana com a senha atual errada -> 401"
curl -s -w "  [HTTP %{http_code}]\n" -X PUT "$URL/trocarsenha/ana" -H "$JSON" -d '{"atual":"xxx","nova":"nova123"}'

passo "15) Trocar senha da ana -> 200"
curl -s -w "  [HTTP %{http_code}]\n" -X PUT "$URL/trocarsenha/ana" -H "$JSON" -d '{"atual":"ana123","nova":"nova123"}'

passo "16) Login da ana com a senha nova -> 200"
curl -s -w "  [HTTP %{http_code}]\n" -X POST "$URL/login" -H "$JSON" -d '{"username":"ana","senha":"nova123"}'

passo "17) Desbloquear maria com ROLE_MANUTENCAO (não é admin) -> 403"
curl -s -o /dev/null -w "  [HTTP %{http_code}]\n" -u manutencao:manut123 -X PUT "$URL/unlock/maria"

passo "18) Desbloquear maria com ROLE_ADMIN -> 200"
curl -s -w "  [HTTP %{http_code}]\n" -u admin:admin123 -X PUT "$URL/unlock/maria"

passo "19) Maria loga de novo -> 200"
curl -s -w "  [HTTP %{http_code}]\n" -X POST "$URL/login" -H "$JSON" -d '{"username":"maria","senha":"maria123"}'

passo "20) Métricas no formato do Prometheus"
curl -s "$URL/actuator/prometheus" | grep -E "^(login_validos_total|login_invalidos_total|usuarios_bloqueados)"
