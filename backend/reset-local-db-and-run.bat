@echo off
REM WARNING: deletes local H2 database, users, students, attendance, rooms, mess and complaints.
if exist data rmdir /s /q data
if exist target rmdir /s /q target
call mvn clean spring-boot:run
