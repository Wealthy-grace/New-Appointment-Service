#FROM eclipse-temurin:17-jre-alpine
#
#WORKDIR /app
#
## Create non-root user for security
#RUN addgroup -S spring && adduser -S spring -G spring
#
## Copy the pre-built jar from your local build
#COPY build/libs/*.jar app.jar
#
## Change ownership to non-root user
#RUN chown -R spring:spring /app
#
#USER spring
#
## Expose port
#EXPOSE 8083
#
## Health check
#HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
#  CMD wget --quiet --tries=1 --spider http://localhost:8083/actuator/health || exit 1
#
## Run the application
#ENTRYPOINT ["java", \
#    "-Djava.security.egd=file:/dev/./urandom", \
#    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-default}", \
#    "-jar", \
#    "app.jar"]


# test the updated docker image

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the pre-built jar from your local build
COPY build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

USER spring

# Expose port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-default}", \
    "-jar", \
    "app.jar"]