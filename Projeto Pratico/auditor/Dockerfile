FROM gradle:jdk17-alpine

COPY auditor /auditor

WORKDIR /auditor

RUN gradle installDist

CMD ["sh", "-c", "app/build/install/app/bin/app ${TAMANHO} ${BANDEIRAS}"]