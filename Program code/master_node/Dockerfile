# Use an official Java JDK runtime as a base image
FROM openjdk:8-jdk-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the java file into the container
COPY server1.java /app

# Compile the java file
RUN javac server1.java

# Command to run the java class
CMD ["java", "server1"]

# Expose the port the server listens on
EXPOSE 5040
