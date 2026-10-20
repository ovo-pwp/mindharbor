#!/usr/bin/env bash
# 接口冒烟测试：覆盖登录 / 测评 / AI / 推荐 / 记录等核心链路
BASE=http://localhost:8080
PY="C:/Users/Administrator/.workbuddy/binaries/python/versions/3.13.12/python.exe"
PASS=0
FAIL=0

j() { "$PY" -c "import sys,json;d=json.load(sys.stdin);print(eval(sys.argv[1]))" "$1"; }

check() {
  local name="$1" expect="$2" actual="$3"
  if [ "$expect" = "$actual" ]; then
    echo "  [PASS] $name -> $actual"
    PASS=$((PASS+1))
  else
    echo "  [FAIL] $name -> 期望 $expect 实际 $actual"
    FAIL=$((FAIL+1))
  fi
}

login() {
  curl -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"123456\"}"
}

echo "=== 1. 登录 ==="
ADMIN=$(login admin)
ADMIN_TOKEN=$(echo "$ADMIN" | j "d['data']['token']")
check "管理员登录" "200" "$(echo "$ADMIN" | j "d['code']")"

PAT=$(login zhangxiaoyu)
PAT_TOKEN=$(echo "$PAT" | j "d['data']['token']")
check "患者登录" "200" "$(echo "$PAT" | j "d['code']")"

DOC=$(login liyisheng)
DOC_TOKEN=$(echo "$DOC" | j "d['data']['token']")
check "医生登录" "200" "$(echo "$DOC" | j "d['code']")"

AH="Authorization: Bearer $ADMIN_TOKEN"
PH="Authorization: Bearer $PAT_TOKEN"
DH="Authorization: Bearer $DOC_TOKEN"

echo "=== 2. 未登录拦截 ==="
NOAUTH=$(curl -s "$BASE/api/user/profile")
check "无 token 访问受限接口" "401" "$(echo "$NOAUTH" | j "d['code']")"

echo "=== 3. 患者端 ==="
check "获取测评量表" "2" "$(curl -s "$BASE/api/assessments" -H "$PH" | j "len(d['data'])")"
check "情绪趋势" "200" "$(curl -s "$BASE/api/symptoms/trend?days=30" -H "$PH" | j "d['code']")"
check "推荐资源" "200" "$(curl -s "$BASE/api/content/recommend?limit=4" -H "$PH" | j "d['code']")"
check "我的报告" "1" "$(curl -s "$BASE/api/reports/mine" -H "$PH" | j "len(d['data'])")"
check "咨询师列表" "200" "$(curl -s "$BASE/api/doctors?page=1&size=9" -H "$PH" | j "d['code']")"
check "会话列表" "1" "$(curl -s "$BASE/api/consult/conversations" -H "$PH" | j "len(d['data'])")"

echo "=== 4. 提交测评（SDS 20 题全选 2）==="
ANS=$("$PY" -c "import json;print(json.dumps({'answers':[2]*20}))")
SUB=$(curl -s -X POST "$BASE/api/assessments/1/submit" -H "$PH" -H "Content-Type: application/json" -d "$ANS")
check "提交测评" "200" "$(echo "$SUB" | j "d['code']")"
echo "     标准分 $(echo "$SUB" | j "d['data']['standardScore']") 等级 $(echo "$SUB" | j "d['data']['level']")"

echo "=== 5. AI 对话（SSE）==="
SSE=$(curl -s -N -X POST "$BASE/api/ai/chat" -H "$PH" -H "Content-Type: application/json" \
  -d '{"content":"最近总是失眠，晚上脑子停不下来"}' --max-time 25)
if echo "$SSE" | grep -q '"type":"delta"'; then
  echo "  [PASS] SSE 流式返回"
  PASS=$((PASS+1))
else
  echo "  [FAIL] SSE 无 delta 事件"
  FAIL=$((FAIL+1))
fi
if echo "$SSE" | grep -q '"type":"done"'; then
  echo "  [PASS] SSE 流结束"
  PASS=$((PASS+1))
else
  echo "  [FAIL] SSE 未收到 done"
  FAIL=$((FAIL+1))
fi

echo "=== 6. 医生端 ==="
check "待审申请" "200" "$(curl -s "$BASE/api/doctor/workbench/applications" -H "$DH" | j "d['code']")"
check "我的患者" "1" "$(curl -s "$BASE/api/doctor/workbench/patients" -H "$DH" | j "len(d['data'])")"
check "报告列表" "1" "$(curl -s "$BASE/api/doctor/workbench/reports" -H "$DH" | j "len(d['data'])")"
PID=$(curl -s "$BASE/api/doctor/workbench/patients" -H "$DH" | j "d['data'][0]['userId']")
check "患者档案" "200" "$(curl -s "$BASE/api/doctor/workbench/patients/$PID/overview" -H "$DH" | j "d['code']")"
DRAFT=$(curl -s -G "$BASE/api/doctor/workbench/reports/ai-draft" --data-urlencode "userId=$PID" \
  --data-urlencode "name=张小雨" -H "$DH" --max-time 40)
check "AI 生成报告草稿" "200" "$(echo "$DRAFT" | j "d['code']")"

echo "=== 7. 管理端 ==="
check "仪表盘" "200" "$(curl -s "$BASE/api/admin/dashboard" -H "$AH" | j "d['code']")"
check "用户列表" "200" "$(curl -s "$BASE/api/admin/users?page=1&size=10" -H "$AH" | j "d['code']")"
check "量表管理" "2" "$(curl -s "$BASE/api/admin/assessments" -H "$AH" | j "len(d['data'])")"
check "科普文章" "8" "$(curl -s "$BASE/api/admin/articles" -H "$AH" | j "len(d['data'])")"
check "患者访问管理端被拒" "403" "$(curl -s "$BASE/api/admin/dashboard" -H "$PH" | j "d['code']")"

echo
echo "================ 结果 ================"
echo "通过 $PASS / 失败 $FAIL"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
