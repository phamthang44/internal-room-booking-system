```markdown
# internal-room-booking-system Development Patterns

> Auto-generated skill from repository analysis

## Overview
This skill teaches you the development conventions and workflows used in the `internal-room-booking-system` Java codebase. You'll learn about file naming, import/export styles, commit patterns, and how to write and run tests. This guide is ideal for contributors aiming for consistency and maintainability in this repository.

## Coding Conventions

### File Naming
- **Convention:** PascalCase for all file names.
- **Example:**  
  ```
  RoomBookingService.java
  UserManager.java
  BookingController.java
  ```

### Import Style
- **Convention:** Use relative imports for referencing other classes within the project.
- **Example:**
  ```java
  import com.company.booking.RoomBookingService;
  import com.company.user.UserManager;
  ```

### Export Style
- **Convention:** Use named exports (Java's `public` classes and methods).
- **Example:**
  ```java
  public class RoomBookingService {
      public void bookRoom(Room room) { ... }
  }
  ```

### Commit Patterns
- **Convention:** Use [Conventional Commits](https://www.conventionalcommits.org/) with the `fix` prefix for bug fixes.
- **Example:**
  ```
  fix: correct date validation logic in BookingController
  ```

## Workflows

### Code Fix Workflow
**Trigger:** When you need to fix a bug or issue in the codebase  
**Command:** `/fix-bug`

1. Identify and fix the bug in the relevant Java file.
2. Ensure the file name follows PascalCase.
3. Use relative imports for any new dependencies.
4. Write a commit message starting with `fix:` followed by a concise description.
5. Push your changes and open a pull request.

### Add New Feature Workflow
**Trigger:** When adding a new feature or module  
**Command:** `/add-feature`

1. Create new Java files using PascalCase naming.
2. Use relative imports for referencing other classes.
3. Export new classes and methods as `public`.
4. Write or update tests in files matching `*.test.*`.
5. Commit changes with a descriptive message (e.g., `feat: add user notification system`).
6. Push and open a pull request.

## Testing Patterns

- **Test Files:** Use the pattern `*.test.*` for test files (e.g., `RoomBookingService.test.java`).
- **Framework:** The specific testing framework is unknown; check existing test files for structure.
- **Example:**
  ```java
  public class RoomBookingServiceTest {
      @Test
      public void testBookRoom() {
          // test logic here
      }
  }
  ```
- **Tip:** Place tests alongside or in a dedicated test directory, following the file naming convention.

## Commands
| Command      | Purpose                                    |
|--------------|--------------------------------------------|
| /fix-bug     | Start the code fix workflow                |
| /add-feature | Start the new feature development workflow |
```
