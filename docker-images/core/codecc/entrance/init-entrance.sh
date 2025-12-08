#!/bin/bash
JWT_TOKEN=$(bash /data/workspace/bk-ci-gen-jwt-token.sh $BK_CI_JWT_RSA_PRIVATE_KEY 'init-codecc')
curl -X PUT --header 'Content-Type: application/json' --header 'Accept: application/json' --header "X-DEVOPS-JWT-TOKEN: $JWT_TOKEN" --header 'X-DEVOPS-UID: admin' -d "{
   \"showProjectList\": true,   
   \"showNav\": true,   
   \"status\": \"ok\",   
   \"deleted\": false,          
   \"link\": \"/codecc/0\",                   
   \"linkNew\": \"/codecc/0\",   
   \"iframeUrl\": \"$BK_CODECC_URL\"  
 }" "http://$BK_CI_PROJECT_INNER_URL/api/op/services/update/CodeCC"