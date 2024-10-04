FROM liquibase/liquibase
USER liquibase
ADD --chown=liquibase:liquibase db /liquibase/changelog/

CMD ["sh", "-c", "docker-entrypoint.sh --url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB} --username=${POSTGRES_USER} --password=${POSTGRES_PASSWORD} --liquibase-schema-name=liquibase --classpath=/liquibase/changelog --changeLogFile=changelog.main.yaml update"]