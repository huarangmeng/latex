// Keep startup failures readable instead of leaving the loading spinner running.
// After Compose mounts its shadow DOM, it owns the preview and its error handling.
function showStartupError() {
    const root = document.getElementById("compose-root");
    if (root.shadowRoot) return;

    root.querySelector(".loading").classList.add("failed");
    document.getElementById("loading-message").textContent =
        "The demo could not start. Please reload, check your connection, or try a current browser with WebAssembly GC support.";
}

window.addEventListener("error", showStartupError, true);
window.addEventListener("unhandledrejection", showStartupError);
