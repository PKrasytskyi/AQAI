# Build the AQAI runtime once so reviewers do not need Maven, Java, or browser setup.
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
COPY demo ./demo
COPY requirements ./requirements
COPY runtime-skills ./runtime-skills
COPY scripts/docker-entrypoint.sh ./scripts/docker-entrypoint.sh

RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM maven:3.9.9-eclipse-temurin-17

ARG AQAI_UID=10001
ARG CHROME_VERSION=148.0.7778.178

ENV CHROME_BIN=/opt/chrome/chrome
ENV CHROMEDRIVER_PATH=/usr/local/bin/chromedriver
ENV JAVA_TOOL_OPTIONS="-Dwebdriver.chrome.driver=/usr/local/bin/chromedriver"

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        dumb-init \
        unzip \
        fonts-liberation \
        libasound2t64 \
        libatk-bridge2.0-0 \
        libatk1.0-0 \
        libcairo2 \
        libcups2t64 \
        libdbus-1-3 \
        libexpat1 \
        libfontconfig1 \
        libgbm1 \
        libglib2.0-0 \
        libgtk-3-0t64 \
        libnspr4 \
        libnss3 \
        libpango-1.0-0 \
        libx11-6 \
        libx11-xcb1 \
        libxcb1 \
        libxcomposite1 \
        libxdamage1 \
        libxext6 \
        libxfixes3 \
        libxrandr2 \
        libxrender1 \
        libxss1 \
        xdg-utils \
    && curl -fsSL \
        "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_VERSION}/linux64/chrome-linux64.zip" \
        -o /tmp/chrome.zip \
    && unzip -q /tmp/chrome.zip -d /opt \
    && mv /opt/chrome-linux64 /opt/chrome \
    && ln -s /opt/chrome/chrome /usr/local/bin/google-chrome \
    && curl -fsSL \
        "https://storage.googleapis.com/chrome-for-testing-public/${CHROME_VERSION}/linux64/chromedriver-linux64.zip" \
        -o /tmp/chromedriver.zip \
    && unzip -q /tmp/chromedriver.zip -d /tmp \
    && install -m 0755 \
        /tmp/chromedriver-linux64/chromedriver \
        /usr/local/bin/chromedriver \
    && rm -rf \
        /tmp/chrome.zip \
        /tmp/chromedriver.zip \
        /tmp/chromedriver-linux64 \
        /var/lib/apt/lists/* \
    && useradd --create-home --uid ${AQAI_UID} --shell /bin/sh aqai \
    && google-chrome --version \
    && chromedriver --version

WORKDIR /opt/aqai

COPY --from=build --chown=aqai:aqai /workspace /opt/aqai
COPY --from=build --chown=aqai:aqai /root/.m2/repository /opt/aqai/.m2/repository

RUN chmod +x /opt/aqai/scripts/docker-entrypoint.sh \
    && mkdir -p /artifacts \
    && chown -R aqai:aqai /opt/aqai /artifacts

ENV AQAI_DEMO=orangehrm \
    AQAI_CONTAINER=true \
    AQAI_ARTIFACTS_DIR=/artifacts \
    MAVEN_CONFIG=/opt/aqai/.m2 \
    MAVEN_OPTS="-Duser.home=/opt/aqai -Dmaven.repo.local=/opt/aqai/.m2/repository" \
    JAVA_TOOL_OPTIONS="-Dwebdriver.chrome.driver=/usr/local/bin/chromedriver"

USER aqai

ENTRYPOINT ["/usr/bin/dumb-init", "--", "/opt/aqai/scripts/docker-entrypoint.sh"]
