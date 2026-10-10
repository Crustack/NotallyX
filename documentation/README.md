# NotallyX Documentation

This directory contains the source code for the official **NotallyX** user and developer documentation website, built with [Docusaurus 3](https://docusaurus.io/).

The documentation is automatically built and deployed to:
👉 **[https://crustack.github.io/NotallyX/](https://crustack.github.io/NotallyX/)**

---

## 🚀 Getting Started

### Prerequisites

- **Node.js**: `v18.0.0` or higher
- **Package Manager**: `yarn`

### Installation

Navigate to the `documentation` directory and install dependencies:

```bash
cd documentation
yarn install
```

---

## 🛠 Local Development

Start the local development server with live-reloading:

```bash
yarn start
```

This command starts a local dev server and opens a browser window at `http://localhost:3000`. Most changes to documentation files in `docs/` are reflected live without restarting the server.

---

## 📦 Building for Production

To compile the static production build of the website:

```bash
yarn build
```

This generates static HTML/JS assets in the `build/` directory.

### Preview Production Build Locally

Test the production build locally before deploying:

```bash
yarn serve
```

---

## 🧹 Useful Utility Commands

- **Clear Cache**: Clean generated build artifacts and bundler caches:
  ```bash
  yarn clear
  ```
- **Typecheck**: Verify TypeScript types across the documentation components:
  ```bash
  yarn typecheck
  ```
