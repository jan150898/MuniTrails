# TODO

## GPX uphill/downhill section ordering fix
- [ ] Update `GpxUploadController.detectSections()` to enforce alternating section boundaries:
  - UH ends where DH starts (or end of tour)
  - DH ends where UH starts (or end of tour)
  - Allow neutral/no-op segments between DH and UH
- [ ] Keep existing `addSectionIfMeaningful` filters to avoid noise
- [x] Run `mvn test` to ensure the build still passes

