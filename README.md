# Hostel Management System — Cleaned & Extended

This is the same uploaded project structure, cleaned and extended. It is still a Spring Boot backend with static HTML/CSS/JS frontend.

## Added

- Excel/PDF import for students
- Excel/PDF import for attendance
- Night attendance round page for warden/supervisor room-to-room marking
- Attendance supports PRESENT / ABSENT / LEAVE
- Room complaint box directly under each student during attendance
- Common hostel complaint while marking attendance
- Room allocation during admission
- Room allocation synchronized with attendance
- Auto-room allocation when room is blank
- Room block support with default capacity 3 and variable capacity per block/room
- Separate complaint categories: ROOM, HOSTEL, MESS, ATTENDANCE, OTHER
- Mess menu management
- Seasonal mess menu suggestions
- H2 zero-setup local database by default
- Default authorized users: admin, warden, supervisor

## Run

```powershell
cd "D:\Hostel-Management-System-copilot-add-hostel-management-platform\backend"
mvn clean spring-boot:run
```

Open:

```text
http://localhost:8080
```

## Logins

| Role | Username | Password |
|---|---|---|
| Admin | admin | Admin@123 |
| Warden | warden | Warden@123 |
| Supervisor | supervisor | Supervisor@123 |

## Student Import Format

Excel/PDF columns:

```text
registrationNo, name, fatherName, phone, email, address, block, roomNumber(optional), block(optional)
```

If `roomNumber` is blank, the app auto-allocates the first room with a free bed. If `block` is blank, block A is used. Default room capacity is 3, but each room can be edited to a different capacity.

## Night Attendance

Open `Attendance` from the sidebar. It shows rooms by block using current room allocation. Supervisor can mark a whole room present/absent/leave or mark individual students and write room complaints.

## Attendance Import Format

Excel/PDF columns:

```text
date, registrationNo, status, roomComplaint, commonComplaint
```

Status values:

```text
PRESENT, ABSENT, LEAVE
```

PDF import works only for text-based PDFs with comma-separated rows. Scanned image PDFs need OCR and are not supported.

## Sample Files

See:

```text
sample-files/student-import-template.xlsx
sample-files/attendance-import-template.xlsx
```

## Main Folder

Run Maven from:

```text
backend
```

That is where `pom.xml` is.

## Attendance update

The attendance page now supports:
- room-wise night attendance
- individual student attendance
- one-click save for visible attendance
- export attendance to Excel
- export attendance to PDF

Attendance import controls were removed from the attendance UI. Import backend endpoints are still kept in the code so older data files can still be loaded if required.


## Latest UI changes

- Attendance page has no bulk attendance import card.
- Attendance page keeps Save Visible Attendance, Individual Student Attendance, Export Excel, and Export PDF.
- Common and room-wise complaints are displayed on Dashboard separately.
- Room complaints entered during attendance appear under Dashboard room-wise complaints.


## PWA / Mobile Support

This build includes Progressive Web App support for mobile use:

- `manifest.webmanifest` for installable app behavior.
- `sw.js` service worker for cached static pages.
- App icons in `backend/src/main/resources/static/icons/`.
- Mobile responsive sidebar and screen-size adaptive layout.

Run the backend and open `http://localhost:8080` on the phone through the same network using the computer IP, for example:

```text
http://192.168.1.10:8080
```

For Android/Chrome: open the app URL, tap menu, then tap **Add to Home screen** or **Install app**.

Saving attendance, students, rooms, complaints and mess menu still requires connection to the running backend server. Cached pages can load offline, but database updates cannot be saved offline in this build.
