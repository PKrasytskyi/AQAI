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

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl dumb-init gnupg unzip \
    && install -d -m 0755 /etc/apt/keyrings \
    && curl -fsSL https://dl.google.com/linux/linux_signing_key.pub | gpg --dearmor --yes -o /etc/apt/keyrings/google-chrome.gpg \
    && echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/google-chrome.gpg] https://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends google-chrome-stable \
    && chrome_major="$(google-chrome --product-version | cut -d. -f1)" \
    && driver_version="$(curl -fsSL "https://googlechromelabs.github.io/chrome-for-testing/LATEST_RELEASE_${chrome_major}")" \
    && curl -fsSL "https://storage.googleapis.com/chrome-for-testing-public/${driver_version}/linux64/chromedriver-linux64.zip" -o /tmp/chromedriver.zip \
    && unzip -q /tmp/chromedriver.zip -d /tmp \
    && install -m 0755 /tmp/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver \
    && rm -rf /tmp/chromedriver* /var/lib/apt/lists/* \
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
