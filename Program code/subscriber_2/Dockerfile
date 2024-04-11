# Use an official Java JDK runtime as a base image
FROM openjdk:8-jdk-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the entire project directory (or just the necessary files) into the container at /app
COPY . /app

# Compile the java file (assuming Subscriber2.java is your Java file)
RUN javac subscriber2.java

# Command to run the java class with arguments
CMD ["java", "subscriber2", "c-subscriber_02-Economic Indicators,Global Events"]

# Expose the port that the subscriber listens on, if it's a server application
EXPOSE 5000