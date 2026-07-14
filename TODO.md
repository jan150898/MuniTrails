# TODO - Difficulty (MTB Singletrail skala S0..S5)

- [ ] Add enum type `MTBSingletrailSkala` (S0..S5) to `com.example.trails.model`
- [ ] Add field(s) to `GPXTrack` for MTB Singletrail difficulty
  - [ ] Ensure it supports “two different numbers too” (second field)
- [ ] Update database schema (Flyway migration)
  - [ ] Add column(s) to `gpx_track`
- [ ] Update REST DTOs
  - [ ] Add difficulty field(s) to `CreateTrackRequest`
  - [ ] Add difficulty field(s) to `TrackResponse`
- [ ] Update `TrackService` create/update mapping from request DTO -> entity
- [ ] Compile + run tests / build (mvn test or mvn package)

