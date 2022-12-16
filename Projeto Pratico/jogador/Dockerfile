FROM gradle:jdk17-alpine

COPY jogador /jogador

WORKDIR /jogador

RUN gradle installDist

CMD ["app/build/install/app/bin/app"]
