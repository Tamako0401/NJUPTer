# NJUPTer

NJUPTer is an Android timetable app built for NJUPT students with Jetpack Compose.
It supports manual course editing, multi-timetable management, per-timetable configuration,
and one-click import from the NJUPT JWXT system.

## Features

- Weekly timetable view with horizontal paging by week
- Manual course creation, editing, and deletion
- Custom course details: name, teacher, classroom, day, sections, weeks, and card color
- Multi-timetable management with local persistence
- Per-timetable settings:
  - timetable name
  - semester start date
  - total teaching weeks
  - whether to show weekends
  - custom session start/end times
- Remember last selected timetable between app launches
- Empty-state guide for first-time use
- Import from NJUPT JWXT:
  - login through built-in WebView
  - capture session cookie and student id after unified authentication
  - fetch timetable page from `jwxt.njupt.edu.cn`
  - parse course name, teacher, classroom, day, sections, and weeks
  - create a brand new timetable from imported data

## Tech Stack

- Kotlin
- Android SDK / Jetpack Compose
- Material 3
- Kotlin Coroutines + Flow
- Gson for local JSON persistence
- OkHttp for network requests
- JSoup for HTML parsing

## Project Structure

```text
app/src/main/java/com/example/njupter/
|- data/           Local persistence, repository, models, import client
|- domain/         Date utilities, validation, import matching
|- ui/             Compose screens, dialogs, components
|- MainActivity.kt App entry and screen composition
```

Key modules:

- `data/LocalFileDataSource.kt`: stores timetable index and timetable JSON files in app local storage
- `data/FileTimetableRepository.kt`: keeps in-memory state and syncs changes to file storage
- `ui/TimetableScreen.kt`: main weekly timetable screen
- `ui/TimetableConfigDialog.kt`: create/edit timetable metadata and session times
- `ui/import/JwxtImportScreen.kt`: WebView-based JWXT login flow
- `data/import/JwxtClient.kt`: requests JWXT timetable HTML
- `data/import/JwxtParser.kt`: parses timetable HTML into temporary remote course models
- `domain/import/TimetableImportMatcher.kt`: converts imported data into local course/session models

## Screens and Workflow

### 1. Timetable

- View the current timetable by week
- Swipe horizontally to switch weeks
- Jump directly to a specific week
- Open the timetable selector from the top bar
- Add or edit a course from the floating action button or by tapping an existing course card

### 2. Timetable Management

- Create multiple timetables
- Switch between timetables
- Each timetable stores its own metadata and course data independently
- The app restores the last selected timetable on next launch

### 3. Settings

- Edit the current timetable name
- Set semester start date
- Set total weeks
- Toggle weekend visibility
- Edit all 12 session time ranges

### 4. JWXT Import

- Start from the new timetable dialog
- Tap the import shortcut button
- Complete NJUPT unified authentication in WebView
- The app captures login cookies and student id from the redirected JWXT page
- It requests the timetable page and parses course information automatically
- A preview dialog lets you name the new timetable before import completes

## Data Storage

The app currently uses local file storage instead of a database.

- Timetable index is stored as a JSON file
- Each timetable is stored as an individual JSON file
- App settings, including the last selected timetable id, are stored in `SharedPreferences`

This design keeps the project lightweight and easy to understand for learning and iteration.

## Requirements

- Android Studio with Android SDK installed
- JDK 11
- Android device or emulator
- Minimum SDK: 24
- Target SDK: 36

## Build and Run

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Run the `app` configuration on an emulator or real device

You can also build from the command line:

```bash
./gradlew assembleDebug
```

On Windows:

```bash
gradlew.bat assembleDebug
```

## Permissions and Network Notes

- The app requests `INTERNET` permission
- JWXT import uses HTTP access to `jwxt.njupt.edu.cn`
- `network_security_config.xml` explicitly allows cleartext traffic for the required NJUPT domains

## Current Limitations

- JWXT import is tailored to the current HTML structure of the NJUPT JWXT timetable page; if the site changes, parsing may need updates
- Data is stored only locally on the device and is not synced across devices
- There is no export, backup, or cloud sync yet
- There are currently no dedicated automated tests for the import pipeline

## Suitable Use Cases

- NJUPT students who want a lightweight local timetable app
- Learning projects for Android architecture with Compose + repository + local JSON persistence
- Experiments around timetable parsing and campus system integration

## Future Directions

- Export and import local timetable files
- Conflict detection and smarter merge strategies
- Better import error feedback and loading states
- Widget support
- Backup and restore
- More complete test coverage

## License

No license file is currently included in this repository.
If you plan to open source the project publicly, consider adding a license such as MIT.
