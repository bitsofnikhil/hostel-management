@echo off
REM Clean target only. This keeps local H2 data and registered users.
call mvn clean spring-boot:run
