# Start from a Java JDK base image
FROM openjdk:8-jdk-alpine

# Set the working directory to /app inside the container
WORKDIR /app

# Copy your Java application into the container
COPY . /app

# Compile your Java application
RUN javac server2.java

# Run your Java application with the required command-line argument
CMD ["java", "server2", "s-server2"]

# Expose the port your application listens on
EXPOSE 5041