#!/usr/bin/env bash
# 把 GitHub Release 的 APK 同步到 Gitee Release。
# 需要环境变量：GITEE_TOKEN、TAG、APK_PATH；可选：APK_NAME、BODY_FILE、PRERELEASE、GITHUB_SHA。
set -euo pipefail

summary_file="${GITHUB_STEP_SUMMARY:-/dev/null}"

if [[ -z "${GITEE_TOKEN:-}" ]]; then
  echo "::warning::未配置 GITEE_TOKEN，跳过 Gitee 发布。"
  {
    echo "## Gitee"
    echo ""
    echo "未配置 GITEE_TOKEN，本次没有发布到 Gitee。"
    echo "如需自动同步安装包，请在仓库 Secrets 中添加 GITEE_TOKEN。"
  } >> "$summary_file"
  exit 0
fi

owner="${GITEE_OWNER:?GITEE_OWNER 未设置}"
repo="${GITEE_REPO:?GITEE_REPO 未设置}"
tag="${TAG:?TAG 未设置}"
apk="${APK_PATH:?APK_PATH 未设置}"
asset_name="${APK_NAME:-$(basename "$apk")}"
body_file="${BODY_FILE:-}"
prerelease="${PRERELEASE:-false}"
target="${TARGET_SHA:-${GITHUB_SHA:-main}}"
api="https://gitee.com/api/v5/repos/${owner}/${repo}"

if [[ ! -f "$apk" ]]; then
  echo "APK 不存在：$apk" >&2
  exit 1
fi

body=""
if [[ -n "$body_file" && -f "$body_file" ]]; then
  body=$(cat "$body_file")
fi
if [[ -z "$body" ]]; then
  body="${tag} 的安装包，由 GitHub Actions 自动构建。"
fi

tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT

status=$(curl -sS -o "$tmp" -w '%{http_code}' \
  "${api}/releases/tags/${tag}?access_token=${GITEE_TOKEN}")

if [[ "$status" != "200" && "$status" != "404" ]]; then
  echo "读取 Gitee Release 失败（HTTP ${status}）：" >&2
  cat "$tmp" >&2
  exit 1
fi

release_id=$(jq -r '.id // empty' "$tmp" 2>/dev/null || true)

if [[ -n "$release_id" ]]; then
  echo "Gitee Release ${tag} 已存在（id=${release_id}）"
else
  # Gitee 对不存在的 release 可能返回 404，也可能返回 200 + null。
  echo "在 Gitee 创建 Release ${tag}"
  args=(
    --data-urlencode "access_token=${GITEE_TOKEN}"
    --data-urlencode "tag_name=${tag}"
    --data-urlencode "name=${tag}"
    --data-urlencode "body=${body}"
    --data-urlencode "target_commitish=${target}"
  )
  if [[ "$prerelease" == "true" ]]; then
    args+=(--data-urlencode "prerelease=true")
  fi
  curl -fsS -X POST "${api}/releases" "${args[@]}" -o "$tmp"
  release_id=$(jq -r '.id // empty' "$tmp")
  if [[ -z "$release_id" ]]; then
    echo "Gitee 创建 Release 失败：" >&2
    cat "$tmp" >&2
    exit 1
  fi
fi

# 删除同名旧附件，保证重复运行也能更新文件。
curl -fsS "${api}/releases/${release_id}/attach_files?per_page=100&access_token=${GITEE_TOKEN}" -o "$tmp"
while IFS= read -r asset_id; do
  [[ -z "$asset_id" ]] && continue
  echo "删除 Gitee 上的同名附件（id=${asset_id}）"
  curl -fsS -X DELETE \
    "${api}/releases/${release_id}/attach_files/${asset_id}?access_token=${GITEE_TOKEN}" \
    -o /dev/null
done < <(jq -r --arg name "$asset_name" '.[] | select(.name == $name) | .id' "$tmp")

curl -fsS -X POST "${api}/releases/${release_id}/attach_files" \
  -F "access_token=${GITEE_TOKEN}" \
  -F "file=@${apk};type=application/vnd.android.package-archive" \
  -o "$tmp"

url=$(jq -r '.browser_download_url // empty' "$tmp")
if [[ -z "$url" ]]; then
  echo "Gitee 上传附件后未返回下载地址：" >&2
  cat "$tmp" >&2
  exit 1
fi

echo "Gitee 发布完成：${url}"
{
  echo "## Gitee"
  echo ""
  echo "- Release: ${tag}"
  echo "- APK: ${url}"
} >> "$summary_file"
