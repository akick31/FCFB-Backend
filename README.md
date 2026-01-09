# FCFB Arceus

**FCFB Arceus** is a Spring Boot service with a REST API created for the **Fake College Football** game. The service acts as the backend for the game, handling game logic, player actions, team management, and real-time updates. It integrates with the larger **FCFB ecosystem**, including the **FCFB Discord Bot** and **FCFB Website**.

The service is responsible for processing game data, simulating plays, updating player stats, and managing the game state. It exposes endpoints that allow the **FCFB Discord Bot** to send requests and receive updates, enabling seamless communication between the game and Discord.

---

## Features
- **REST API**: Built with Spring Boot, the service provides a RESTful API for interacting with the game.
- **Game Logic**: Handles game logic, including play simulation, player actions, and team management.
- **Team Management**: Allows players to create, manage, and customize their teams.
- **User Management**: Supports user registration, login, and authentication.
- **Stats Tracking**: Tracks player stats, team stats, and game stats in real-time.
- **Season Management**: Manages game seasons, schedules, and standings.
- **Rankings Management**: Manages team rankings
- **Discord Integration**: Connects with the **FCFB Discord Bot** for player interaction and game updates.

---

## Reporting Bugs or Issues
To report bugs or submit feature requests:
- Feel free to submit an issue directly via Github.

---

## Setup Instructions
### 1. Prerequisites
- **Java Development Kit (JDK)**: Version 17 or higher.
- **Gradle**: Installed on your system for building the project.
- **Discord Bot Token**: You’ll need a bot token from the Discord Developer Portal, the same one used for FCFB-Discord-Bot
- **Application Properties**: A pre-configured application.properties file with sensitive information.

### 2. Clone the Repository
Clone the project to your local machine:
```bash
git clone https://github.com/akick31/fcfb-arceus.git
cd fcfb-arceus
```

### 3. Configure Application Properties

The application requires an application.properties file for configuration. This file contains sensitive information, including the bot token, database connection info, and encryption tokens.

To get the required application.properties file:
- Contact me directly to receive a pre-configured file.

Once you have the file:
1.	Place it in the src/main/resources directory.
2.	Double-check the values for correctness.

# 4. Build the Project

Use Gradle to build the project:
```bash
gradle build
```

# 5. Run the Application

After building, run the application with:
```bash
gradle run
```

# 6. Verify
- Confirm it’s online and responding to endpoint requests.

---

## Development Notes
- **Frontend Integration**: The application is tightly integrated with [FCFB-Discord-Ref-Bot](https://github.com/akick31/FCFB-Discord-Ref-Bot), but information from the Discord bot can be manually sent via Postman or other REST clients. The Discord bot displays the information on Discord for users.
- **Exposed Endpoint**: The application exposes several REST endpoints that allow the Discord bot to send requests to it. Ensure proper networking configuration if both are not running on the same machine.
- **Database**: The application uses a MariaDB database to store game, team, user, and stat data. Make sure the database is running and accessible if you’re testing the service.

---

## Contributing
Contributions are welcome! If you’d like to contribute to the project, please follow these guidelines:
- **Fork the Repository**: Create your own fork of the project.
- **Create a Branch**: Make your changes in a new branch.
- **Commit Changes**: Commit your changes with clear messages.
- **Push Changes**: Push your changes to your fork.
- **Submit a Pull Request**: Create a new pull request with your changes.
- **Code Review**: Your changes will be reviewed, and feedback will be provided.
- **Merge Changes**: Once approved, your changes will be merged into the main branch.

---

Feel free to reach out with any questions or concerns. Happy coding!

---

## License
This project is licensed under the MIT License. See the LICENSE file for more information.
