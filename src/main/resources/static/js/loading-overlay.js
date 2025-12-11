/**
 * Reusable Loading Overlay Utility
 *
 * This utility provides a simple way to show a full-screen loading overlay
 * for long-running operations in the JobFit application.
 *
 * Usage:
 *
 * 1. Include the CSS and HTML in your page:
 *    - Copy the CSS from the <style> section (search for "Loading Overlay")
 *    - Copy the HTML overlay div (search for id="loadingOverlay")
 *
 * 2. Include this script in your page:
 *    <script src="/js/loading-overlay.js"></script>
 *
 * 3. Use the LoadingOverlay object:
 *
 *    // Show the overlay
 *    LoadingOverlay.show("Custom message...", "Optional subtitle");
 *
 *    // Show with default message
 *    LoadingOverlay.show();
 *
 *    // Hide the overlay (usually not needed as page navigation hides it automatically)
 *    LoadingOverlay.hide();
 *
 *    // Attach to buttons automatically (will show overlay on click)
 *    LoadingOverlay.attachToButtons('.action-btn');
 *
 *    // Attach to a specific button
 *    LoadingOverlay.attachToButton(document.getElementById('myButton'));
 */

const LoadingOverlay = (function() {
    'use strict';

    let overlay = null;
    let titleElement = null;
    let messageElement = null;

    /**
     * Initialize the overlay by finding the DOM elements
     */
    function init() {
        if (overlay) return; // Already initialized

        overlay = document.getElementById('loadingOverlay');
        if (!overlay) {
            console.warn('LoadingOverlay: overlay element not found. Make sure to include the overlay HTML in your page.');
            return;
        }

        titleElement = overlay.querySelector('h4');
        messageElement = overlay.querySelector('p');
    }

    /**
     * Show the loading overlay
     * @param {string} title - Optional title text (default: "Generating Your Results...")
     * @param {string} message - Optional message text (default: "This may take up to 30 seconds...")
     */
    function show(title, message) {
        init();

        if (!overlay) return;

        // Update text if provided
        if (title && titleElement) {
            titleElement.textContent = title;
        }
        if (message && messageElement) {
            messageElement.innerHTML = message;
        }

        // Show the overlay
        overlay.classList.add('active');

        // Prevent body scrolling
        document.body.style.overflow = 'hidden';
    }

    /**
     * Hide the loading overlay
     */
    function hide() {
        init();

        if (!overlay) return;

        overlay.classList.remove('active');

        // Restore body scrolling
        document.body.style.overflow = '';
    }

    /**
     * Attach the overlay to a button element
     * The overlay will show when the button is clicked
     * @param {HTMLElement} button - The button element
     * @param {string} title - Optional custom title
     * @param {string} message - Optional custom message
     */
    function attachToButton(button, title, message) {
        if (!button) return;

        button.addEventListener('click', function(e) {
            show(title, message);
        });
    }

    /**
     * Attach the overlay to multiple buttons matching a selector
     * @param {string} selector - CSS selector for buttons (e.g., '.action-btn')
     * @param {string} title - Optional custom title
     * @param {string} message - Optional custom message
     */
    function attachToButtons(selector, title, message) {
        const buttons = document.querySelectorAll(selector);
        buttons.forEach(button => attachToButton(button, title, message));
    }

    /**
     * Initialize on DOM ready
     */
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Public API
    return {
        show: show,
        hide: hide,
        attachToButton: attachToButton,
        attachToButtons: attachToButtons
    };
})();
