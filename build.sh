mkdir -p /kaniko/.docker
echo "{\"auths\":{\"${HARBOR_HOST:-$CI_REGISTRY}\":{\"auth\":\"$(printf "%s:%s" "${HARBOR_USERNAME:-$CI_REGISTRY_USER}" "${HARBOR_PASSWORD:-$CI_REGISTRY_PASSWORD}" | base64 | tr -d '\n')\"}}}" > /kaniko/.docker/config.json

echo "Building images..."
TAG=$CI_COMMIT_REF_NAME
VERSION=$(eval "$BUILD_VERSION")

for f in docker-images/*.dockerfile
do
  echo "...................................$f..................................."
  [ -e "$f" ] || continue
  FILENAME=$(basename "${f%%.*}" | cut -c 4-)
  DEST="${HARBOR_HOST}/${HARBOR_PROJECT}"
  if [ "$DEST" == "/" ]; then
    DEST=$CI_REGISTRY_IMAGE
  fi
  echo "Building ${FILENAME}..."
  /kaniko/executor \
    --context "${CI_PROJECT_DIR}" \
    --dockerfile "${f}" \
    --destination "${DEST}/${FILENAME}:${TAG}" \
    --build-arg VERSION="$(eval "$BUILD_VERSION")" \
    --cleanup
  echo "Done building & pushing ${FILENAME}"
done
