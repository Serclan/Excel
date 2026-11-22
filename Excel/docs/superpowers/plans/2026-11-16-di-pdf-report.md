# DI PDF Report Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Tasks are performed inline in the current session; do not delegate.

**Goal:** Integrate the teacher's PDF report assignment into the existing sports-reporting Java Swing application and prepare the required examples and explanation.

**Architecture:** Keep `Excel` as the Swing entry point, add a focused `GeneradorPDF` that receives selected domain objects and filtered data, and use the existing `CalculosDeportivos` for regression/statistical calculations. Use only Maven dependency `org.apache.pdfbox:pdfbox-app:3.0.1` from the teacher's notes; charts may be drawn with Java2D and embedded using PDFBox, with no chart framework.

## Global Constraints

- Use `pdfbox-app` version `3.0.1`, as shown in `DI\Excel y PDF\Informes PDF.pdf`.
- Reuse `Deportista`, `Entrenamiento`, `PruebaRendimiento`, and `CalculosDeportivos`; do not fork or change existing indicators.
- Keep the report between 3 and 5 A4 pages and include all mandatory sections from the second-part brief.
- Add only API calls needed for the assignment requirements not shown in the introductory notes; do not add external libraries.
- Keep generated school artifacts inside `E:\NetBeansProjects\Excel\entrega-di2`, not Downloads.
- No unrelated UI redesign or refactor.

---

## File map

- Modify `pom.xml`: add the PDFBox 3.0.1 dependency.
- Create `src/main/java/com/mycompany/excel/ResumenInforme.java`: immutable aggregate of date-filtered sessions, weekly totals, filtered tests, and summary indicators.
- Create `src/main/java/com/mycompany/excel/GeneradorPDF.java`: PDF pages, fonts, tables, charts, image placement, pagination, and file save.
- Modify `src/main/java/com/mycompany/excel/Excel.java`: add athlete/date selection, validation, PDF save flow, and one additional athlete's sample sessions/tests.
- Create `entrega-di2/Explicacion.pdf.txt`: one-page explanation text for the submission.
- Create `entrega-di2/Informe_DEP01.pdf` and `Informe_DEP02.pdf`: distinct generated examples.
- Create `entrega-di2/tarea2_nombre_apellidos.zip`: source and required deliverables in the requested archive format.

### Task 1: Add PDFBox 3.0.1 and report data aggregation

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/mycompany/excel/ResumenInforme.java`

**Interfaces:**
- `ResumenInforme.crear(Deportista, LocalDate desde, LocalDate hasta, List<Entrenamiento>, List<PruebaRendimiento>)` filters inclusive date ranges and produces session count, km, minutes, load, best 5k, weekly totals, and selected athlete test history.
- Weekly records expose week start, km, minutes, load, session count, and 3-week moving load average when available.

- [ ] Add Maven dependency `org.apache.pdfbox:pdfbox-app:3.0.1`.
- [ ] Implement inclusive date filtering for sessions and 5k tests.
- [ ] Aggregate weekly values by Monday-start weeks; preserve weeks with no sessions between the first and last selected week as zero values.
- [ ] Reuse `CalculosDeportivos.regresionLineal` when there are at least two selected 5k tests; otherwise mark trend as insufficient data.
- [ ] Reject an empty session-and-test range with a clear validation result for the caller.

### Task 2: Implement the PDF generator

**Files:**
- Create: `src/main/java/com/mycompany/excel/GeneradorPDF.java`

**Interface:**
- `static void generar(ResumenInforme informe, File destino) throws IOException`

- [ ] Create and save a `PDDocument` and A4 pages using the teacher's constructor/save patterns.
- [ ] Load an included TTF font and a bold variant from project resources; use at least three sizes and clear heading/body styles.
- [ ] Add page 1 cover and summary: club name, report title, athlete name/category/goal, selected period, generation date, representative image, sessions, km, minutes, total load, and best 5k.
- [ ] Add page 2 weekly training table and weekly-load chart, with legible header, numeric alignment, and moving average when at least three weeks exist.
- [ ] Add page 3 performance table and 5k evolution chart; include date, time, delta vs first test, and regression line when calculable.
- [ ] Add page 4 cautious interpretation, indicator limitations, source note, and footer/page numbering; keep all content within consistent margins.
- [ ] Wrap text by measured font width and prevent drawing past page margins; add a page when a section does not fit.
- [ ] Create chart PNGs with Java2D and insert them with PDFBox; retain aspect ratio and fall back to a generic running illustration if no athlete photo exists.
- [ ] Close content streams/documents and surface I/O errors to the UI caller.

### Task 3: Connect PDF export to Swing and support two athletes

**Files:**
- Modify: `src/main/java/com/mycompany/excel/Excel.java`

- [ ] Add a second export button labeled `Generar informe PDF` without changing the current Excel export.
- [ ] Provide a `JComboBox<Deportista>` and two date spinners initialized to the available date range; display dates as `yyyy-MM-dd`.
- [ ] Validate that the start date is not after the end date and that the selected interval has reportable data.
- [ ] Open `JFileChooser`, invoke `ResumenInforme.crear` and `GeneradorPDF.generar`, then show success or readable error dialogs.
- [ ] Add coherent sample training and at least two 5k tests for `DEP02`, so two different athletes produce different reports.

### Task 4: Prepare submission artifacts

**Files:**
- Create: `entrega-di2/Explicacion.pdf.txt`
- Create: `entrega-di2/Informe_DEP01.pdf`
- Create: `entrega-di2/Informe_DEP02.pdf`
- Create: `entrega-di2/tarea2_nombre_apellidos.zip`

- [ ] Write a concise explanation (maximum one page when rendered) covering Swing selection, data reuse, image/chart insertion, and page control.
- [ ] Run the application and generate one report for `DEP01` and one for `DEP02` with the same period; confirm each names the correct athlete and varies its own data.
- [ ] Render both output PDFs to images and inspect page count, text clipping, table boundaries, chart labels, aspect ratios, and footers.
- [ ] Package Java source, the two example reports, explanation, and evidence images in the archive; exclude `target/` build output and local machine paths.

## Self-review

Coverage maps to the brief's selection, validation, personalization, 3–5 page report, text/font, images, tables, two charts, pagination, error handling, dynamic athlete examples, explanation, and archive. No external charting or image library is introduced. Project has no Git repository, so no commit task applies. The two generated athlete PDFs are assignment evidence, not a separate automated test suite.
