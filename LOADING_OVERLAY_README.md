# Loading Overlay Implementation

This document describes the loading overlay implementation for long-running dashboard actions in the JobFit application.

## Overview

The loading overlay provides visual feedback to users when they trigger long-running operations (such as generating suggestions, improving fit scores, upgrading CVs, or preparing for interviews). It displays a full-screen modal with a spinner and informative text while the operation is in progress.

## Features

- ✅ **Full-screen blocking overlay** - Prevents user interaction during operations
- ✅ **Spinner animation** - Visual indication that work is in progress
- ✅ **User-friendly messaging** - Clear instructions and time estimates
- ✅ **Instant feedback** - Shows immediately on button click
- ✅ **Automatic dismissal** - Hides when the new page loads
- ✅ **Reusable components** - Easy to use on any page
- ✅ **Bootstrap compatible** - Works with the existing design system
- ✅ **Plain JavaScript** - No additional frameworks required

## Implementation Details

### Dashboard Actions

The loading overlay is implemented on the dashboard (`/dashboard`) for the following actions:

1. **Suggestions** (<40% fit score) - Career direction recommendations
2. **Improve Fit** (40-74% fit score) - Improvement recommendations
3. **Upgrade CV** (75-85% fit score) - CV tailoring for the role
4. **Get Ready** (>85% fit score) - Interview preparation content

### Files Modified

- **`src/main/resources/templates/dashboard.html`**
  - Added CSS styles for the loading overlay (lines 97-144)
  - Added `action-btn` class and `data-action` attributes to action buttons (lines 216-219)
  - Added loading overlay HTML structure (lines 231-238)
  - Added JavaScript to show overlay on button clicks (lines 240-256)

### Reusable Components Created

For easy reuse across other pages, the following standalone components were created:

1. **`src/main/resources/static/css/loading-overlay.css`**
   - Standalone CSS file with all overlay styles
   - Includes responsive design for mobile devices
   - Includes animations and transitions

2. **`src/main/resources/static/js/loading-overlay.js`**
   - JavaScript utility with a simple API
   - Methods: `show()`, `hide()`, `attachToButton()`, `attachToButtons()`
   - Comprehensive inline documentation

3. **`src/main/resources/static/loading-overlay-example.html`**
   - Example page demonstrating usage
   - Multiple examples with different scenarios
   - Copy-paste code snippets

## How It Works

### Current Dashboard Implementation

1. User clicks an action button (Suggestions, Improve Score, etc.)
2. JavaScript event listener detects the click
3. Loading overlay is shown immediately with `.active` class
4. Browser navigates to the action URL (e.g., `/suggestions/123`)
5. Server processes the request (may take ~30 seconds if not cached)
6. Server returns the result page or redirects
7. New page loads, replacing the current page
8. Overlay is automatically hidden (page replacement)

### Flow Diagram

```
User clicks action button
        ↓
JavaScript shows overlay immediately
        ↓
Browser navigates to action URL
        ↓
Server checks cache
        ↓
   Cache Hit?
   ↙        ↘
  Yes        No
   ↓          ↓
Returns    Invokes Agent
instantly  (~30 seconds)
   ↓          ↓
   └─────┬────┘
         ↓
   Returns result page
         ↓
   Overlay hidden (new page loads)
```

## Usage on New Pages

To add the loading overlay to other pages, follow these steps:

### Option 1: Inline Implementation (like dashboard.html)

1. **Add CSS** to the `<style>` section:
```html
<style>
    /* Copy CSS from dashboard.html lines 97-144 */
    .loading-overlay { /* ... */ }
    .loading-overlay.active { /* ... */ }
    .loading-content { /* ... */ }
    .loading-spinner { /* ... */ }
    /* ... etc ... */
</style>
```

2. **Add HTML** before closing `</body>`:
```html
<div id="loadingOverlay" class="loading-overlay">
    <div class="loading-content">
        <div class="loading-spinner"></div>
        <h4>Action In Progress...</h4>
        <p>This may take up to 1 minute.<br>Please don't close this window.</p>
    </div>
</div>
```

3. **Add JavaScript** before closing `</body>`:
```html
<script>
    document.addEventListener('DOMContentLoaded', function() {
        const overlay = document.getElementById('loadingOverlay');
        const actionButtons = document.querySelectorAll('.action-btn');

        actionButtons.forEach(button => {
            button.addEventListener('click', function() {
                overlay.classList.add('active');
            });
        });
    });
</script>
```

4. **Add class to buttons**:
```html
<a class="btn btn-primary action-btn" href="/some-action">Action Button</a>
```

### Option 2: Using Reusable Components (Recommended)

1. **Include CSS**:
```html
<link rel="stylesheet" href="/css/loading-overlay.css">
```

2. **Add HTML** before closing `</body>`:
```html
<div id="loadingOverlay" class="loading-overlay">
    <div class="loading-content">
        <div class="loading-spinner"></div>
        <h4>Action In Progress...</h4>
        <p>This may take up to 1 minute.<br>Please don't close this window.</p>
    </div>
</div>
```

3. **Include JavaScript**:
```html
<script src="/js/loading-overlay.js"></script>
```

4. **Use the API**:
```html
<script>
    // Automatically attach to all buttons with class 'action-btn'
    LoadingOverlay.attachToButtons('.action-btn');

    // Or attach to a specific button with custom message
    LoadingOverlay.attachToButton(
        document.getElementById('myButton'),
        'Processing Request...',
        'This may take a moment.'
    );

    // Or manually control
    document.getElementById('myButton').addEventListener('click', function() {
        LoadingOverlay.show('Custom Title', 'Custom message');
    });
</script>
```

## Customization

### Changing the Message

You can customize the overlay message in two ways:

**1. Update the HTML directly:**
```html
<div id="loadingOverlay" class="loading-overlay">
    <div class="loading-content">
        <div class="loading-spinner"></div>
        <h4>Your Custom Title</h4>
        <p>Your custom message here.</p>
    </div>
</div>
```

**2. Use JavaScript (with reusable components):**
```javascript
LoadingOverlay.show('Processing...', 'Please wait a moment.');
```

### Changing the Colors

Edit the CSS to match your brand:

```css
.loading-spinner {
    border: 5px solid #e0e7ff;           /* Light color */
    border-top: 5px solid #667eea;       /* Brand color */
}

.loading-content h4 {
    color: #2d3748;                      /* Title color */
}

.loading-content p {
    color: #718096;                      /* Message color */
}
```

### Changing the Animation Speed

```css
.loading-spinner {
    animation: spin 1s linear infinite;   /* Change 1s to your preferred speed */
}
```

## Browser Compatibility

The loading overlay works in all modern browsers:
- ✅ Chrome/Edge (latest)
- ✅ Firefox (latest)
- ✅ Safari (latest)
- ✅ Mobile browsers (iOS Safari, Chrome Mobile)

Note: The `backdrop-filter: blur()` effect may not work in older browsers, but the overlay will still function correctly.

## Performance Considerations

- The overlay uses CSS animations (GPU accelerated)
- No external dependencies or libraries required
- Minimal JavaScript footprint (~3KB uncompressed)
- CSS is ~2KB uncompressed

## Testing

To test the loading overlay:

1. Navigate to the dashboard: `http://localhost:8080/dashboard`
2. Click any of the action buttons (Suggestions, Improve Score, etc.)
3. Verify the overlay appears immediately
4. Wait for the result page to load
5. Verify the overlay disappears when the new page loads

For cached responses (instant loading), the overlay may appear very briefly.

## Troubleshooting

**Overlay doesn't appear:**
- Check that the `loadingOverlay` element exists in the HTML
- Check that the `.action-btn` class is on the buttons
- Check browser console for JavaScript errors

**Overlay doesn't hide:**
- This is expected behavior - the overlay hides when the page changes
- If the page doesn't navigate (e.g., AJAX request), call `LoadingOverlay.hide()` manually

**Styling conflicts:**
- Check z-index values (overlay uses `z-index: 9999`)
- Ensure Bootstrap or other CSS frameworks aren't overriding styles

## Future Enhancements

Possible improvements for the future:

- Progress bar showing actual completion percentage
- Cancel button for long-running operations
- Estimated time remaining countdown
- Different overlay styles for different action types
- Integration with server-sent events for real-time progress updates

## Support

For questions or issues with the loading overlay, please contact the development team or create an issue in the project repository.
