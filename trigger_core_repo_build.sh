#! /bin/bash

body='{
"request": {
  "branch":"master"
}}'

curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -H "Travis-API-Version: 3" \
  -H "Authorization: token KfyuQBqOE1K4Qm5bbfYMKw" \
  -d "$body" \
  https://api.travis-ci.com/repo/flygare%2FQvantel/requests
