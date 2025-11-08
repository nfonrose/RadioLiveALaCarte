FROM eclipse-temurin:23

# Install ffmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /opt/prtlabs/radiolivealacarte/

# Create logs directory
RUN mkdir -p /opt/prtlabs/radiolivealacarte/logs/

# Copy the JAR file
COPY ./build/libs/rlalc-backend-media-capture-service-0.1-SNAPSHOT.jar /opt/prtlabs/radiolivealacarte/rlalc-backend-media-capture-service.jar

# Set the entrypoint
ENTRYPOINT ["java", "-jar", "/opt/prtlabs/radiolivealacarte/rlalc-backend-media-capture-service.jar"]
