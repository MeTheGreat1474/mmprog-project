# Darkroom Atelier Dashboard 
### Current Capabilities & Features

**Darkroom Atelier** is an aesthetically refined, feature-rich photo management dashboard built using JavaFX. Designed specifically for professional creatives, it leverages an embedded SQLite persistence layer to manage local high-resolution file imports intuitively. 

Here is everything the application can currently do:

---

### 1. Photo Library Management
- **Seamless Local Importer:** Easily populate the platform with RAW/JPEG files by clicking the "Drag & Drop or Click to Browse" prompt block natively hooked into the OS-level File chooser.
- **Dynamic Flexible Grid Rendering:** The application mathematically detects window dimension changes and collapses/expands the main picture grid automatically (between 1-4 columns), guaranteeing thumbnails never break out of their structural bounds.
- **Aliasing Suppression System:** The application bypasses brute-force pixel resizing by relying on native hardware image readers, importing extreme resolution photography rapidly as cleanly downsampled 300px preview icons without choking system memory allocations.
- **Embedded SQL Metadata Tracker:** Every file dragged into the application permanently links its OS datapath inside a localized hidden SQLite database (`~/DarkroomLibrary/library_catalog.db`), retaining session memory universally across boots. 
- **Tooltips on Hover:** Exposes internal system image path file names organically anytime a user's mouse rests inside an image grid cell slot. 

### 2. Annotation & Editorial Marking (DB Persistence)
- **Fluid Sidebar Tracking:** Clicking any item within the core library viewport seamlessly fires rapid UI updates, displaying a crisp 500-pixel resolution preview scaling along the right-hand inspection dock. 
- **Persistent Text Note-Taking:** Photographers can write robust project details, processing intents, or editorial cues inside the `Annotation Area`. Leveraging strict SQLite bindings, these notes instantly save to the SQL map upon pressing "Save Annotations."
- **Instant Clear Memory Dumps:** Utilizing the "Clear" button completely blanks the text interface while rigorously executing `DELETE FROM annotations` in the backend to safely flush database caches completely out of scope. 
- **Visual Heart Badges (❤️):** Anytime a user attaches textual data metrics to an image file, a stylized pink heart with a dense white CSS vector-styled drop shadow perfectly aligns onto the corner wrapper of the native image. Selecting it or pressing clear visibly strips the object locally from the DOM tree, all entirely synchronous and persistent entirely through application reloads.

### 3. Full-Resolution Modal Viewing
- **Action Triggers:** Images can easily be popped out into an unconstrained viewport via a double-click on any library grid item, or by targeting the "Open Full Image Viewer" navigation bar option.
- **Unconstrained Resolution Resampling:** The app opens isolated `Stage` windows that load the absolute uncompressed `URI` stream from the parent OS disk layer, rendering 100% full detail with zero interpolation artifacts for maximum zoom validation. All windows freely map to their native window drag scales dynamically.
