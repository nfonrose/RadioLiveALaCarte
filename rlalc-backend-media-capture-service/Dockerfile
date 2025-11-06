FROM openjdk:23

# Install ffmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /opt/prtlabs/radioLiveALaCarte/

# Create logs directory
RUN mkdir -p /opt/prtlabs/radioLiveALaCarte/logs/

# Copy the JAR file
COPY ./build/libs/rlalc-backend-media-capture-service-1.0-SNAPSHOT.jar /opt/prtlabs/radioLiveALaCarte/rlalc-backend-media-capture-service.jar

# Set the entrypoint
ENTRYPOINT ["java", "-jar", "/opt/prtlabs/radioLiveALaCarte/rlalc-backend-media-capture-service.jar"]
