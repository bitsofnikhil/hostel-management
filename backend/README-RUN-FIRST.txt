RUN FROM THIS FOLDER ONLY:
backend

Normal run:
mvn clean spring-boot:run

If dashboard/API fails after many code changes, reset local database once:
reset-local-db-and-run.bat

First account created from login page becomes ADMIN.
Next accounts become SUPERVISOR.

PWA/mobile:
- Open http://YOUR_LAPTOP_IP:8080 on phone.
- Install from Chrome menu.
- Saving data needs backend server running.

Deployment:
- Use PostgreSQL, not H2, for real data.
- Set SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD, SPRING_DATASOURCE_DRIVER.
