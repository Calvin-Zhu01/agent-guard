# 创建 Agent:
curl -X POST http://localhost:8080/api/v1/agents \
  -H "Content-Type: application/json" \
  -d '{"name":"测试Agent","type":"CUSTOMER_SERVICE","department":"技术部","environment":"TEST","description":"测试用Agent"}'

# 查询 Agent 列表
curl "http://localhost:8080/api/v1/agents?current=1&size=10"

# 获取 Agent 详情 (替换 {id} 为实际 ID)
curl http://localhost:8080/api/v1/agents/{id}

# 更新 Agent
curl -X PUT http://localhost:8080/api/v1/agents/{id} \
  -H "Content-Type: application/json" \
  -d '{"name":"更新后的Agent","type":"FINANCE"}'

# 删除 Agent
curl -X DELETE http://localhost:8080/api/v1/agents/{id}

# 测试正常请求 (金额 <= 1000，应该通过)
curl -X POST http://localhost:8080/proxy/v1/request \
  -H "Content-Type: application/json" \
  -d '{"apiKey":"ag_xxx","targetUrl":"https://api.example.com/payment","method":"POST","body":{"amount":500}}'

# 测试拦截请求 (包含 /transfer 且金额 > 1000，应该被拦截)
curl -X POST http://localhost:8080/proxy/v1/request \
  -H "Content-Type: application/json" \
  -d '{"apiKey":"ag_xxx","targetUrl":"https://api.example.com/transfer","method":"POST","body":{"amount":1500,"to":"account123"}}'
