# Fix: Track status should be DRAFT (not PUBLISHED) on creation

## Goal
Fix test failures where `GPXTrackStatus.DRAFT` is expected but `PUBLISHED` is set.

## Steps
- [x] Plan approved
- [x] 1. Edit `TrackService.java` - change `setStatus(PUBLISHED)` → `setStatus(DRAFT)`
- [x] 2. Edit `GpxUploadController.java` - change `setStatus(PUBLISHED)` → `setStatus(DRAFT)` in `saveTrack()`
- [x] 3. Edit `GarminController.java` - change `setStatus(PUBLISHED)` → `setStatus(DRAFT)` for main track and sections
- [x] 4. Run tests to verify

