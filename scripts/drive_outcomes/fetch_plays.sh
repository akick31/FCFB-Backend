#!/bin/sh
set -eu
API="${1:-https://fakecollegefootball.com/api/v1/arceus}"
OUT="${2:-plays}"
mkdir -p "$OUT"
page=0
: > "$OUT/game_ids.txt"
while :; do
  body=$(curl -sf "$API/game?category=PAST&size=500&page=$page")
  echo "$body" | python3 -c 'import json,sys; [print(g["game_id"]) for g in json.load(sys.stdin)["content"]]' >> "$OUT/game_ids.txt"
  echo "$body" | python3 -c 'import json,sys; sys.exit(0 if json.load(sys.stdin)["last"] else 1)' && break
  page=$((page + 1))
done
xargs -P 4 -I{} sh -c "test -s '$OUT/{}.json' || curl -sf --max-time 60 '$API/play/all?gameId={}' -o '$OUT/{}.json'" < "$OUT/game_ids.txt"
